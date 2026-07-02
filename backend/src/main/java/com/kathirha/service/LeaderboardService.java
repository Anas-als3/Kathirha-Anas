package com.kathirha.service;

import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.AchievementLeague;
import com.kathirha.domain.InsightKind;
import com.kathirha.domain.User;
import com.kathirha.repository.PointsLedgerRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.ai.AiInsightService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.dto.Views;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fairness leaderboard — ranks by EARNED score points, never by wealth.
 *
 * Score = saving points + activity points, where:
 *  - saving points: the savings *ratio* converts to points linearly up to the cap
 *    (cap% of income = 1000 points; saving beyond the cap earns NO extra points), and
 *  - activity points: every positive points-ledger entry (missions, daily question,
 *    surveys, streaks). Earn-only: spending in the shop never lowers the score.
 *
 * Leagues are achievement tiers (Bronze → Silver → Gold → Diamond) climbed by score —
 * income never groups or labels anyone.
 */
@Service
public class LeaderboardService {

    private static final int SAVING_POINTS_AT_CAP = 1000;

    private final UserRepository users;
    private final TransactionService transactionService;
    private final KathirhaProperties props;
    private final AiInsightService insights;
    private final PointsLedgerRepository ledger;

    public LeaderboardService(UserRepository users, TransactionService transactionService,
                              KathirhaProperties props, AiInsightService insights,
                              PointsLedgerRepository ledger) {
        this.users = users;
        this.transactionService = transactionService;
        this.props = props;
        this.insights = insights;
        this.ledger = ledger;
    }

    private record Scored(User user, double ratePercent, int savingPoints, int activityPoints) {
        int score() { return savingPoints + activityPoints; }
    }

    public double savingsRatePercent(User u) {
        SpendingProfile p = SpendingProfile.from(u.getBaselineIncome(), transactionService.forUser(u));
        return p.savingsRatePercent();
    }

    /** Saving ratio → points, capped: cap% of income = 1000 points, beyond the cap = no extra points. */
    public int savingPoints(double ratePercent) {
        int cap = props.getEconomy().getSavingsCapPercent();
        double effective = Math.max(0, Math.min(ratePercent, cap));
        return (int) Math.round(effective / cap * SAVING_POINTS_AT_CAP);
    }

    public int scorePoints(User u) {
        return savingPoints(savingsRatePercent(u)) + ledger.earnedTotal(u);
    }

    public AchievementLeague leagueOf(User u) {
        return AchievementLeague.fromScore(scorePoints(u));
    }

    private Scored scoreOf(User u) {
        double rate = savingsRatePercent(u);
        return new Scored(u, rate, savingPoints(rate), ledger.earnedTotal(u));
    }

