package com.kathirha.web;

import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.LeaderboardService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.dto.Views;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboard;
    private final AccountService accounts;

    public LeaderboardController(LeaderboardService leaderboard, AccountService accounts) {
        this.leaderboard = leaderboard;
        this.accounts = accounts;
    }

    @GetMapping
    public Views.LeaderboardResponse leaderboard(@AuthenticationPrincipal AppUserDetails principal) {
        return leaderboard.leaderboard(accounts.require(principal));
    }

    @GetMapping("/explain")
    public AiModels.LeaderboardExplanation explain(@AuthenticationPrincipal AppUserDetails principal) {
        return leaderboard.explain(accounts.require(principal));
    }
}
