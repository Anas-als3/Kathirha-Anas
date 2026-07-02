package com.kathirha.service;

import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.*;
import com.kathirha.repository.SavingsGoalRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiInsightService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GoalService {

    private final SavingsGoalRepository goals;
    private final TransactionService transactionService;
    private final AiCoach coach;
    private final KathirhaProperties props;
    private final WhatsAppService whatsapp;
    private final AiInsightService insights;
    private final StreakService streaks;

    public GoalService(SavingsGoalRepository goals, TransactionService transactionService, AiCoach coach,
                       KathirhaProperties props, WhatsAppService whatsapp, AiInsightService insights,
                       StreakService streaks) {
        this.goals = goals;
        this.transactionService = transactionService;
        this.coach = coach;
        this.props = props;
        this.whatsapp = whatsapp;
        this.insights = insights;
        this.streaks = streaks;
    }

    @Transactional
    public SavingsGoal create(User user, String name, BigDecimal target, LocalDate targetDate) {
        SpendingProfile p = transactionService.profileFor(user);
        // Compliance: no future predictions — the plan works from the plain target only.
        AiModels.GoalPlan plan = coach.planGoal(name, target, targetDate, p, java.math.BigDecimal.ZERO);

        SavingsGoal g = new SavingsGoal();
        g.setUser(user);
        g.setName(name);
        g.setTargetAmount(target);
        g.setTargetDate(targetDate);
        g.setInflationAdjustedTarget(plan.inflationAdjustedTarget());
        g.setMonthlySaving(plan.monthlySaving());
        g.setWeeklySaving(plan.weeklySaving());
        g.setRiskLevel(plan.riskLevel());
        g.setStrategy(plan.strategy());
        g.setCurrentAmount(BigDecimal.ZERO);
        g.setStatus(GoalStatus.ON_TRACK);
        goals.save(g);

        AiInsightService.Narration n = insights.narrate("This is a savings-goal plan.", plan.strategy());
        insights.save(user, InsightKind.GOAL_PLAN, "خطة: " + name, n.text(), plan, n.source());
        return g;
    }

    public List<SavingsGoal> listFor(User user) {
        return goals.findByUserOrderByCreatedAtDesc(user);
    }

    public SavingsGoal get(User user, Long id) {
        return goals.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiExceptions.NotFoundException("الهدف غير موجود"));
    }

    @Transactional
    public SavingsGoal contribute(User user, Long id, BigDecimal amount) {
        SavingsGoal g = get(user, id);
        g.setCurrentAmount(g.getCurrentAmount().add(amount.abs()));
        BigDecimal target = denom(g);
        if (g.getCurrentAmount().compareTo(target) >= 0) {
            g.setStatus(GoalStatus.COMPLETED);
        } else if (g.getStatus() == GoalStatus.BEHIND) {
            g.setStatus(GoalStatus.ON_TRACK);
        }
        goals.save(g);
        streaks.recordSaving(user);
        return g;
    }

    @Transactional
    public AiModels.GoalRescue rescue(User user, Long id) {
        SavingsGoal g = get(user, id);
        SpendingProfile p = transactionService.profileFor(user);
        AiModels.GoalRescue rescue = coach.rescueGoal(g, p);

        AiInsightService.Narration n = insights.narrate("This is a goal-rescue nudge.", rescue.message());
        AiModels.GoalRescue narrated = new AiModels.GoalRescue(n.text(), rescue.extend(), rescue.increase());
        insights.save(user, InsightKind.GOAL_RESCUE, "تأخّر في " + g.getName(), n.text(), narrated, n.source());

        whatsapp.send(user, MessageCategory.GOAL_RESCUE,
                narrated.message()
                        + "\n\n1️⃣ " + rescue.extend().label() + " — " + rescue.extend().detail()
                        + "\n2️⃣ " + rescue.increase().label() + " — " + rescue.increase().detail()
                        + "\n\nرد بـ 1 أو 2.",
                "goal:" + g.getId());
        return narrated;
    }

    @Transactional
    public SavingsGoal applyRescue(User user, Long id, String option) {
        SavingsGoal g = get(user, id);
        SpendingProfile p = transactionService.profileFor(user);
        AiModels.GoalRescue rescue = coach.rescueGoal(g, p);

        String opt = option == null ? "" : option.trim().toUpperCase();
        if (opt.equals("EXTEND") || opt.equals("1")) {
            g.setTargetDate(rescue.extend().newTargetDate());
            g.setMonthlySaving(rescue.extend().newMonthlySaving());
        } else if (opt.equals("INCREASE") || opt.equals("2")) {
            g.setMonthlySaving(rescue.increase().newMonthlySaving());
        } else {
            throw new ApiExceptions.BadRequestException("اختر EXTEND (1) أو INCREASE (2)");
        }
        g.setStatus(GoalStatus.ON_TRACK);
        goals.save(g);

        whatsapp.recordInbound(user, MessageCategory.GOAL_RESCUE, opt.startsWith("1") || opt.equals("EXTEND") ? "1" : "2", "goal:" + g.getId());
        whatsapp.send(user, MessageCategory.GOAL_RESCUE,
                "✅ تم! عاد هدف \"" + g.getName() + "\" إلى مساره الصحيح. الخطة الجديدة: "
                        + g.getMonthlySaving() + " ريال شهريًا، والموعد المستهدف " + g.getTargetDate() + ".",
                "goal:" + g.getId());
        return g;
    }

    /** Force a goal into BEHIND for the demo (deterministic). */
    @Transactional
    public SavingsGoal markBehind(SavingsGoal g) {
        g.setStatus(GoalStatus.BEHIND);
        return goals.save(g);
    }

    private static BigDecimal denom(SavingsGoal g) {
        return g.getTargetAmount();
    }
}
