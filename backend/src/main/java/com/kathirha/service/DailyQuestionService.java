package com.kathirha.service;

import com.kathirha.domain.DailyQuestion;
import com.kathirha.domain.PointsReason;
import com.kathirha.domain.PointsType;
import com.kathirha.domain.User;
import com.kathirha.repository.DailyQuestionRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiModels;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
public class DailyQuestionService {

    /** Seasonal bonus per correct answer — feeds the battle pass, never the leaderboard score. */
    private static final int SEASONAL_BONUS = 5;

    private final DailyQuestionRepository questions;
    private final TransactionService transactionService;
    private final AiCoach coach;
    private final PointsService points;
    private final SeasonService seasons;

    public DailyQuestionService(DailyQuestionRepository questions, TransactionService transactionService,
                                AiCoach coach, PointsService points, SeasonService seasons) {
        this.questions = questions;
        this.transactionService = transactionService;
        this.coach = coach;
        this.points = points;
        this.seasons = seasons;
    }

    @Transactional
    public DailyQuestion today(User user) {
        LocalDate today = LocalDate.now();
        return questions.findByUserAndQuestionDate(user, today).orElseGet(() -> {
            SpendingProfile p = transactionService.profileFor(user);
            long seed = (user.getId() == null ? 1L : user.getId()) * 31 + today.toEpochDay();
            AiModels.QuizSpec spec = coach.dailyQuiz(p, seed);
            DailyQuestion q = new DailyQuestion();
            q.setUser(user);
            q.setQuestionDate(today);
            q.setPrompt(spec.prompt());
            q.setOptions(new ArrayList<>(spec.options()));
            q.setCorrectIndex(spec.correctIndex());
            q.setExplanation(spec.explanation());
            q.setRewardPoints(spec.rewardPoints());
            return questions.save(q);
        });
    }

    @Transactional
    public AnswerResult answer(User user, int index) {
        DailyQuestion q = today(user);
        if (q.isAnswered()) {
            throw new ApiExceptions.BadRequestException("أجبت على سؤال اليوم بالفعل — عد غدًا لسؤال جديد");
        }
        boolean correct = index == q.getCorrectIndex();
        q.setAnswered(true);
        q.setAnsweredIndex(index);
        q.setAnsweredCorrect(correct);
        questions.save(q);

        int awarded = 0;
        int balance = points.balance(user, PointsType.NORMAL);
        if (correct) {
            awarded = q.getRewardPoints();
            balance = points.award(user, awarded, PointsType.NORMAL, PointsReason.QUIZ,
                    "إجابة صحيحة على سؤال اليوم", null);
            Long seasonId = seasons.currentSeasonId();
            if (seasonId != null) {
                points.award(user, SEASONAL_BONUS, PointsType.SEASONAL, PointsReason.QUIZ,
                        "سؤال اليوم — نقاط الموسم", seasonId);
            }
        }
        return new AnswerResult(correct, q.getCorrectIndex(), q.getExplanation(), awarded, balance);
    }

    public record AnswerResult(boolean correct, int correctIndex, String explanation,
                               int pointsAwarded, int newBalance) {}
}
