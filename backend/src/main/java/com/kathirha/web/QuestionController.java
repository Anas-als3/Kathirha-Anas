package com.kathirha.web;

import com.kathirha.domain.DailyQuestion;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.DailyQuestionService;
import com.kathirha.web.dto.Requests;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final DailyQuestionService questions;
    private final AccountService accounts;

    public QuestionController(DailyQuestionService questions, AccountService accounts) {
        this.questions = questions;
        this.accounts = accounts;
    }

    @GetMapping("/today")
    public Map<String, Object> today(@AuthenticationPrincipal AppUserDetails principal) {
        DailyQuestion q = questions.today(accounts.require(principal));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", q.getId());
        out.put("prompt", q.getPrompt());
        out.put("options", q.getOptions());
        out.put("rewardPoints", q.getRewardPoints());
        out.put("answered", q.isAnswered());
        out.put("answeredIndex", q.getAnsweredIndex());
        // Only reveal the answer after the user has responded.
        if (q.isAnswered()) {
            out.put("correctIndex", q.getCorrectIndex());
            out.put("explanation", q.getExplanation());
            out.put("answeredCorrect", q.isAnsweredCorrect());
        }
        return out;
    }

    @PostMapping("/answer")
    public DailyQuestionService.AnswerResult answer(@AuthenticationPrincipal AppUserDetails principal,
                                                    @Valid @RequestBody Requests.AnswerRequest req) {
        return questions.answer(accounts.require(principal), req.index());
    }
}
