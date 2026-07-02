package com.kathirha.service.bank;

import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;
import com.kathirha.domain.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock open-banking provider. Simulates a consent flow and generates realistic Saudi-style
 * transactions. NEVER asks for real bank credentials — the architecture is ready to swap in a
 * real provider (Tarabut/Lean) behind the same shape, but the demo runs fully offline.
 */
@Component
public class MockOpenBankingProvider {

    public record ConsentResult(String consentId, String bankName, String accountMask, String status) {}

    /** A spending personality preset that shapes the generated data. */
    public enum Preset {
        BALANCED(0.30, w(0.25, 0.15, 0.08, 0.10, 0.07, 0.20, 0.15), null),
        COFFEE_HEAVY(0.22, w(0.22, 0.14, 0.18, 0.10, 0.06, 0.16, 0.14), TransactionCategory.COFFEE),
        FOOD_HEAVY(0.18, w(0.20, 0.28, 0.08, 0.10, 0.08, 0.12, 0.14), TransactionCategory.FOOD_DELIVERY);

        final double savingsRate;
        final double[] weights; // groceries, food, coffee, transport, entertainment, shopping, bills
        final TransactionCategory spikeCategory; // anomaly in the latest month

        Preset(double savingsRate, double[] weights, TransactionCategory spike) {
            this.savingsRate = savingsRate;
            this.weights = weights;
            this.spikeCategory = spike;
        }
    }

    private static double[] w(double... v) { return v; }

    private static final TransactionCategory[] SPEND_CATS = {
            TransactionCategory.GROCERIES, TransactionCategory.FOOD_DELIVERY, TransactionCategory.COFFEE,
            TransactionCategory.TRANSPORT, TransactionCategory.ENTERTAINMENT, TransactionCategory.SHOPPING,
            TransactionCategory.BILLS
    };

    private static final String[][] MERCHANTS = {
            {"Tamimi Markets", "Danube", "Panda", "Carrefour"},          // groceries
            {"Jahez", "HungerStation", "ToYou", "Mrsool"},               // food delivery
            {"Barn's", "Dunkin", "Starbucks", "Half Million"},           // coffee
            {"Uber", "Careem", "Petromin", "ADNOC"},                     // transport
            {"VOX Cinemas", "AMC", "Boulevard", "Steam"},                // entertainment
            {"Jarir", "Amazon.sa", "Noon", "Centrepoint"},               // shopping
            {"STC", "Saudi Electricity Co", "Mobily", "National Water"}  // bills
    };

    public ConsentResult connect(User user) {
        String mask = "SA**" + String.format("%04d", Math.abs((user.getPhone()).hashCode()) % 10000);
        String consentId = "consent_" + Math.abs(user.hashCode());
        return new ConsentResult(consentId, "مصرف الراجحي (تجريبي)", mask, "GRANTED");
    }

    public List<Transaction> generate(User user, BigDecimal monthlyIncome, int months, Preset preset) {
        return generate(user, monthlyIncome, months, preset, -1);
    }

    /**
     * @param savingsRateOverride if {@code > 0}, used instead of the preset's default savings rate
     *                            (lets us build a varied, realistic leaderboard).
     */
    public List<Transaction> generate(User user, BigDecimal monthlyIncome, int months, Preset preset,
                                      double savingsRateOverride) {
        long seed = user.getPhone() != null ? user.getPhone().hashCode() : 42L;
        Random rnd = new Random(seed);
        List<Transaction> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        double income = monthlyIncome.doubleValue();
        double rate = savingsRateOverride > 0 ? savingsRateOverride : preset.savingsRate;
        double monthlySpend = income * (1 - rate);
        double monthlySave = income * rate;

        for (int m = months - 1; m >= 0; m--) {
            LocalDate monthAnchor = today.minusMonths(m);
            int year = monthAnchor.getYear();
            int month = monthAnchor.getMonthValue();
            int maxDay = Math.min(28, today.lengthOfMonth());
            boolean latestMonth = (m == 0);

            // Salary credit (income) on the 27th
            out.add(tx(user, LocalDate.of(year, month, Math.min(27, maxDay)),
                    "راتب - Acme Co", "Acme Co", BigDecimal.valueOf(income), TransactionCategory.SALARY));

            // Savings transfers (2 per month)
            double saveEach = monthlySave / 2.0;
            for (int i = 0; i < 2; i++) {
                out.add(tx(user, randomDay(year, month, maxDay, rnd),
                        "تحويل إلى الادّخار", "توفير الراجحي",
                        neg(saveEach * jitter(rnd, 0.15)), TransactionCategory.SAVINGS_TRANSFER));
            }

            // Spending by category
            for (int c = 0; c < SPEND_CATS.length; c++) {
                TransactionCategory cat = SPEND_CATS[c];
                double catMonthly = monthlySpend * preset.weights[c];
                if (latestMonth && cat == preset.spikeCategory) catMonthly *= 1.45; // anomaly spike
                int count = switch (cat) {
                    case COFFEE -> 14;
                    case FOOD_DELIVERY -> 9;
                    case GROCERIES -> 6;
                    case TRANSPORT -> 6;
                    case BILLS -> 2;
                    default -> 3;
                };
                double each = catMonthly / count;
                for (int i = 0; i < count; i++) {
                    String merchant = MERCHANTS[c][rnd.nextInt(MERCHANTS[c].length)];
                    out.add(tx(user, randomDay(year, month, maxDay, rnd),
                            merchant, merchant, neg(each * jitter(rnd, 0.35)), cat));
                }
            }
        }
        return out;
    }

    private static Transaction tx(User user, LocalDate date, String desc, String merchant,
                                  BigDecimal amount, TransactionCategory cat) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setDate(date);
        t.setDescription(desc);
        t.setMerchant(merchant);
        t.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        t.setCategory(cat);
        return t;
    }

    private static LocalDate randomDay(int year, int month, int maxDay, Random rnd) {
        return LocalDate.of(year, month, 1 + rnd.nextInt(maxDay));
    }

    private static double jitter(Random rnd, double spread) {
        return 1.0 - spread + rnd.nextDouble() * (2 * spread);
    }

    private static BigDecimal neg(double v) {
        return BigDecimal.valueOf(-Math.abs(v));
    }
}
