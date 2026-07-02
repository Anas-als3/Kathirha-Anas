package com.kathirha.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kathirha.config.KathirhaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin, dependency-free OpenAI chat client used ONLY to rephrase narrative text more warmly.
 * It never produces numbers or decisions — those come from {@link DeterministicAiCoach}.
 * Returns {@link Optional#empty()} whenever no key is configured or anything goes wrong,
 * so the deterministic text is always used as the fallback.
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final KathirhaProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public OpenAiClient(KathirhaProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isConfigured() {
        String provider = props.getAi().getProvider();
        String key = props.getAi().getOpenai().getApiKey();
        boolean hasKey = key != null && !key.isBlank();
        return hasKey && !"deterministic".equalsIgnoreCase(provider);
    }

    /**
     * Rephrase {@code text} according to {@code instruction}, preserving all facts/numbers.
     * @return enhanced text, or empty to signal "use the deterministic version".
     */
    public Optional<String> rephrase(String instruction, String text) {
        if (!isConfigured() || text == null || text.isBlank()) return Optional.empty();
        try {
            var openai = props.getAi().getOpenai();
            String system = "You are Kathirha's savings coach. Rephrase the user's text into one warm, "
                    + "concise, encouraging line. IMPORTANT: all user-facing text in your response MUST be "
                    + "written in Arabic (Saudi-friendly Modern Standard Arabic). Keep digits in Latin form "
                    + "(e.g. 1,234), use \"ريال\" for currency, and keep brand or technical "
                    + "terms (e.g. Netflix, Open Banking, API) in English. If your response contains JSON, all "
                    + "JSON keys and enum values MUST stay in English — only human-readable text values are "
                    + "in Arabic. Do NOT change, add, "
                    + "or remove any numbers, amounts, dates or facts. Output only the rephrased text. "
                    + instruction;
            Map<String, Object> body = Map.of(
                    "model", openai.getModel(),
                    "temperature", 0.5,
                    "max_tokens", 160,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", text)));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(openai.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(openai.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openai.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("OpenAI returned {} — falling back to deterministic text", res.statusCode());
                return Optional.empty();
            }
            JsonNode root = mapper.readTree(res.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return content.isBlank() ? Optional.empty() : Optional.of(content.trim());
        } catch (Exception e) {
            log.warn("OpenAI call failed ({}) — using deterministic text", e.getMessage());
            return Optional.empty();
        }
    }
}
