package com.kathirha.service;

import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;
import com.kathirha.domain.User;
import com.kathirha.repository.TransactionRepository;
import com.kathirha.service.ai.AiModels;
import com.kathirha.service.bank.MockOpenBankingProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final MockOpenBankingProvider bank;

    public TransactionService(TransactionRepository transactions, MockOpenBankingProvider bank) {
        this.transactions = transactions;
        this.bank = bank;
    }

    @Transactional
    public int importTransactions(User user, BigDecimal monthlyIncome, int months,
                                  MockOpenBankingProvider.Preset preset) {
        return importTransactions(user, monthlyIncome, months, preset, -1);
    }

    @Transactional
    public int importTransactions(User user, BigDecimal monthlyIncome, int months,
                                  MockOpenBankingProvider.Preset preset, double savingsRateOverride) {
        // Re-import replaces the previous window — appending would double income and spending.
        if (transactions.countByUser(user) > 0) transactions.deleteByUser(user);
        List<Transaction> generated = bank.generate(user, monthlyIncome, months, preset, savingsRateOverride);
        transactions.saveAll(generated);
        return generated.size();
    }

    public List<Transaction> forUser(User user) {
        return transactions.findByUserOrderByDateDesc(user);
    }

    public SpendingProfile profileFor(User user) {
        return SpendingProfile.from(user.getBaselineIncome(), forUser(user));
    }

    /** Category breakdown of spending (for the dashboard), sorted by amount descending. */
    public List<AiModels.CategoryAmount> breakdown(User user) {
        SpendingProfile p = profileFor(user);
        BigDecimal total = p.monthlySpending.signum() > 0 ? p.monthlySpending : BigDecimal.ONE;
        List<AiModels.CategoryAmount> out = new ArrayList<>();
        for (var e : p.monthlyByCategory.entrySet()) {
            TransactionCategory c = e.getKey();
            BigDecimal amount = e.getValue();
            double share = amount.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100;
            out.add(new AiModels.CategoryAmount(c.name(), c.label, c.emoji, amount, share));
        }
        out.sort(Comparator.comparing(AiModels.CategoryAmount::amount).reversed());
        return out;
    }

    @Transactional
    public void clearFor(User user) {
        transactions.deleteByUser(user);
    }

    public long countFor(User user) {
        return transactions.countByUser(user);
    }
}
