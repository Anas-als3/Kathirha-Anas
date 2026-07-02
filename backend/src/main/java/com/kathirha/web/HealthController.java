package com.kathirha.web;

import com.kathirha.service.ai.AiInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final AiInsightService ai;

    public HealthController(AiInsightService ai) {
        this.ai = ai;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "app", "kathirha",
                "aiProvider", ai.openAiActive() ? "openai" : "deterministic");
    }
}
