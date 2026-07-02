package com.kathirha.web;

import com.kathirha.domain.PointsType;
import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.PointsService;
import com.kathirha.service.ShopService;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.dto.Views;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shop;
    private final PointsService points;
    private final AccountService accounts;

    public ShopController(ShopService shop, PointsService points, AccountService accounts) {
        this.shop = shop;
        this.points = points;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Views.ShopItemView> list(@AuthenticationPrincipal AppUserDetails principal) {
        User user = accounts.require(principal);
        int normal = points.balance(user, PointsType.NORMAL);
        int seasonal = points.balance(user, PointsType.SEASONAL);
        return shop.list().stream().map(i -> {
            int bal = i.getPointsType() == PointsType.SEASONAL ? seasonal : normal;
            return Views.ShopItemView.of(i, bal >= i.getCostPoints());
        }).toList();
    }

    @GetMapping("/redemptions")
    public List<Views.RedemptionView> redemptions(@AuthenticationPrincipal AppUserDetails principal) {
        return shop.redemptionsFor(accounts.require(principal)).stream().map(Views.RedemptionView::of).toList();
    }

    @PostMapping("/{id}/redeem")
    public Views.RedemptionView redeem(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return Views.RedemptionView.of(shop.redeem(accounts.require(principal), id));
    }

    @GetMapping("/recommendation")
    public AiModels.RewardRecommendation recommendation(@AuthenticationPrincipal AppUserDetails principal) {
        User user = accounts.require(principal);
        return shop.recommend(user, points.balance(user, PointsType.NORMAL));
    }
}
