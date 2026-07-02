package com.kathirha.service.whatsapp;

import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.MessageCategory;
import com.kathirha.domain.MessageDirection;
import com.kathirha.domain.User;
import com.kathirha.domain.WhatsAppMessage;
import com.kathirha.repository.WhatsAppMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * WhatsApp gateway. Always records messages to the DB so the judge-facing simulator can show the
 * exact conversation. When {@code kathirha.whatsapp.provider=twilio} and credentials are present,
 * a real Twilio send would be wired here — but the mock path never requires any credentials.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WhatsAppMessageRepository repo;
    private final KathirhaProperties props;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsAppService(WhatsAppMessageRepository repo, KathirhaProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public WhatsAppMessage send(User user, MessageCategory category, String body, String meta) {
        WhatsAppMessage m = new WhatsAppMessage();
        m.setUser(user);
        m.setDirection(MessageDirection.OUTBOUND);
        m.setCategory(category);
        m.setBody(body);
        m.setMeta(meta);
        repo.save(m);

        if (twilioConfigured()) {
            sendViaTwilio("whatsapp:" + user.getPhone(), body);
        } else {
            log.info("[whatsapp-mock] -> {} ({}): {}", user.getPhone(), category, body);
        }
        return m;
    }

    /** Real Twilio WhatsApp send via the REST API. Never throws — failures are logged. */
    private void sendViaTwilio(String to, String body) {
        var t = props.getWhatsapp().getTwilio();
        try {
            String form = "From=" + enc(t.getFrom()) + "&To=" + enc(to) + "&Body=" + enc(body);
            String auth = Base64.getEncoder().encodeToString(
                    (t.getAccountSid() + ":" + t.getAuthToken()).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + t.getAccountSid() + "/Messages.json"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2) {
                log.info("[twilio] sent to {}", to);
            } else {
                log.warn("[twilio] send to {} failed ({}): {}", to, res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.warn("[twilio] send to {} error: {}", to, e.getMessage());
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public WhatsAppMessage recordInbound(User user, MessageCategory category, String body, String meta) {
        WhatsAppMessage m = new WhatsAppMessage();
        m.setUser(user);
        m.setDirection(MessageDirection.INBOUND);
        m.setCategory(category);
        m.setBody(body);
        m.setMeta(meta);
        return repo.save(m);
    }

    public List<WhatsAppMessage> inbox(User user) {
        return repo.findByUserOrderByCreatedAtAsc(user);
    }

    private boolean twilioConfigured() {
        var t = props.getWhatsapp().getTwilio();
        return "twilio".equalsIgnoreCase(props.getWhatsapp().getProvider())
                && t.getAccountSid() != null && !t.getAccountSid().isBlank()
                && t.getAuthToken() != null && !t.getAuthToken().isBlank();
    }
}
