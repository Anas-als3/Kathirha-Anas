package com.kathirha.service.ai;

import com.kathirha.domain.*;
import com.kathirha.service.SpendingProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Deterministic, rule-based implementation of the AI coach. Produces realistic, personalized
 * outputs from the user's {@link SpendingProfile} with zero external dependencies, so the demo
 * always works. {@code OpenAiClient} may later rephrase narrative strings, but never the numbers.
 */
@Component
public class DeterministicAiCoach implements AiCoach {

    // ---- Personality ---------------------------------------------------------------------

    @Override
    public AiModels.PersonalityResult personality(SpendingProfile p) {
        double total = p.monthlySpending.doubleValue();
        double coffee = share(p, TransactionCategory.COFFEE, total);
        double food = share(p, TransactionCategory.FOOD_DELIVERY, total);
        double fun = (p.categoryMonthly(TransactionCategory.ENTERTAINMENT).doubleValue()
                + p.categoryMonthly(TransactionCategory.SHOPPING).doubleValue()) / Math.max(1, total);

        SpendingPersonality type;
        String reason;
        if (p.savingsRate >= 0.30) {
            type = SpendingPersonality.CAUTIOUS_SAVER;
            reason = String.format("تدّخر %.0f%% من دخلك — أعلى من المتوسط بوضوح. إنفاقك منضبط ويمكن توقّعه.", p.savingsRatePercent());
        } else if (food > 0.20) {
            type = SpendingPersonality.FOOD_DELIVERY_HEAVY;
            reason = String.format("توصيل الطعام يشكّل %.0f%% من إنفاقك (%s شهريًا) — أكبر تسرّب في ميزانيتك.", food * 100, money(p.categoryMonthly(TransactionCategory.FOOD_DELIVERY)));
        } else if (coffee > 0.10) {
            type = SpendingPersonality.COFFEE_LEAKER;
            reason = String.format("القهوة تشكّل %.0f%% من إنفاقك (%s شهريًا). أكواب صغيرة، وتسرّب كبير.", coffee * 100, money(p.categoryMonthly(TransactionCategory.COFFEE)));
        } else if (fun > 0.35) {
            type = SpendingPersonality.IMPULSE_SPENDER;
            reason = String.format("التسوّق والترفيه يشكّلان %.0f%% من إنفاقك — إنفاق اندفاعي ومتقلّب.", fun * 100);
        } else if (p.savingsRate >= 0.18) {
            type = SpendingPersonality.GOAL_ORIENTED_SAVER;
            reason = String.format("تدّخر %.0f%% بانتظام وتُبقي المصروفات الكمالية تحت السيطرة.", p.savingsRatePercent());
        } else {
            type = SpendingPersonality.BALANCED;
            reason = "إنفاقك موزّع بتوازن دون تسرّب كبير في فئة واحدة — ملف متوازن مع مساحة لادّخار أكثر.";
        }
        return new AiModels.PersonalityResult(type.name(), type.label, reason);
    }

    // ---- Savings Health Score ------------------------------------------------------------

    @Override
    public AiModels.HealthScore healthScore(SpendingProfile p, int missionsCompletedRatio,
                                            int goalsOnTrackPercent, double emergencyFundProgress) {
        int savingsScore = clamp((int) Math.round(p.savingsRate / 0.40 * 100));
        double discShare = (p.categoryMonthly(TransactionCategory.FOOD_DELIVERY).doubleValue()
                + p.categoryMonthly(TransactionCategory.COFFEE).doubleValue()
                + p.categoryMonthly(TransactionCategory.ENTERTAINMENT).doubleValue()
                + p.categoryMonthly(TransactionCategory.SHOPPING).doubleValue())
                / Math.max(1, p.monthlySpending.doubleValue());
        int controlScore = clamp((int) Math.round(120 - discShare * 200));
        int goalScore = clamp(goalsOnTrackPercent);
        int emergencyScore = clamp((int) Math.round(emergencyFundProgress * 100));
        int missionScore = clamp(missionsCompletedRatio);

        List<AiModels.ScoreFactor> factors = List.of(
                new AiModels.ScoreFactor("نسبة الادّخار", savingsScore, 35, String.format("تدّخر %.0f%% من الدخل", p.savingsRatePercent())),
                new AiModels.ScoreFactor("ضبط الإنفاق", controlScore, 20, String.format("%.0f%% من إنفاقك كمالي", discShare * 100)),
                new AiModels.ScoreFactor("تقدّم الأهداف", goalScore, 20, goalsOnTrackPercent + "% من أهدافك على المسار الصحيح"),
                new AiModels.ScoreFactor("صندوق الطوارئ", emergencyScore, 15, String.format("%.0f%% من احتياطي 3 أشهر", emergencyFundProgress * 100)),
                new AiModels.ScoreFactor("المواظبة على المهام", missionScore, 10, missionsCompletedRatio + "% من المهام مكتملة")
        );
        int overall = clamp((int) Math.round(
                savingsScore * 0.35 + controlScore * 0.20 + goalScore * 0.20
                        + emergencyScore * 0.15 + missionScore * 0.10));
        String grade = overall >= 80 ? "ممتاز" : overall >= 65 ? "جيد" : overall >= 50 ? "مقبول" : "يحتاج تحسينًا";
        String summary = overall >= 65
                ? "صحة مالية قوية — حافظ على سلسلتك وواصل التقدّم في لوحة الصدارة."
                : "أمامك مجال واضح للتحسّن: ارفع نسبة الادّخار وقلّل المصروفات الكمالية لترتقي درجة.";
        return new AiModels.HealthScore(overall, grade, summary, factors);
    }

