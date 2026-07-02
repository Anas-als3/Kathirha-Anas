package com.kathirha.web;

import com.kathirha.domain.GoalStatus;
import com.kathirha.domain.MessageCategory;
import com.kathirha.domain.SavingsGoal;
import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.GoalService;
import com.kathirha.service.NotificationService;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.dto.Requests;
import com.kathirha.web.dto.Views;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mock WhatsApp simulator + two-way replies (e.g. choosing a goal-rescue option by replying 1/2). */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsapp;
    private final GoalService goals;
    private final AccountService accounts;
    private final NotificationService notifications;

    public WhatsAppController(WhatsAppService whatsapp, GoalService goals, AccountService accounts,
                             NotificationService notifications) {
        this.whatsapp = whatsapp;
        this.goals = goals;
        this.accounts = accounts;
        this.notifications = notifications;
    }

    @GetMapping
    public List<Views.WhatsAppView> inbox(@AuthenticationPrincipal AppUserDetails principal) {
        return whatsapp.inbox(accounts.require(principal)).stream().map(Views.WhatsAppView::of).toList();
    }

    /** Push today's daily question to the current user's WhatsApp now (for live testing). */
    @PostMapping("/send-daily-question")
    public List<Views.WhatsAppView> sendDailyQuestion(@AuthenticationPrincipal AppUserDetails principal) {
        User user = accounts.require(principal);
        notifications.pushDailyQuestion(user);
        return whatsapp.inbox(user).stream().map(Views.WhatsAppView::of).toList();
    }

    @PostMapping("/reply")
    public Map<String, Object> reply(@AuthenticationPrincipal AppUserDetails principal,
                                     @Valid @RequestBody Requests.ReplyRequest req) {
        User user = accounts.require(principal);
        String body = req.body().trim();
        String action = "received";

        Optional<SavingsGoal> behind = goals.listFor(user).stream()
                .filter(g -> g.getStatus() == GoalStatus.BEHIND).findFirst();
        boolean rescueChoice = body.equals("1") || body.equals("2")
                || body.equalsIgnoreCase("extend") || body.equalsIgnoreCase("increase");

        if (behind.isPresent() && rescueChoice) {
            goals.applyRescue(user, behind.get().getId(), body); // records the inbound + confirmation
            action = "goal_rescue_applied";
        } else {
            whatsapp.recordInbound(user, MessageCategory.GENERAL, body, null);
            whatsapp.send(user, MessageCategory.GENERAL,
                    "Thanks! Open the Kathirha app to keep growing your savings 🌱", null);
            action = "auto_reply";
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", action);
        out.put("messages", whatsapp.inbox(user).stream().map(Views.WhatsAppView::of).toList());
        return out;
    }
}
