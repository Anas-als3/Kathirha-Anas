package com.kathirha.service;

import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A derived, monthly-normalized view of a user's money: income, spending by category,
 * amount saved and savings rate. Pure value object — the single source of truth that
 * every AI feature reads from.
 */
public class SpendingProfile {

    public final BigDecimal baselineIncome;
    public final int months;
    public final Map<TransactionCategory, BigDecimal> monthlyByCategory;
    public final BigDecimal monthlySpending;
    public final BigDecimal monthlySaved;
    /** monthlySaved / baselineIncome, e.g. 0.30 = 30%. */
    public final double savingsRate;
    public final TransactionCategory topSpendCategory;

    private SpendingProfile(BigDecimal baselineIncome, int months,
                            Map<TransactionCategory, BigDecimal> monthlyByCategory,
                            BigDecimal monthlySpending, BigDecimal monthlySaved,
                            double savingsRate, TransactionCategory topSpendCategory) {
        this.baselineIncome = baselineIncome;
        this.months = months;
        this.monthlyByCategory = monthlyByCategory;
        this.monthlySpending = monthlySpending;
        this.monthlySaved = monthlySaved;
        this.savingsRate = savingsRate;
        this.topSpendCategory = topSpendCategory;
    }

    public BigDecimal categoryMonthly(TransactionCategory c) {
        return monthlyByCategory.getOrDefault(c, BigDecimal.ZERO);
    }

    public double savingsRatePercent() {
        return savingsRate * 100.0;
    }

    public static SpendingProfile from(BigDecimal baselineIncome, List<Transaction> txns) {
        Set<YearMonth> monthsSeen = new HashSet<>();
        Map<TransactionCategory, BigDecimal> totalByCategory = new EnumMap<>(TransactionCategory.class);
        BigDecimal salaryTotal = BigDecimal.ZERO;
        BigDecimal savedTotal = BigDecimal.ZERO;
        BigDecimal spendingTotal = BigDecimal.ZERO;

        for (Transaction t : txns) {
            if (t.getDate() != null) monthsSeen.add(YearMonth.from(t.getDate()));
            TransactionCategory c = t.getCategory();
            BigDecimal abs = t.absAmount();
            if (c == TransactionCategory.SALARY) {
                salaryTotal = salaryTotal.add(abs);
            } else if (c == TransactionCategory.SAVINGS_TRANSFER) {
                savedTotal = savedTotal.add(abs);
            } else if (c.spending && t.getAmount() != null && t.getAmount().signum() < 0) {
                // Only debits count as spending (real-bank credits/refunds are ignored here).
                totalByCategory.merge(c, abs, BigDecimal::add);
                spendingTotal = spendingTotal.add(abs);
            }
        }

        int months = Math.max(1, monthsSeen.size());

        BigDecimal income = baselineIncome;
        if (income == null || income.signum() <= 0) {
            income = salaryTotal.signum() > 0
                    ? salaryTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        Map<TransactionCategory, BigDecimal> monthlyByCategory = new EnumMap<>(TransactionCategory.class);
        TransactionCategory top = null;
        BigDecimal topVal = BigDecimal.ZERO;
        for (var e : totalByCategory.entrySet()) {
            BigDecimal monthly = e.getValue().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            monthlyByCategory.put(e.getKey(), monthly);
            if (monthly.compareTo(topVal) > 0) { topVal = monthly; top = e.getKey(); }
        }

        BigDecimal monthlySpending = spendingTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal monthlySaved = savedTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        double savingsRate = income.signum() > 0
                ? monthlySaved.divide(income, 6, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new SpendingProfile(income, months, monthlyByCategory,
                monthlySpending, monthlySaved, savingsRate, top);
    }
}
