package com.kathirha.domain;

/**
 * Achievement leagues — climbed by earned score points, never by income.
 * Everyone starts Bronze; income never groups or labels anyone (fairness by effort).
 */
public enum AchievementLeague {
    BRONZE("الدوري البرونزي", 0),
    SILVER("الدوري الفضي", 750),
    GOLD("الدوري الذهبي", 1750),
    DIAMOND("الدوري الماسي", 3250);

    public final String label;
    public final int minScore;

    AchievementLeague(String label, int minScore) {
        this.label = label;
        this.minScore = minScore;
    }

    public static AchievementLeague fromScore(int scorePoints) {
        AchievementLeague result = BRONZE;
        for (AchievementLeague l : values()) {
            if (scorePoints >= l.minScore) result = l;
        }
        return result;
    }
}
