package com.kathirha.web;

import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.GoalService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.dto.Requests;
import com.kathirha.web.dto.Views;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goals;
    private final AccountService accounts;

    public GoalController(GoalService goals, AccountService accounts) {
        this.goals = goals;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Views.GoalView> list(@AuthenticationPrincipal AppUserDetails principal) {
        return goals.listFor(accounts.require(principal)).stream().map(Views.GoalView::of).toList();
    }

    @PostMapping
    public Views.GoalView create(@AuthenticationPrincipal AppUserDetails principal,
                                 @Valid @RequestBody Requests.CreateGoalRequest req) {
        User user = accounts.require(principal);
        return Views.GoalView.of(goals.create(user, req.name(), req.targetAmount(), req.targetDate()));
    }

    @GetMapping("/{id}")
    public Views.GoalView get(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return Views.GoalView.of(goals.get(accounts.require(principal), id));
    }

    @PostMapping("/{id}/contribute")
    public Views.GoalView contribute(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                     @Valid @RequestBody Requests.ContributeRequest req) {
        return Views.GoalView.of(goals.contribute(accounts.require(principal), id, req.amount()));
    }

    @PostMapping("/{id}/rescue")
    public AiModels.GoalRescue rescue(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return goals.rescue(accounts.require(principal), id);
    }

    @PostMapping("/{id}/rescue/apply")
    public Views.GoalView applyRescue(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                      @Valid @RequestBody Requests.RescueRequest req) {
        return Views.GoalView.of(goals.applyRescue(accounts.require(principal), id, req.option()));
    }
}
