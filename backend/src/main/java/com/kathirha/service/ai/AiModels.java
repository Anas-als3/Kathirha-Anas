package com.kathirha.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Structured AI outputs. Grouped as nested records to keep the package tidy. */
public final class AiModels {
    private AiModels() {}

    public record CategoryAmount(String category, String label, String emoji, BigDecimal amount, double sharePercent) {}

    public record PersonalityResult(String personality, String label, String reason) {}

    public record ScoreFactor(String name, int score, int weightPercent, String note) {}

    public record HealthScore(int score, String grade, String summary, List<ScoreFactor> factors) {}

    public record MissionSpec(String title, String description, String type, String difficulty,
                              int rewardPoints, String targetCategory, BigDecimal targetAmount) {}

    public record GoalPlan(BigDecimal inflationAdjustedTarget, BigDecimal monthlySaving, BigDecimal weeklySaving,
                           String riskLevel, String strategy, List<MissionSpec> suggestedMissions) {}

    public record RescueOption(String type, String label, String detail,
                               BigDecimal newMonthlySaving, LocalDate newTargetDate) {}

    public record GoalRescue(String message, RescueOption extend, RescueOption increase) {}

    public record CashbackRecommendation(String cardName, String emoji, String category,
                                         BigDecimal estimatedMonthlySaving, String reason) {}

    public record BudgetItem(String category, String label, String emoji, BigDecimal current, BigDecimal suggested) {}

    public record BudgetBreakdown(List<BudgetItem> items, BigDecimal suggestedSavings, String note) {}

    public record Anomaly(String category, String label, BigDecimal current, BigDecimal usual,
                          double percentAbove, String message) {}

    public record QuizSpec(String prompt, List<String> options, int correctIndex, String explanation, int rewardPoints) {}

    public record StreakCoachMessage(String message, boolean encouraging) {}

    public record LeaderboardExplanation(int rank, double savingsRatePercent, boolean capped,
                                         String rankReason, String capExplanation,
                                         BigDecimal extraMonthlySavingToClimb, String nextStep) {}

    public record RewardRecommendation(Long itemId, String itemName, String emoji, String reason) {}

    public record AdminInsightItem(String title, String detail, String metric) {}

    public record AdminInsights(int totalUsers, int verifiedUsers, double avgSavingsRatePercent,
                                List<AdminInsightItem> insights) {}
}
