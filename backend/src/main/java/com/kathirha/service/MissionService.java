package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.MissionRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MissionService {

    private final MissionRepository missions;
    private final TransactionService transactionService;
    private final AiCoach coach;
    private final PointsService points;
    private final StreakService streaks;
    private final SeasonService seasons;

    public MissionService(MissionRepository missions, TransactionService transactionService, AiCoach coach,
                          PointsService points, StreakService streaks, SeasonService seasons) {
        this.missions = missions;
        this.transactionService = transactionService;
        this.coach = coach;
        this.points = points;
        this.streaks = streaks;
        this.seasons = seasons;
    }

    @Transactional
    public List<Mission> generateFor(User user) {
        SpendingProfile p = transactionService.profileFor(user);
        IncomeLeague league = user.getIncomeLeague() == null ? IncomeLeague.STARTER : user.getIncomeLeague();
        List<Mission> created = new ArrayList<>();
        for (AiModels.MissionSpec s : coach.generateMissions(p, league)) {
            Mission m = new Mission();
            m.setUser(user);
            m.setTitle(s.title());
            m.setDescription(s.description());
            m.setType(MissionType.valueOf(s.type()));
            m.setDifficulty(MissionDifficulty.valueOf(s.difficulty()));
            m.setRewardPoints(s.rewardPoints());
            m.setPointsType(PointsType.NORMAL);
            m.setStatus(MissionStatus.ACTIVE);
            if (s.targetCategory() != null) m.setTargetCategory(TransactionCategory.valueOf(s.targetCategory()));
            m.setTargetAmount(s.targetAmount());
            m.setAiGenerated(true);
            m.setDueDate(dueDateFor(m.getType()));
            created.add(missions.save(m));
        }
        return created;
    }

    public List<Mission> listFor(User user) {
        return missions.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Mission> activeFor(User user) {
        return missions.findByUserAndStatus(user, MissionStatus.ACTIVE);
    }

    /** Ensure the user has missions to act on (used after demo seeding / first load). */
    @Transactional
    public List<Mission> ensure(User user) {
        List<Mission> active = activeFor(user);
        if (active.isEmpty()) return generateFor(user);
        return active;
    }

    @Transactional
    public CompleteResult complete(User user, Long missionId) {
        Mission m = missions.findById(missionId)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiExceptions.NotFoundException("المهمة غير موجودة"));
        if (m.getStatus() != MissionStatus.ACTIVE) {
            throw new ApiExceptions.BadRequestException("المهمة لم تعد نشطة — حالتها الحالية: " + m.getStatus().name().toLowerCase());
        }
        m.setStatus(MissionStatus.COMPLETED);
        m.setCompletedAt(Instant.now());
        missions.save(m);

        Long seasonId = m.getPointsType() == PointsType.SEASONAL ? seasons.currentSeasonId() : null;
        int balance = points.award(user, m.getRewardPoints(), m.getPointsType(),
                PointsReason.MISSION, "مهمة: " + m.getTitle(), seasonId);
        AiModels.StreakCoachMessage streak = streaks.recordSaving(user);

        return new CompleteResult(m, m.getRewardPoints(), m.getPointsType().name(), balance, streak.message());
    }

    public record CompleteResult(Mission mission, int pointsAwarded, String pointsType,
                                 int newBalance, String streakMessage) {}

    private static LocalDate dueDateFor(MissionType type) {
        return switch (type) {
            case DAILY -> LocalDate.now().plusDays(1);
            case WEEKLY -> LocalDate.now().plusDays(7);
            case PAYDAY -> LocalDate.now().plusDays(3);
            case MONTHLY, EMERGENCY -> LocalDate.now().plusDays(30);
        };
    }
}