    // ---- Mission generation --------------------------------------------------------------

    @Override
    public List<AiModels.MissionSpec> generateMissions(SpendingProfile p, IncomeLeague league) {
        List<AiModels.MissionSpec> out = new ArrayList<>();
        BigDecimal coffee = p.categoryMonthly(TransactionCategory.COFFEE);
        BigDecimal food = p.categoryMonthly(TransactionCategory.FOOD_DELIVERY);
        TransactionCategory top = p.topSpendCategory == null ? TransactionCategory.SHOPPING : p.topSpendCategory;

        // DAILY easy — quick win against a small leak
        BigDecimal coffeeCut = scale(coffee.signum() > 0 ? coffee : new BigDecimal("60"), 0.25);
        out.add(new AiModels.MissionSpec(
                "تخطَّ زيارتين للمقهى",
                "حضّر قهوتك في البيت مرتين هذا الأسبوع وحوّل " + money(coffeeCut) + " مباشرة إلى مدخراتك.",
                "DAILY", "EASY", reward(MissionDifficulty.EASY),
                TransactionCategory.COFFEE.name(), coffeeCut));

        // WEEKLY medium — trim the top category
        BigDecimal topCut = scale(p.categoryMonthly(top), 0.15);
        if (topCut.signum() <= 0) topCut = new BigDecimal("100");
        out.add(new AiModels.MissionSpec(
                "قلّل " + top.label + " بنسبة 15%",
                "إنفاقك على " + top.label.toLowerCase() + " يبلغ " + money(p.categoryMonthly(top)) + " شهريًا. قلّله بنسبة 15% (" + money(topCut) + ") هذا الأسبوع.",
                "WEEKLY", "MEDIUM", reward(MissionDifficulty.MEDIUM),
                top.name(), topCut));

        // MONTHLY hard — meaningful monthly save
        BigDecimal monthlyTarget = scale(p.baselineIncome.signum() > 0 ? p.baselineIncome : new BigDecimal("5000"), 0.10);
        out.add(new AiModels.MissionSpec(
                "ادّخر 10% من دخل هذا الشهر",
                "حوّل " + money(monthlyTarget) + " إلى مدخراتك قبل نهاية الشهر لتحافظ على مركزك.",
                "MONTHLY", "HARD", reward(MissionDifficulty.HARD),
                TransactionCategory.SAVINGS_TRANSFER.name(), monthlyTarget));

        // PAYDAY — save first
        BigDecimal paydayTarget = scale(p.baselineIncome.signum() > 0 ? p.baselineIncome : new BigDecimal("5000"), 0.15);
        out.add(new AiModels.MissionSpec(
                "ادفع لنفسك أولًا",
                "يوم الراتب، حوّل " + money(paydayTarget) + " إلى مدخراتك قبل أن تصرف ريالًا واحدًا.",
                "PAYDAY", "MEDIUM", reward(MissionDifficulty.MEDIUM),
                TransactionCategory.SAVINGS_TRANSFER.name(), paydayTarget));

        // EMERGENCY — buffer builder
        BigDecimal emergencyTarget = scale(p.monthlySpending.signum() > 0 ? p.monthlySpending : new BigDecimal("3000"), 0.20);
        out.add(new AiModels.MissionSpec(
                "ابنِ شبكة أمانك",
                "أضف " + money(emergencyTarget) + " نحو صندوق طوارئ يغطي 3 أشهر.",
                "EMERGENCY", "MEDIUM", reward(MissionDifficulty.MEDIUM),
                TransactionCategory.SAVINGS_TRANSFER.name(), emergencyTarget));

        // SURVEY — the feedback loop: users share opinions, we learn and reward
        out.add(new AiModels.MissionSpec(
                "شارك رأيك في استبيان كثّرها",
                "استبيان 60 ثانية عن تجربتك هذا الأسبوع — نتعلّم منك ونكافئك بالنقاط.",
                "SURVEY", "EASY", reward(MissionDifficulty.EASY), null, BigDecimal.ZERO));

        // LEARN — financial literacy beyond saving
        out.add(new AiModels.MissionSpec(
                "أجب على سؤال اليوم 3 أيام متتالية",
                "دقيقة واحدة يوميًا ترفع وعيك المالي وتحافظ على سلسلتك.",
                "LEARN", "MEDIUM", reward(MissionDifficulty.MEDIUM), null, BigDecimal.ZERO));

        // LEARN easy — know where your money goes
        out.add(new AiModels.MissionSpec(
                "راجع خريطة مصروفاتك",
                "افتح «أين تذهب أموالك؟» وحدّد أكبر ثلاث فئات إنفاق لديك هذا الشهر.",
                "LEARN", "EASY", reward(MissionDifficulty.EASY), null, BigDecimal.ZERO));

        // SOCIAL — bring a friend into the savings challenge
        out.add(new AiModels.MissionSpec(
                "ادعُ صديقًا لتحدّي الادّخار",
                "الادّخار أمتع بالمنافسة — شارك دعوتك وانضمّا لنفس الدوري.",
                "SOCIAL", "MEDIUM", reward(MissionDifficulty.MEDIUM), null, BigDecimal.ZERO));

        return out;
    }

