package com.kathirha.domain;

import java.math.BigDecimal;

/** Income bands (monthly SAR) so users compete with true peers, not by absolute wealth. */
public enum IncomeLeague {
    STARTER("Starter", 0, 4000),
    BRONZE("Bronze", 4000, 8000),
    SILVER("Silver", 8000, 15000),
    GOLD("Gold", 15000, 25000),
    PLATINUM("Platinum", 25000, Integer.MAX_VALUE);

    public final String label;
    public final int minInclusive;
    public final int maxExclusive;

    IncomeLeague(String label, int minInclusive, int maxExclusive) {
        this.label = label;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    public static IncomeLeague fromIncome(BigDecimal monthlyIncome) {
        if (monthlyIncome == null) return STARTER;
        double v = monthlyIncome.doubleValue();
        for (IncomeLeague l : values()) {
            if (v >= l.minInclusive && v < l.maxExclusive) return l;
        }
        return PLATINUM;
    }
}