    /** Everyone opted-in & verified in the viewer's achievement league, ranked by score. */
    private List<Scored> rankedLeague(User viewer) {
        Scored viewerScored = scoreOf(viewer);
        AchievementLeague league = AchievementLeague.fromScore(viewerScored.score());

        List<Scored> scored = new ArrayList<>();
        for (User u : users.findByCompetitiveOptInTrueAndBankVerifiedTrue()) {
            if (u.getId().equals(viewer.getId())) continue;
            Scored s = scoreOf(u);
            if (AchievementLeague.fromScore(s.score()) == league) scored.add(s);
        }
        scored.add(viewerScored);
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(Comparator.comparingDouble(Scored::ratePercent).reversed())
                .thenComparing(s -> s.user().getId()));
        return scored;
    }

    public Views.LeaderboardResponse leaderboard(User viewer) {
        int cap = props.getEconomy().getSavingsCapPercent();
        List<Scored> scored = rankedLeague(viewer);
        List<Views.LeaderboardEntry> entries = new ArrayList<>();
        int viewerRank = 0;
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            boolean isViewer = s.user().getId().equals(viewer.getId());
            if (isViewer) viewerRank = i + 1;
            entries.add(new Views.LeaderboardEntry(
                    i + 1,
                    s.user().getDisplayName() == null ? s.user().getPhone() : s.user().getDisplayName(),
                    AchievementLeague.fromScore(s.score()).label,
                    s.score(), round1(s.ratePercent()),
                    s.ratePercent() > cap, isViewer));
        }
        AchievementLeague league = leagueOf(viewer);
        return new Views.LeaderboardResponse(league.label, cap, viewerRank, scored.size(), entries);
    }

    public AiModels.LeaderboardExplanation explain(User viewer) {
        int cap = props.getEconomy().getSavingsCapPercent();
        List<Scored> scored = rankedLeague(viewer);

        int idx = -1;
        for (int i = 0; i < scored.size(); i++) {
            if (scored.get(i).user().getId().equals(viewer.getId())) { idx = i; break; }
        }
        Scored me = idx >= 0 ? scored.get(idx) : scoreOf(viewer);
        boolean capped = me.ratePercent() > cap;
        String capExplanation = "نقاطك = نقاط الادّخار (نسبة ادّخارك حتى " + cap + "٪ من دخلك) + نقاط نشاطك "
                + "(المهام وسؤال اليوم والاستبيانات والسلاسل). الادّخار فوق " + cap + "٪ لا يمنح نقاطًا إضافية — "
                + "فالصدارة تكافئ الانضباط لا الثروة. ونقاطك تُكتسب ولا تُنقَص عند الصرف.";

        String rankReason;
        String nextStep;
        BigDecimal extra = BigDecimal.ZERO;

        if (idx == 0) {
            rankReason = String.format("أنت في المركز الأول في %s برصيد %d نقطة (ادّخار %.1f٪%s).",
                    AchievementLeague.fromScore(me.score()).label, me.score(), me.ratePercent(),
                    capped ? " — بلغت سقف العدالة" : "");
            nextStep = "حافظ على صدارتك — واصل سلسلتك وأكمل تحدّيات الموسم لنقاط إضافية.";
        } else {
            Scored above = scored.get(idx - 1);
            int deltaPoints = Math.max(1, above.score() - me.score() + 1);
            rankReason = String.format("أنت في المركز %d برصيد %d نقطة، والمتنافس فوقك عنده %d نقطة.",
                    idx + 1, me.score(), above.score());
            if (capped || me.savingPoints() >= SAVING_POINTS_AT_CAP) {
                nextStep = String.format("بلغت سقف نقاط الادّخار — تحتاج %d نقطة لتجاوزه، تكسبها من المهام وسؤال اليوم والاستبيانات وتحدّيات الموسم.", deltaPoints);
            } else {
                BigDecimal income = viewer.getBaselineIncome() == null ? BigDecimal.ZERO : viewer.getBaselineIncome();
                // 1 saving point = (cap% of income) / 1000 in SAR
                BigDecimal sarPerPoint = income.multiply(BigDecimal.valueOf(cap))
                        .divide(BigDecimal.valueOf(100L * SAVING_POINTS_AT_CAP), 4, RoundingMode.HALF_UP);
                extra = sarPerPoint.multiply(BigDecimal.valueOf(deltaPoints)).setScale(0, RoundingMode.CEILING);
                nextStep = String.format("تحتاج %d نقطة لتجاوزه — ادّخر نحو %s ريال إضافية هذا الشهر، أو اكسبها من المهام وسؤال اليوم.",
                        deltaPoints, extra.toPlainString());
            }
        }

        AiModels.LeaderboardExplanation exp = new AiModels.LeaderboardExplanation(
                idx + 1, round1(me.ratePercent()), capped, rankReason, capExplanation, extra, nextStep);

        AiInsightService.Narration n = insights.narrate("This explains a leaderboard rank.", rankReason + " " + nextStep);
        insights.save(viewer, InsightKind.LEADERBOARD_EXPLAIN, "لماذا ترتيبك #" + (idx + 1), n.text(), exp, n.source());
        return exp;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
