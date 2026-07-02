package com.kathirha.service;

import com.kathirha.domain.User;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiModels;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saving streaks with a freeze safety-net, plus the AI streak coach message. */
@Service
public class StreakService {

    private final UserRepository users;
    private final AiCoach coach;

    public StreakService(UserRepository users, AiCoach coach) {
        this.users = users;
        this.coach = coach;
    }

    @Transactional
    public AiModels.StreakCoachMessage recordSaving(User user) {
        user.setCurrentStreak(user.getCurrentStreak() + 1);
        user.setLongestStreak(Math.max(user.getLongestStreak(), user.getCurrentStreak()));
        users.save(user);
        return coach.streakCoach(user.getCurrentStreak(), false);
    }

    @Transactional
    public AiModels.StreakCoachMessage useFreeze(User user) {
        if (user.getStreakFreezes() > 0) {
            user.setStreakFreezes(user.getStreakFreezes() - 1);
            users.save(user);
            return coach.streakCoach(user.getCurrentStreak(), true);
        }
        // no freeze left -> streak resets
        user.setCurrentStreak(0);
        users.save(user);
        return new AiModels.StreakCoachMessage(
                "لم يتبقَّ لديك تجميد للسلسلة — عادت سلسلتك إلى 0. ادّخر اليوم لتبدأ سلسلة جديدة!", false);
    }
}
