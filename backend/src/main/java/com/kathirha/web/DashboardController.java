package com.kathirha.web;

import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final AccountService accounts;

    public DashboardController(DashboardService dashboard, AccountService accounts) {
        this.dashboard = dashboard;
        this.accounts = accounts;
    }

    @GetMapping
    public Map<String, Object> dashboard(@AuthenticationPrincipal AppUserDetails principal) {
        return dashboard.build(accounts.require(principal));
    }
}
