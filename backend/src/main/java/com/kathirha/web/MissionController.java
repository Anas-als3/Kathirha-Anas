package com.kathirha.web;

import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.MissionService;
import com.kathirha.web.dto.Views;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missions;
    private final AccountService accounts;

    public MissionController(MissionService missions, AccountService accounts) {
        this.missions = missions;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Views.MissionView> list(@AuthenticationPrincipal AppUserDetails principal) {
        return missions.listFor(accounts.require(principal)).stream().map(Views.MissionView::of).toList();
    }

    @PostMapping("/generate")
    public List<Views.MissionView> generate(@AuthenticationPrincipal AppUserDetails principal) {
        return missions.generateFor(accounts.require(principal)).stream().map(Views.MissionView::of).toList();
    }

    @PostMapping("/{id}/complete")
    public Map<String, Object> complete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        User user = accounts.require(principal);
        MissionService.CompleteResult r = missions.complete(user, id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mission", Views.MissionView.of(r.mission()));
        out.put("pointsAwarded", r.pointsAwarded());
        out.put("pointsType", r.pointsType());
        out.put("newBalance", r.newBalance());
        out.put("streakMessage", r.streakMessage());
        return out;
    }
}
