package com.kathirha.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kathirha.domain.AiInsight;
import com.kathirha.domain.AiSource;
import com.kathirha.domain.InsightKind;
import com.kathirha.domain.User;
import com.kathirha.repository.AiInsightRepository;
import org.springframework.stereotype.Service;

/** Persists every AI output and optionally enhances narrative text via OpenAI. */
@Service
public class AiInsightService {

    private final AiInsightRepository repo;
    private final OpenAiClient openai;
    private final ObjectMapper mapper;

    public AiInsightService(AiInsightRepository repo, OpenAiClient openai, ObjectMapper mapper) {
        this.repo = repo;
        this.openai = openai;
        this.mapper = mapper;
    }

    public record Narration(String text, AiSource source) {}

    /** Enhance deterministic narrative text with OpenAI when available; otherwise pass through. */
    public Narration narrate(String instruction, String deterministicText) {
        return openai.rephrase(instruction, deterministicText)
                .map(t -> new Narration(t, AiSource.OPENAI))
                .orElse(new Narration(deterministicText, AiSource.DETERMINISTIC));
    }

    public AiInsight save(User user, InsightKind kind, String title, String body,
                          Object payload, AiSource source) {
        AiInsight insight = new AiInsight();
        insight.setUser(user);
        insight.setKind(kind);
        insight.setTitle(title);
        insight.setBody(body);
        insight.setPayloadJson(toJson(payload));
        insight.setSource(source);
        return repo.save(insight);
    }

    public String toJson(Object payload) {
        if (payload == null) return null;
        try {
            String s = mapper.writeValueAsString(payload);
            return s.length() > 3900 ? s.substring(0, 3900) : s;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public boolean openAiActive() {
        return openai.isConfigured();
    }
}
