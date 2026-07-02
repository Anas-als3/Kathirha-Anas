package com.kathirha.config;

import com.kathirha.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fires the daily WhatsApp question on a cron schedule (default 09:00 every day). */
@Component
public class NotificationScheduler {

    private final NotificationService notifications;

    public NotificationScheduler(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Scheduled(cron = "${kathirha.schedule.daily-question-cron:0 0 9 * * *}", zone = "Asia/Riyadh")
    public void dailyQuestion() {
        notifications.pushDailyQuestionToAll();
    }
}
