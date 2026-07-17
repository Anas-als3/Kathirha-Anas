package com.kathirha.web;

import com.kathirha.domain.Season;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.SeasonPassService;
import com.kathirha.service.SeasonService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final SeasonService seasons;
    private final SeasonPassService pass;
    private final AccountService accounts;

    public SeasonController(SeasonService seasons, SeasonPassService pass, AccountService accounts) {
        this.seasons = seasons;
        this.pass = pass;
        this.accounts = accounts;
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        Map<String, Object> out = new LinkedHashMap<>();
        seasons.current().ifPresentOrElse(s -> {
            out.put("active", true);
            out.put("id", s.getId());
            out.put("name", s.getName());
            out.put("theme", s.getTheme());
            out.put("description", s.getDescription());
            out.put("startDate", s.getStartDate() == null ? null : s.getStartDate().toString());
            out.put("endDate", s.getEndDate() == null ? null : s.getEndDate().toString());
        }, () -> out.put("active", false));
        return out;
    }

    /** The battle pass: seasonal points, level, and the 7-tier reward track. */
    @GetMapping("/pass")
    public Map<String, Object> pass(@AuthenticationPrincipal AppUserDetails principal) {
        return pass.pass(accounts.require(principal));
    }

    /** Claim an unlocked tier's reward (points / freeze / real coupon / badge / season card). */
    @PostMapping("/pass/claim/{tier}")
    public Map<String, Object> claim(@AuthenticationPrincipal AppUserDetails principal, @PathVariable int tier) {
        return pass.claim(accounts.require(principal), tier);
    }

    /** Activate كثّرها+ (demo). Production checks: salary transfer, savings balance, or subscription. */
    @PostMapping("/plus/activate")
    public Map<String, Object> activatePlus(@AuthenticationPrincipal AppUserDetails principal) {
        return pass.activatePlus(accounts.require(principal));
    }
}