    // ---- Smart goal planner --------------------------------------------------------------

    @Override
    public AiModels.GoalPlan planGoal(String goalName, BigDecimal target, LocalDate targetDate,
                                      SpendingProfile p, BigDecimal inflationPercent) {
        long months = monthsUntil(targetDate);
        // Compliance: no inflation forecasting — the plan divides the plain target over the time left.
        BigDecimal adjusted = target.setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthly = adjusted.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal weekly = monthly.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(52), 2, RoundingMode.HALF_UP);

        BigDecimal disposable = p.baselineIncome.subtract(p.monthlySpending).max(BigDecimal.ZERO);
        String risk;
        if (disposable.signum() <= 0 || monthly.compareTo(disposable) > 0) risk = "مرتفعة";
        else if (monthly.compareTo(scale(disposable, 0.5)) > 0) risk = "متوسطة";
        else risk = "منخفضة";

        String strategy = String.format(
                "خطتك: %s شهريًا (%s أسبوعيًا) لمدة %s حتى تبلغ هدفك %s. "
                        + "المتاح لديك بعد مصروفاتك نحو %s شهريًا. فعّل التحويل التلقائي يوم الراتب حتى لا يُصرف قبل أن يُدَّخر.",
                money(monthly), money(weekly), monthsLabel(months), money(adjusted), money(disposable));

        List<AiModels.MissionSpec> suggested = generateMissions(p, IncomeLeague.fromIncome(p.baselineIncome))
                .stream().limit(2).toList();

