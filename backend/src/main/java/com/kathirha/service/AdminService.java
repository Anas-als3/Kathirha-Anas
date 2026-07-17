package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.MissionRepository;
import com.kathirha.repository.RedemptionRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.ai.AiInsightService;
import com.kathirha.service.ai.AiModels;
import org.springframework.stereotype.Service;

import java.util.*;

/** AI-generated product insights for the admin dashboard. */
@Service
public class AdminService {

    private final UserRepository users;
    private final TransactionService transactionService;
    private final MissionRepository missions;
    private final RedemptionRepository redemptions;
    private final AiInsightService insights;

    public AdminService(UserRepository users, TransactionService transactionService, MissionRepository missions,
                        RedemptionRepository redemptions, AiInsightService insights) {
        this.users = users;
        this.transactionService = transactionService;
        this.missions = missions;
        this.redemptions = redemptions;
        this.insights = insights;
    }

    public AiModels.AdminInsights productInsights() {
        List<User> all = users.findByRole(Role.USER);
        int total = all.size();
        int verified = 0;
        double sumRate = 0;
        int counted = 0;
        List<String> atRisk = new ArrayList<>();
        Map<TransactionCategory, Integer> topCatCount = new EnumMap<>(TransactionCategory.class);

        for (User u : all) {
            if (!u.isBankVerified()) continue;
            verified++;
            SpendingProfile p = SpendingProfile.from(u.getBaselineIncome(), transactionService.forUser(u));
            sumRate += p.savingsRatePercent();
            counted++;
            if (p.savingsRate < 0.15 || u.getCurrentStreak() == 0) {
                atRisk.add(u.getDisplayName() == null ? u.getPhone() : u.getDisplayName());
            }
            if (p.topSpendCategory != null) topCatCount.merge(p.topSpendCategory, 1, Integer::sum);
        }
        double avgRate = counted > 0 ? sumRate / counted : 0;

        // Most common spending problem
        TransactionCategory worst = topCatCount.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        // Most completed mission type
        Map<MissionType, Integer> completedByType = new EnumMap<>(MissionType.class);
        for (Mission m : missions.findAll()) {
            if (m.getStatus() == MissionStatus.COMPLETED) {
                completedByType.merge(m.getType(), 1, Integer::sum);
            }
        }
        MissionType topMission = completedByType.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        // Engagement driver (most redeemed reward)
        Map<String, Integer> rewardCounts = new HashMap<>();
        for (Redemption r : redemptions.findAllWithItem()) {
            rewardCounts.merge(r.getShopItem().getName(), 1, Integer::sum);
        }
        String topReward = rewardCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");

        List<AiModels.AdminInsightItem> items = new ArrayList<>();
        items.add(new AiModels.AdminInsightItem(
                "المستخدمون الأكثر عرضة للانقطاع",
                atRisk.isEmpty() ? "لا يوجد مستخدمون معرّضون للانقطاع — التفاعل بحالة ممتازة."
                        : String.join(", ", atRisk.subList(0, Math.min(5, atRisk.size())))
                        + " (نسبة ادّخار منخفضة أو سلسلة منقطعة).",
                atRisk.size() + " معرّضون للانقطاع"));
        items.add(new AiModels.AdminInsightItem(
                "أكثر مشاكل الصرف شيوعًا",
                worst == null ? "لا تتوفر بيانات كافية بعد."
                        : worst.label + " هي أعلى فئة صرف بين المستخدمين — هدف قوي للمهام وتنبيهات الاسترداد النقدي.",
                worst == null ? "—" : worst.label));
        items.add(new AiModels.AdminInsightItem(
                "نوع المهام الأكثر إنجازًا",
                topMission == null ? "لا توجد مهام مكتملة بعد."
                        : "المهام " + missionTypeLabel(topMission) + " هي الأكثر إنجازًا — ركّز عليها لتعزيز الاستمرارية.",
                topMission == null ? "—" : missionTypeLabel(topMission)));
        items.add(new AiModels.AdminInsightItem(
                "أقوى محفّز للتفاعل",
                "\"" + topReward + "\" هي المكافأة الأكثر استبدالًا — تعيد المستخدمين للادّخار من جديد.",
                topReward));

        AiModels.AdminInsights result = new AiModels.AdminInsights(total, verified, round1(avgRate), items);
        insights.save(null, InsightKind.ADMIN_INSIGHT, "رؤى المنتج",
                "متوسط نسبة الادّخار " + round1(avgRate) + "% لدى " + verified + " مستخدم موثّق.", result,
                AiSource.DETERMINISTIC);
        return result;
    }

    private String missionTypeLabel(MissionType t) {
        return switch (t) {
            case DAILY -> "اليومية";
            case WEEKLY -> "الأسبوعية";
            case MONTHLY -> "الشهرية";
            case PAYDAY -> "يوم الراتب";
            case EMERGENCY -> "الطارئة";
            case SURVEY -> "الاستبيانات";
            case LEARN -> "التعليمية";
            case SOCIAL -> "الاجتماعية";
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
