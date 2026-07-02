package com.kathirha.web;

import com.kathirha.service.AdminService;
import com.kathirha.service.NotificationService;
import com.kathirha.service.ai.AiModels;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** ROLE_ADMIN only (enforced by SecurityConfig path rule for /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final NotificationService notifications;

    public AdminController(AdminService admin, NotificationService notifications) {
        this.admin = admin;
        this.notifications = notifications;
    }

    @GetMapping("/insights")
    public AiModels.AdminInsights insights() {
        return admin.productInsights();
    }

    /** Broadcast today's daily question to every user over WhatsApp (also runs on a daily cron). */
    @PostMapping("/notify/daily-question")
    public Map<String, Object> notifyDailyQuestion() {
        return Map.of("sent", notifications.pushDailyQuestionToAll());
    }
}
