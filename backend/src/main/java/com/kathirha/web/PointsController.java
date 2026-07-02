package com.kathirha.web;

import com.kathirha.domain.PointsType;
import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.PointsService;
import com.kathirha.web.dto.Views;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    private final PointsService points;
    private final AccountService accounts;

    public PointsController(PointsService points, AccountService accounts) {
        this.points = points;
        this.accounts = accounts;
    }

    @GetMapping
    public Views.PointsView points(@AuthenticationPrincipal AppUserDetails principal) {
        User user = accounts.require(principal);
        return new Views.PointsView(
                points.balance(user, PointsType.NORMAL),
                points.balance(user, PointsType.SEASONAL),
                points.recent(user).stream().map(Views.LedgerView::of).toList());
    }
}
