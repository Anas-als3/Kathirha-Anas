package com.kathirha.service;

import com.kathirha.domain.User;
import com.kathirha.repository.UserRepository;
import com.kathirha.security.AppUserDetails;
import com.kathirha.web.ApiExceptions;
import com.kathirha.web.dto.Views;
import org.springframework.stereotype.Service;

/** Resolves the current authenticated user (fresh from the DB) and builds the canonical user view. */
@Service
public class AccountService {

    private final UserRepository users;
    private final TransactionService transactionService;

    public AccountService(UserRepository users, TransactionService transactionService) {
        this.users = users;
        this.transactionService = transactionService;
    }

    public User require(AppUserDetails principal) {
        if (principal == null) throw new ApiExceptions.UnauthorizedException("Not authenticated");
        return users.findById(principal.getUser().getId())
                .orElseThrow(() -> new ApiExceptions.UnauthorizedException("Account not found"));
    }

    public double savingsRatePercent(User u) {
        return SpendingProfile.from(u.getBaselineIncome(), transactionService.forUser(u)).savingsRatePercent();
    }

    public Views.UserView meView(User u) {
        return Views.UserView.of(u, savingsRatePercent(u));
    }
}
