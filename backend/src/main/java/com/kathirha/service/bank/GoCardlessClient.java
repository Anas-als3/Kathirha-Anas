package com.kathirha.service.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kathirha.config.KathirhaProperties;
import com.kathirha.web.ApiExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal REST client for GoCardless Bank Account Data (formerly Nordigen).
 * Flow: token -> list institutions -> create requisition (auth link) -> read accounts -> read transactions.
 * Use institution id {@code SANDBOXFINANCE_SFIN0000} for the sandbox with ready demo data.
 */
@Component
public class GoCardlessClient {

    private static final Logger log = LoggerFactory.getLogger(GoCardlessClient.class);

    public record Institution(String id, String name, String bic, String logo) {}
    public record Requisition(String id, String link) {}

    private final KathirhaProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private volatile String accessToken;
    private volatile long accessExpiresAt; // epoch seconds

    public GoCardlessClient(KathirhaProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        var g = props.getBank().getGocardless();
        return "gocardless".equalsIgnoreCase(props.getBank().getProvider())
                && notBlank(g.getSecretId()) && notBlank(g.getSecretKey());
    }

    public List<Institution> institutions(String country) {
        JsonNode arr = request("GET", "/institutions/?country=" + country, null, true);
        List<Institution> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                out.add(new Institution(n.path("id").asText(), n.path("name").asText(),
                        n.path("bic").asText(""), n.path("logo").asText("")));
            }
        }
        return out;
    }

    public Requisition createRequisition(String institutionId, String reference) {
        var g = props.getBank().getGocardless();
        Map<String, Object> body = Map.of(
                "institution_id", institutionId,
                "redirect", g.getRedirectUrl(),
                "reference", reference,
                "user_language", "EN");
        JsonNode n = request("POST", "/requisitions/", body, true);
        return new Requisition(n.path("id").asText(), n.path("link").asText());
    }

    public List<String> requisitionAccounts(String requisitionId) {
        JsonNode n = request("GET", "/requisitions/" + requisitionId + "/", null, true);
        List<String> ids = new ArrayList<>();
        JsonNode arr = n.path("accounts");
        if (arr.isArray()) for (JsonNode a : arr) ids.add(a.asText());
        return ids;
    }

    public JsonNode accountTransactions(String accountId) {
        return request("GET", "/accounts/" + accountId + "/transactions/", null, true);
    }

    // ---- internals ----------------------------------------------------------------------

    private synchronized String token() {
        long now = System.currentTimeMillis() / 1000;
        if (accessToken != null && now < accessExpiresAt - 60) return accessToken;
        var g = props.getBank().getGocardless();
        try {
            String body = mapper.writeValueAsString(Map.of("secret_id", g.getSecretId(), "secret_key", g.getSecretKey()));
            JsonNode res = rawPost("/token/new/", body, null);
            accessToken = res.path("access").asText(null);
            accessExpiresAt = now + res.path("access_expires").asLong(3600);
            if (accessToken == null) throw new ApiExceptions.BadRequestException("GoCardless did not return an access token");
            return accessToken;
        } catch (ApiExceptions.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiExceptions.BadRequestException("GoCardless auth failed: " + e.getMessage());
        }
    }

    private JsonNode request(String method, String path, Object jsonBody, boolean auth) {
        try {
            String bearer = auth ? token() : null;
            if ("GET".equals(method)) return rawGet(path, bearer);
            String body = jsonBody == null ? "{}" : mapper.writeValueAsString(jsonBody);
            return rawPost(path, body, bearer);
        } catch (ApiExceptions.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiExceptions.BadRequestException("GoCardless request failed: " + e.getMessage());
        }
    }

    private JsonNode rawGet(String path, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(props.getBank().getGocardless().getBaseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json").GET();
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return send(b.build());
    }

    private JsonNode rawPost(String path, String jsonBody, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(props.getBank().getGocardless().getBaseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return send(b.build());
    }

    private JsonNode send(HttpRequest req) throws Exception {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            log.warn("GoCardless {} -> {}: {}", req.uri(), res.statusCode(), res.body());
            throw new ApiExceptions.BadRequestException("GoCardless returned " + res.statusCode());
        }
        return mapper.readTree(res.body());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
