package com.kathirha.web;

import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.CashbackService;
import com.kathirha.service.ai.AiModels;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cashback")
public class CashbackController {

    private final CashbackService cashback;
    private final AccountService accounts;

    public CashbackController(CashbackService cashback, AccountService accounts) {
        this.cashback = cashback;
        this.accounts = accounts;
    }

    @GetMapping
    public AiModels.CashbackRecommendation recommendation(@AuthenticationPrincipal AppUserDetails principal) {
        return cashback.recommend(accounts.require(principal));
    }
}
