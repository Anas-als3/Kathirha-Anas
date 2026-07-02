package com.kathirha.web.dto;

import com.kathirha.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Response DTOs (records) with mappers from entities — keeps lazy entities out of the JSON layer. */
public final class Views {
    private Views() {}

    public record UserView(
            Long id, String phone, String displayName, String email, String role,
            String integrityStatus, boolean bankVerified, boolean phoneVerified, boolean competitiveOptIn,
            BigDecimal baselineIncome,
            String spendingPersonality, Integer savingsHealthScore,
            int currentStreak, int longestStreak, int streakFreezes, double savingsRatePercent) {

        public static UserView of(User u, double savingsRatePercent) {
            return new UserView(
                    u.getId(), u.getPhone(), u.getDisplayName(), u.getEmail(), u.getRole().name(),
                    u.getIntegrityStatus().name(), u.isBankVerified(), u.isPhoneVerified(), u.isCompetitiveOptIn(),
                    u.getBaselineIncome(),
                    u.getSpendingPersonality() == null ? null : u.getSpendingPersonality().label,
                    u.getSavingsHealthScore(),
                    u.getCurrentStreak(), u.getLongestStreak(), u.getStreakFreezes(),
                    round1(savingsRatePercent));
        }
    }

    public record AuthResponse(String token, UserView user) {}

    public record MissionView(
            Long id, String title, String description, String type, String difficulty,
            int rewardPoints, String pointsType, String status, String targetCategory,
            BigDecimal targetAmount, String dueDate, boolean aiGenerated) {

        public static MissionView of(Mission m) {
            return new MissionView(
                    m.getId(), m.getTitle(), m.getDescription(), m.getType().name(), m.getDifficulty().name(),
                    m.getRewardPoints(), m.getPointsType().name(), m.getStatus().name(),
                    m.getTargetCategory() == null ? null : m.getTargetCategory().name(),
                    m.getTargetAmount(), m.getDueDate() == null ? null : m.getDueDate().toString(),
                    m.isAiGenerated());
        }
    }

    public record GoalView(
            Long id, String name, BigDecimal targetAmount,
            BigDecimal currentAmount, String targetDate, String status, BigDecimal monthlySaving,
            BigDecimal weeklySaving, String riskLevel, String strategy, double progressPercent) {

        public static GoalView of(SavingsGoal g) {
            BigDecimal denom = g.getTargetAmount();
            double progress = denom != null && denom.signum() > 0
                    ? g.getCurrentAmount().divide(denom, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
            return new GoalView(
                    g.getId(), g.getName(), g.getTargetAmount(),
                    g.getCurrentAmount(), g.getTargetDate().toString(), g.getStatus().name(),
                    g.getMonthlySaving(), g.getWeeklySaving(), g.getRiskLevel(), g.getStrategy(),
                    round1(Math.min(100, progress)));
        }
    }

    public record LedgerView(int delta, int balanceAfter, String pointsType, String reason,
                             String description, String createdAt) {
        public static LedgerView of(PointsLedgerEntry e) {
            return new LedgerView(e.getDelta(), e.getBalanceAfter(), e.getPointsType().name(),
                    e.getReason() == null ? null : e.getReason().name(), e.getDescription(),
                    e.getCreatedAt().toString());
        }
    }

    public record PointsView(int normalBalance, int seasonalBalance, List<LedgerView> ledger) {}

    public record ShopItemView(Long id, String name, String description, String category,
                               int costPoints, String pointsType, String emoji, int stock, boolean affordable) {
        public static ShopItemView of(ShopItem s, boolean affordable) {
            return new ShopItemView(s.getId(), s.getName(), s.getDescription(), s.getCategory(),
                    s.getCostPoints(), s.getPointsType().name(), s.getEmoji(), s.getStock(), affordable);
        }
    }

    public record RedemptionView(Long id, String itemName, String emoji, String couponCode,
                                 int costPoints, String status, String createdAt) {
        public static RedemptionView of(Redemption r) {
            return new RedemptionView(r.getId(), r.getShopItem().getName(), r.getShopItem().getEmoji(),
                    r.getCouponCode(), r.getCostPoints(), r.getStatus().name(), r.getCreatedAt().toString());
        }
    }

    public record TransactionView(Long id, String date, String description, String merchant,
                                  BigDecimal amount, String category, String categoryLabel, String emoji) {
        public static TransactionView of(Transaction t) {
            return new TransactionView(t.getId(), t.getDate().toString(), t.getDescription(), t.getMerchant(),
                    t.getAmount(), t.getCategory().name(), t.getCategory().label, t.getCategory().emoji);
        }
    }

    public record WhatsAppView(Long id, String direction, String category, String body, String createdAt) {
        public static WhatsAppView of(WhatsAppMessage m) {
            return new WhatsAppView(m.getId(), m.getDirection().name(), m.getCategory().name(),
                    m.getBody(), m.getCreatedAt().toString());
        }
    }

    public record LeaderboardEntry(int rank, String displayName, String leagueLabel,
                                   int scorePoints, double savingsRatePercent, boolean capped, boolean currentUser) {}

    public record LeaderboardResponse(String leagueLabel, int capPercent, int viewerRank,
                                      int totalPlayers, List<LeaderboardEntry> entries) {}

    static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
