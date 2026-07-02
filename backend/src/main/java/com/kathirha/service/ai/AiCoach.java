package com.kathirha.service.ai;

import com.kathirha.domain.CashbackCard;
import com.kathirha.domain.IncomeLeague;
import com.kathirha.domain.SavingsGoal;
import com.kathirha.domain.Transaction;
import com.kathirha.service.SpendingProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The AI brain. {@code DeterministicAiCoach} is the always-available implementation;
 * narrative fields may be enhanced by OpenAI when a key is configured.
 * Every method must work fully without any external API.
 */
public interface AiCoach {

    AiModels.PersonalityResult personality(SpendingProfile p);

    AiModels.HealthScore healthScore(SpendingProfile p, int missionsCompletedRatio,
                                     int goalsOnTrackPercent, double emergencyFundProgress);

    List<AiModels.MissionSpec> generateMissions(SpendingProfile p, IncomeLeague league);

    AiModels.GoalPlan planGoal(String goalName, BigDecimal target, LocalDate targetDate,
                               SpendingProfile p, BigDecimal inflationPercent);

    AiModels.GoalRescue rescueGoal(SavingsGoal goal, SpendingProfile p);

    AiModels.CashbackRecommendation recommendCashback(SpendingProfile p, List<CashbackCard> cards);

    AiModels.BudgetBreakdown budget(SpendingProfile p);

    List<AiModels.Anomaly> anomalies(SpendingProfile p, List<Transaction> txns);

    AiModels.QuizSpec dailyQuiz(SpendingProfile p, long seed);

    AiModels.StreakCoachMessage streakCoach(int currentStreak, boolean missed);

    String savingInsight(SpendingProfile p);
}
