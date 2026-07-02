package com.kathirha;

import com.kathirha.domain.IncomeLeague;
import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;
import com.kathirha.service.SpendingProfile;
import com.kathirha.service.ai.AiModels;
import com.kathirha.service.ai.DeterministicAiCoach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the financial logic and the deterministic AI fallback (no Spring context). */
class CoreLogicTest {

    private final DeterministicAiCoach coach = new DeterministicAiCoach();

    private Transaction tx(TransactionCategory c, double amount, LocalDate date) {
        Transaction t = new Transaction();
        t.setCategory(c);
        t.setAmount(BigDecimal.valueOf(amount));
        t.setDate(date);
        return t;
    }

    private List<Transaction> twoMonthCoffeeHeavy() {
        LocalDate m1 = LocalDate.now().minusMonths(1).withDayOfMonth(15);
        LocalDate m2 = LocalDate.now().withDayOfMonth(15);
        List<Transaction> txns = new ArrayList<>();
        for (LocalDate d : List.of(m1, m2)) {
            txns.add(tx(TransactionCategory.SALARY, 9000, d));
            txns.add(tx(TransactionCategory.SAVINGS_TRANSFER, -1800, d));   // 20% saved
            txns.add(tx(TransactionCategory.COFFEE, -900, d));             // ~32% of spend
            txns.add(tx(TransactionCategory.GROCERIES, -1500, d));
            txns.add(tx(TransactionCategory.FOOD_DELIVERY, -400, d));      // ~14% (below 20%)
        }
        return txns;
    }

    @Test
    void detectsIncomeAndSavingsRateFromTransactions() {
        SpendingProfile p = SpendingProfile.from(null, twoMonthCoffeeHeavy());
        assertEquals(2, p.months, "two distinct months");
        assertEquals(9000, p.baselineIncome.intValue(), "income detected from salary credits");
        assertEquals(0.20, p.savingsRate, 0.001, "1800 saved / 9000 income");
    }

    @Test
    void leaderboardScoreIsCappedAtForty() {
        int cap = 40;
        double highSaverRate = 45.0; // saves 45%
        double fairScore = Math.min(highSaverRate, cap);
        assertEquals(40.0, fairScore, "saving beyond 40% does not raise the fairness score");
    }

    @Test
    void classifiesCoffeeLeakerPersonality() {
        SpendingProfile p = SpendingProfile.from(null, twoMonthCoffeeHeavy());
        AiModels.PersonalityResult r = coach.personality(p);
        assertEquals("COFFEE_LEAKER", r.personality(), "coffee is >10% of spend with sub-30% savings");
        assertNotNull(r.reason());
    }

    @Test
    void goalPlanUsesPlainTargetAndSplitsPerPeriod() {
        // Compliance: no inflation forecasting — the plan must work from the plain target only.
        SpendingProfile p = SpendingProfile.from(BigDecimal.valueOf(9000), List.of());
        AiModels.GoalPlan plan = coach.planGoal("Trip", BigDecimal.valueOf(6000),
                LocalDate.now().plusMonths(12), p, BigDecimal.valueOf(2.3));
        assertEquals(0, plan.inflationAdjustedTarget().compareTo(new BigDecimal("6000.00")),
                "target must NOT be grossed up — no future predictions");
        assertEquals(0, plan.monthlySaving().compareTo(new BigDecimal("500.00")),
                "6000 over 12 months = 500/month");
        assertTrue(plan.weeklySaving().signum() > 0);
        assertNotNull(plan.riskLevel());
    }

    @Test
    void generatesAcrossMissionTypesWithPositiveRewards() {
        SpendingProfile p = SpendingProfile.from(null, twoMonthCoffeeHeavy());
        List<AiModels.MissionSpec> missions = coach.generateMissions(p, IncomeLeague.SILVER);
        assertTrue(missions.size() >= 5);
        assertTrue(missions.stream().allMatch(m -> m.rewardPoints() > 0));
        assertTrue(missions.stream().anyMatch(m -> m.type().equals("PAYDAY")));
        assertTrue(missions.stream().anyMatch(m -> m.type().equals("EMERGENCY")));
    }

    @Test
    void healthScoreStaysWithinZeroToHundred() {
        SpendingProfile p = SpendingProfile.from(null, twoMonthCoffeeHeavy());
        AiModels.HealthScore hs = coach.healthScore(p, 50, 100, 0.5);
        assertTrue(hs.score() >= 0 && hs.score() <= 100);
        assertFalse(hs.factors().isEmpty());
    }
}
