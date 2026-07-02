package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiInsightService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.dto.Views;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the home dashboard: aggregates every per-user AI feature into one payload and persists
 * the resulting insights (personality, health score, anomalies, saving nudge).
 */
@Service
public class DashboardService {

    private final TransactionService transactionService;
    private final AiCoach coach;
    private final PointsService points;
    private final LeaderboardService leaderboard;
    private final MissionService missions;
    private final GoalService goals;
    private final CashbackService cashback;
    private final AiInsightService insights;
    private final AccountService accounts;
    private final UserRepository users;

    public DashboardService(TransactionService transactionService, AiCoach coach, PointsService points,
                            LeaderboardService leaderboard, MissionService missions, GoalService goals,
                            CashbackService cashback, AiInsightService insights, AccountService accounts,
                            UserRepository users) {
        this.transactionService = transactionService;
        this.coach = coach;
        this.points = points;
        this.leaderboard = leaderboard;
        this.missions = missions;
        this.goals = goals;
        this.cashback = cashback;
        this.insights = insights;
        this.accounts = accounts;
        this.users = users;
    }

    @Transactional
    public Map<String, Object> build(User user) {
        SpendingProfile p = transactionService.profileFor(user);

        // Mission consistency
        List<Mission> allMissions = missions.listFor(user);
        long completed = allMissions.stream().filter(m -> m.getStatus() == MissionStatus.COMPLETED).count();
        int missionRatio = allMissions.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / allMissions.size());

        // Goal progress
        List<SavingsGoal> goalList = goals.listFor(user);
        long onTrack = goalList.stream().filter(g -> g.getStatus() != GoalStatus.BEHIND).count();
        int goalsOnTrackPercent = goalList.isEmpty() ? 100 : (int) Math.round(onTrack * 100.0 / goalList.size());

        // Emergency fund proxy
        BigDecimal totalSaved = p.monthlySaved.multiply(BigDecimal.valueOf(p.months));
        double emergencyTarget = Math.max(1, p.monthlySpending.doubleValue() * 3);
        double emergencyProgress = Math.min(1.0, totalSaved.doubleValue() / emergencyTarget);

        AiModels.PersonalityResult personality = coach.personality(p);
        AiModels.HealthScore health = coach.healthScore(p, missionRatio, goalsOnTrackPercent, emergencyProgress);
        String savingInsight = coach.savingInsight(p);
        List<AiModels.Anomaly> anomalies = coach.anomalies(p, transactionService.forUser(user));
        AiModels.BudgetBreakdown budget = coach.budget(p);
        AiModels.CashbackRecommendation cashbackRec = cashback.recommend(user);

        // Persist insights + update user profile fields
        user.setSpendingPersonality(SpendingPersonality.valueOf(personality.personality()));
        user.setSavingsHealthScore(health.score());
        users.save(user);

        AiInsightService.Narration pn = insights.narrate("This describes a spending personality.", personality.reason());
        insights.save(user, InsightKind.PERSONALITY, personality.label(), pn.text(), personality, pn.source());
        insights.save(user, InsightKind.HEALTH_SCORE, "صحة الادّخار " + health.score(), health.summary(), health, AiSource.DETERMINISTIC);
        AiInsightService.Narration sn = insights.narrate("This is a saving nudge.", savingInsight);
        insights.save(user, InsightKind.SAVING_INSIGHT, "نصيحة ادّخار", sn.text(), Map.of("text", sn.text()), sn.source());
        if (!anomalies.isEmpty()) {
            insights.save(user, InsightKind.ANOMALY, "حركة صرف غير معتادة", anomalies.get(0).message(), anomalies, AiSource.DETERMINISTIC);
        }

        Views.LeaderboardResponse lb = leaderboard.leaderboard(user);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user", accounts.meView(user));
        out.put("savingsRatePercent", p.savingsRatePercent());
        out.put("monthlyIncome", p.baselineIncome);
        out.put("monthlySpending", p.monthlySpending);
        out.put("monthlySaved", p.monthlySaved);
        out.put("personality", new AiModels.PersonalityResult(personality.personality(), personality.label(), pn.text()));
        out.put("healthScore", health);
        out.put("savingInsight", sn.text());
        out.put("anomalies", anomalies);
        out.put("budget", budget);
        out.put("cashback", cashbackRec);
        out.put("breakdown", transactionService.breakdown(user));
        out.put("normalPoints", points.balance(user, PointsType.NORMAL));
        out.put("seasonalPoints", points.balance(user, PointsType.SEASONAL));
        out.put("leaderboardRank", lb.viewerRank());
        out.put("leagueLabel", lb.leagueLabel());
        out.put("leaderboardPlayers", lb.totalPlayers());
        out.put("activeMissions", missions.activeFor(user).stream().map(Views.MissionView::of).toList());
        out.put("topGoal", goalList.isEmpty() ? null : Views.GoalView.of(goalList.get(0)));
        out.put("aiProvider", insights.openAiActive() ? "openai" : "deterministic");
        return out;
    }
}
