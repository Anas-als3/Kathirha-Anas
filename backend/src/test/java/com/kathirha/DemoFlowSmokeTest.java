package com.kathirha;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end smoke test of the judge demo flow over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoFlowSmokeTest {

    @Autowired
    TestRestTemplate rest;

    private HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullJudgeDemoFlowWorks() {
        // 1. Health
        ResponseEntity<Map> health = rest.getForEntity("/api/health", Map.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertEquals("UP", health.getBody().get("status"));

        // 2. Judge Demo Mode (public)
        ResponseEntity<Map> seed = rest.postForEntity("/api/demo/seed", null, Map.class);
        assertEquals(HttpStatus.OK, seed.getStatusCode());
        String token = (String) seed.getBody().get("token");
        assertNotNull(token, "demo seed returns a JWT");

        // 3. Dashboard aggregates AI features
        ResponseEntity<Map> dash = rest.exchange("/api/dashboard", HttpMethod.GET, auth(token), Map.class);
        assertEquals(HttpStatus.OK, dash.getStatusCode());
        assertNotNull(dash.getBody().get("user"));
        assertNotNull(dash.getBody().get("healthScore"));
        assertNotNull(dash.getBody().get("personality"));

        // 4. Fairness leaderboard caps at 40%
        ResponseEntity<Map> lb = rest.exchange("/api/leaderboard", HttpMethod.GET, auth(token), Map.class);
        assertEquals(HttpStatus.OK, lb.getStatusCode());
        assertEquals(40, lb.getBody().get("capPercent"));
        List<Map<String, Object>> entries = (List<Map<String, Object>>) lb.getBody().get("entries");
        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(e -> Boolean.TRUE.equals(e.get("capped"))),
                "at least one competitor exceeds the 40% cap");

        // 5. Complete a mission -> points awarded
        ResponseEntity<List> missions = rest.exchange("/api/missions", HttpMethod.GET, auth(token), List.class);
        List<Map<String, Object>> ms = missions.getBody();
        assertNotNull(ms);
        Map<String, Object> active = ms.stream()
                .filter(m -> "ACTIVE".equals(m.get("status"))).findFirst().orElseThrow();
        Long missionId = ((Number) active.get("id")).longValue();
        ResponseEntity<Map> complete = rest.exchange("/api/missions/" + missionId + "/complete",
                HttpMethod.POST, auth(token), Map.class);
        assertEquals(HttpStatus.OK, complete.getStatusCode());
        assertTrue(((Number) complete.getBody().get("pointsAwarded")).intValue() > 0);

        // 6. Leaderboard explanation (AI)
        ResponseEntity<Map> explain = rest.exchange("/api/leaderboard/explain", HttpMethod.GET, auth(token), Map.class);
        assertEquals(HttpStatus.OK, explain.getStatusCode());
        assertNotNull(explain.getBody().get("capExplanation"));

        // 7. Redeem a reward, then list redemptions (regression: lazy shopItem serialization)
        ResponseEntity<List> shop = rest.exchange("/api/shop", HttpMethod.GET, auth(token), List.class);
        List<Map<String, Object>> items = shop.getBody();
        Map<String, Object> affordable = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.get("affordable"))).findFirst().orElseThrow();
        Long itemId = ((Number) affordable.get("id")).longValue();
        ResponseEntity<Map> redeem = rest.exchange("/api/shop/" + itemId + "/redeem", HttpMethod.POST, auth(token), Map.class);
        assertEquals(HttpStatus.OK, redeem.getStatusCode());
        assertNotNull(redeem.getBody().get("couponCode"));
        ResponseEntity<List> reds = rest.exchange("/api/shop/redemptions", HttpMethod.GET, auth(token), List.class);
        assertEquals(HttpStatus.OK, reds.getStatusCode());
        assertFalse(reds.getBody().isEmpty(), "redemptions list must serialize (no lazy-init error)");

        // 8. Admin insights AFTER a redemption exists (regression: lazy shopItem in findAll)
        ResponseEntity<Map> adminLogin = rest.postForEntity("/api/auth/login",
                Map.of("phone", "admin", "password", "admin1234"), Map.class);
        String adminToken = (String) adminLogin.getBody().get("token");
        ResponseEntity<Map> insights = rest.exchange("/api/admin/insights", HttpMethod.GET, auth(adminToken), Map.class);
        assertEquals(HttpStatus.OK, insights.getStatusCode());

        // 9. Security: the public demo endpoint must NOT mint a token for the admin account
        ResponseEntity<Map> badSeed = rest.postForEntity("/api/demo/seed?phone=admin", null, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, badSeed.getStatusCode(), "demo seed must reject non-demo accounts");
    }
}
