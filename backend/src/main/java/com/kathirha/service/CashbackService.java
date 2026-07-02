package com.kathirha.service;

import com.kathirha.domain.InsightKind;
import com.kathirha.domain.User;
import com.kathirha.repository.CashbackCardRepository;
import com.kathirha.service.ai.AiCoach;
import com.kathirha.service.ai.AiInsightService;
import com.kathirha.service.ai.AiModels;
import org.springframework.stereotype.Service;

@Service
public class CashbackService {

    private final CashbackCardRepository cards;
    private final AiCoach coach;
    private final TransactionService transactionService;
    private final AiInsightService insights;

    public CashbackService(CashbackCardRepository cards, AiCoach coach,
                           TransactionService transactionService, AiInsightService insights) {
        this.cards = cards;
        this.coach = coach;
        this.transactionService = transactionService;
        this.insights = insights;
    }

    public AiModels.CashbackRecommendation recommend(User user) {
        SpendingProfile p = transactionService.profileFor(user);
        AiModels.CashbackRecommendation rec = coach.recommendCashback(p, cards.findAll());
        AiInsightService.Narration n = insights.narrate("This recommends a cashback card.", rec.reason());
        AiModels.CashbackRecommendation result = new AiModels.CashbackRecommendation(
                rec.cardName(), rec.emoji(), rec.category(), rec.estimatedMonthlySaving(), n.text());
        insights.save(user, InsightKind.CASHBACK, "أفضل بطاقة: " + rec.cardName(), n.text(), result, n.source());
        return result;
    }
}
