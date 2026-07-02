package com.kathirha.service;

import com.kathirha.domain.IncomeLeague;
import com.kathirha.domain.IntegrityStatus;
import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;
import com.kathirha.domain.User;
import com.kathirha.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects a verified baseline income from real salary credits and assigns an income league.
 * This is what makes competition fair and tamper-resistant — income is derived, not self-reported.
 */
@Service
public class IncomeService {

    private final TransactionService transactionService;
    private final UserRepository users;

    public IncomeService(TransactionService transactionService, UserRepository users) {
        this.transactionService = transactionService;
        this.users = users;
    }

    @Transactional
    public BigDecimal detectAndVerify(User user) {
        List<Transaction> txns = transactionService.forUser(user);
        BigDecimal salaryTotal = BigDecimal.ZERO;
        Set<YearMonth> salaryMonths = new HashSet<>();
        for (Transaction t : txns) {
            if (t.getCategory() == TransactionCategory.SALARY) {
                salaryTotal = salaryTotal.add(t.absAmount());
                if (t.getDate() != null) salaryMonths.add(YearMonth.from(t.getDate()));
            }
        }
        if (salaryTotal.signum() <= 0) {
            return user.getBaselineIncome(); // nothing detected; leave as-is
        }
        int months = Math.max(1, salaryMonths.size());
        BigDecimal baseline = salaryTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        user.setBaselineIncome(baseline);
        user.setIncomeLeague(IncomeLeague.fromIncome(baseline));
        user.setBankVerified(true);
        user.setPhoneVerified(true);
        if (user.getIntegrityStatus() == IntegrityStatus.NEW) {
            user.setIntegrityStatus(IntegrityStatus.VERIFIED);
        }
        users.save(user);
        return baseline;
    }
}