        return new AiModels.GoalPlan(adjusted, monthly, weekly, risk, strategy, suggested);
    }

    // ---- Goal rescue ---------------------------------------------------------------------

    @Override
    public AiModels.GoalRescue rescueGoal(SavingsGoal goal, SpendingProfile p) {
        BigDecimal targetAmt = goal.getTargetAmount();
        BigDecimal remaining = targetAmt.subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        long monthsLeft = Math.max(1, monthsUntil(goal.getTargetDate()));
        BigDecimal currentMonthly = goal.getMonthlySaving() == null || goal.getMonthlySaving().signum() <= 0
                ? remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP)
                : goal.getMonthlySaving();

        long monthsNeeded = Math.max(1, (long) Math.ceil(remaining.doubleValue() / Math.max(1, currentMonthly.doubleValue())));
        LocalDate newDate = LocalDate.now().plusMonths(monthsNeeded);
        long extraMonths = Math.max(0, monthsNeeded - monthsLeft);
        BigDecimal increased = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP);

        String extendDetail = extraMonths > 0
                ? String.format("واصل ادّخار %s شهريًا لتبلغ هدفك بحلول %s (%d %s).",
                        money(currentMonthly), newDate, extraMonths, extraMonths == 1 ? "شهر إضافي" : "أشهر إضافية")
                : String.format("واصل ادّخار %s شهريًا — أنت على المسار الصحيح للإنجاز بحلول %s.", money(currentMonthly), newDate);
        AiModels.RescueOption extend = new AiModels.RescueOption(
                "EXTEND", "مدّد الموعد النهائي", extendDetail, currentMonthly, newDate);

        AiModels.RescueOption increase = new AiModels.RescueOption(
                "INCREASE",
                "ادّخر أكثر قليلًا",
                String.format("ارفع الادّخار إلى %s شهريًا وحقّق موعدك الأصلي في %s.", money(increased), goal.getTargetDate()),
                increased, goal.getTargetDate());

        String message = String.format(
                "تأخرت قليلًا عن هدف \"%s\" — تبقّى %s. لا تقلق، أمامك طريقان واضحان للعودة إلى المسار 👇",
                goal.getName(), money(remaining));

        return new AiModels.GoalRescue(message, extend, increase);
    }

    // ---- Cashback card recommendation ----------------------------------------------------

    @Override
    public AiModels.CashbackRecommendation recommendCashback(SpendingProfile p, List<CashbackCard> cards) {
        CashbackCard best = null;
        BigDecimal bestSaving = BigDecimal.valueOf(-1);
        for (CashbackCard c : cards) {
            BigDecimal spend = p.categoryMonthly(c.getRewardCategory());
            BigDecimal saving = spend.multiply(c.getCashbackPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .subtract(c.getAnnualFee().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
            if (saving.compareTo(bestSaving) > 0) { bestSaving = saving; best = c; }
        }
        if (best == null) {
            return new AiModels.CashbackRecommendation("لا توجد بطاقة", "💳", "OTHER", BigDecimal.ZERO,
                    "لا تتوفر بيانات إنفاق كافية بعد لنرشّح لك بطاقة.");
        }
        BigDecimal spend = p.categoryMonthly(best.getRewardCategory());
        String reason = String.format("تنفق %s شهريًا على %s — %s تعيد لك %.1f%%، أي نحو %s شهريًا على إنفاقك الحالي. والأذكى دائمًا: خفض الفئة نفسها — فنقاطك تُكسب من الادّخار لا من الإنفاق.",
                money(spend), best.getRewardCategory().label, best.getName(), best.getCashbackPercent().doubleValue(), money(bestSaving.max(BigDecimal.ZERO)));
        return new AiModels.CashbackRecommendation(best.getName(), best.getEmoji(), best.getRewardCategory().label,
                bestSaving.max(BigDecimal.ZERO), reason);
    }

    // ---- Budget breakdown ----------------------------------------------------------------

    @Override
    public AiModels.BudgetBreakdown budget(SpendingProfile p) {
        List<AiModels.BudgetItem> items = new ArrayList<>();
        TransactionCategory[] cats = {
                TransactionCategory.GROCERIES, TransactionCategory.FOOD_DELIVERY, TransactionCategory.COFFEE,
                TransactionCategory.TRANSPORT, TransactionCategory.ENTERTAINMENT, TransactionCategory.SHOPPING,
                TransactionCategory.BILLS
        };
        for (TransactionCategory c : cats) {
            BigDecimal current = p.categoryMonthly(c);
            double cut = switch (c) {
                case FOOD_DELIVERY -> 0.25;
                case COFFEE -> 0.30;
                case ENTERTAINMENT, SHOPPING -> 0.20;
                default -> 0.05; // essentials trimmed lightly
            };
            BigDecimal suggested = scale(current, 1 - cut);
            items.add(new AiModels.BudgetItem(c.name(), c.label, c.emoji, current, suggested));
        }
        BigDecimal suggestedSpend = items.stream().map(AiModels.BudgetItem::suggested).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal suggestedSavings = p.baselineIncome.subtract(suggestedSpend).max(BigDecimal.ZERO);
        String note = String.format("باتّباع هذه الخطة يصبح ادّخارك نحو %s شهريًا (%.0f%% من الدخل).",
                money(suggestedSavings),
                p.baselineIncome.signum() > 0 ? suggestedSavings.doubleValue() / p.baselineIncome.doubleValue() * 100 : 0);
        return new AiModels.BudgetBreakdown(items, suggestedSavings, note);
    }

    // ---- Anomaly detection ---------------------------------------------------------------

    @Override
    public List<AiModels.Anomaly> anomalies(SpendingProfile p, List<Transaction> txns) {
        // Group spending by (YearMonth, category)
        Map<YearMonth, Map<TransactionCategory, BigDecimal>> byMonth = new TreeMap<>();
        for (Transaction t : txns) {
            if (t.getCategory() == null || !t.getCategory().spending || t.getDate() == null) continue;
            byMonth.computeIfAbsent(YearMonth.from(t.getDate()), k -> new EnumMap<>(TransactionCategory.class))
                    .merge(t.getCategory(), t.absAmount(), BigDecimal::add);
        }
        if (byMonth.size() < 2) return List.of();

        List<YearMonth> ordered = new ArrayList<>(byMonth.keySet());
        YearMonth latest = ordered.get(ordered.size() - 1);
        List<YearMonth> priors = ordered.subList(0, ordered.size() - 1);

        List<AiModels.Anomaly> out = new ArrayList<>();
        for (TransactionCategory c : TransactionCategory.values()) {
            if (!c.spending) continue;
            BigDecimal current = byMonth.getOrDefault(latest, Map.of()).getOrDefault(c, BigDecimal.ZERO);
            BigDecimal priorSum = BigDecimal.ZERO;
            int n = 0;
            for (YearMonth m : priors) {
                BigDecimal v = byMonth.getOrDefault(m, Map.of()).getOrDefault(c, BigDecimal.ZERO);
                if (v.signum() > 0) { priorSum = priorSum.add(v); n++; }
            }
            if (n == 0 || current.signum() == 0) continue;
            BigDecimal usual = priorSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            if (usual.signum() <= 0) continue;
            double pctAbove = (current.doubleValue() - usual.doubleValue()) / usual.doubleValue() * 100.0;
            if (pctAbove >= 30 && current.subtract(usual).compareTo(new BigDecimal("100")) >= 0) {
                out.add(new AiModels.Anomaly(c.name(), c.label, current, usual, pctAbove,
                        String.format("إنفاقك على %s أعلى من المعتاد بنسبة %.0f%% (%s مقابل %s).",
                                c.label.toLowerCase(), pctAbove, money(current), money(usual))));
            }
        }
        out.sort((a, b) -> Double.compare(b.percentAbove(), a.percentAbove()));
        return out;
    }

    // ---- Daily literacy quiz -------------------------------------------------------------

    @Override
    public AiModels.QuizSpec dailyQuiz(SpendingProfile p, long seed) {
        String topLabel = p.topSpendCategory == null ? "التسوّق" : p.topSpendCategory.label.toLowerCase();
        BigDecimal income = p.baselineIncome.signum() > 0 ? p.baselineIncome : new BigDecimal("5000");
        BigDecimal tenPct = scale(income, 0.10);

        List<AiModels.QuizSpec> bank = List.of(
                new AiModels.QuizSpec(
                        "وفق قاعدة '50/30/20'، ما النسبة المقترحة من الدخل للادّخار؟",
                        List.of("5%", "20%", "50%", "0%"), 1,
                        "50% للاحتياجات، 30% للرغبات، 20% للادّخار. على دخلك يعادل ذلك نحو " + money(scale(income, 0.20)) + " شهريًا.", 15),
                new AiModels.QuizSpec(
                        "كم شهرًا من المصروفات يُفترض أن يغطي صندوق الطوارئ عادةً؟",
                        List.of("نصف شهر", "شهر واحد", "3–6 أشهر", "12 شهرًا"), 2,
                        "3–6 أشهر هي شبكة الأمان المعتادة حتى لا يربكك مصروف مفاجئ.", 15),
                new AiModels.QuizSpec(
                        String.format("تدّخر %s يوم الراتب قبل أن تصرف. تُسمى هذه العادة…", money(tenPct)),
                        List.of("الدفع لنفسك أولًا", "تمدّد الميزانية", "تضخّم نمط الحياة", "الدين المركّب"), 0,
                        "'ادفع لنفسك أولًا' تعني تحويل المدخرات قبل أن تتاح للصرف — أكثر عادة ادّخار فعالية.", 15),
                new AiModels.QuizSpec(
                        String.format("أكبر فئة إنفاق لديك هي %s. خفضها بنسبة 15%% يحسّن بشكل أساسي…", topLabel),
                        List.of("سجلّك الائتماني", "نسبة الادّخار لديك", "راتبك", "التضخّم"), 1,
                        "خفض أكبر فئة إنفاق يرفع نسبة الادّخار مباشرة — ومعه نقاطك في لوحة الصدارة.", 15),
                new AiModels.QuizSpec(
                        "لماذا تمنح كثّرها نقاط الادّخار حتى 40% من الدخل فقط؟",
                        List.of("لمعاقبة أصحاب الدخل المرتفع", "ليتفوّق الاجتهاد على الثراء", "اختيار عشوائي", "لتوفير تكاليف الخوادم"), 1,
                        "لأن النقاط تُحتسب حتى 40% من الدخل، يمكن لطالب يدّخر 30% أن يتفوّق على مدّخر ثري — العدالة تكافئ الانضباط لا الثروة.", 15)
        );
        int idx = (int) Math.floorMod(seed, bank.size());
        return bank.get(idx);
    }

    // ---- Streak coach --------------------------------------------------------------------

    @Override
    public AiModels.StreakCoachMessage streakCoach(int currentStreak, boolean missed) {
        if (missed) {
            return new AiModels.StreakCoachMessage(
                    "فاتك يوم — لكن تجميد السلسلة أنقذك 🧊. حوّل أي مبلغ إلى مدخراتك اليوم لتحافظ على الزخم.",
                    false);
        }
        String msg = currentStreak >= 30
                ? "🔥 سلسلة " + currentStreak + " يومًا! أنت ضمن نخبة المدّخرين. استمرارية كهذه يتضاعف أثرها."
                : currentStreak >= 7
                ? "🔥 سلسلة " + daysLabel(currentStreak) + " من الادّخار المتواصل — واصل لتحصد نقاطًا إضافية."
                : "أحسنت — اليوم " + currentStreak + ". مدخرات صغيرة يوميًا أفضل من دفعة كبيرة شهريًا. نراك غدًا!";
        return new AiModels.StreakCoachMessage(msg, true);
    }

    // ---- Saving insight ------------------------------------------------------------------

    @Override
    public String savingInsight(SpendingProfile p) {
        if (p.topSpendCategory == null) {
            return "اربط المزيد من العمليات وسأحدّد لك بدقة أين يتسرّب مالك.";
        }
        BigDecimal top = p.categoryMonthly(p.topSpendCategory);
        BigDecimal saveable = scale(top, 0.15);
        return String.format("%s هي أكبر فئة إنفاق لديك بواقع %s شهريًا. خفضها 15%% فقط يحرّر %s — يُضاف مباشرة إلى نسبة الادّخار.",
                p.topSpendCategory.label, money(top), money(saveable));
    }

    // ---- helpers -------------------------------------------------------------------------

    private static double share(SpendingProfile p, TransactionCategory c, double total) {
        return p.categoryMonthly(c).doubleValue() / Math.max(1, total);
    }

    private static int reward(MissionDifficulty d) {
        return d.baseReward;
    }

    private static BigDecimal scale(BigDecimal v, double f) {
        if (v == null) return BigDecimal.ZERO;
        return v.multiply(BigDecimal.valueOf(f)).setScale(2, RoundingMode.HALF_UP);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static long monthsUntil(LocalDate date) {
        if (date == null) return 1;
        long m = ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(date));
        return Math.max(1, m);
    }

    /** Whole-riyal formatting for human-readable messages. */
    static String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString() + " ريال";
    }

    /** Arabic number agreement for months: شهر واحد / شهران / 3-10 أشهر / 11+ شهرًا. */
    static String monthsLabel(long n) {
        if (n == 1) return "شهر واحد";
        if (n == 2) return "شهرين";
        if (n <= 10) return n + " أشهر";
        return n + " شهرًا";
    }

    /** Arabic number agreement for days: يوم واحد / يومان / 3-10 أيام / 11+ يومًا. */
    static String daysLabel(long n) {
        if (n == 1) return "يوم واحد";
        if (n == 2) return "يومين";
        if (n <= 10) return n + " أيام";
        return n + " يومًا";
    }
}
