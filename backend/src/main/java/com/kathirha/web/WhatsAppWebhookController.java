package com.kathirha.web;

import com.kathirha.service.whatsapp.WhatsAppInboundService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Twilio WhatsApp inbound webhook. Twilio POSTs form-encoded fields (From, Body) when a
 * user replies. Point your Twilio sandbox "When a message comes in" at:
 *   https://<your-ngrok>/api/whatsapp/webhook
 *
 * Returns empty TwiML; replies are sent via the Messages API (so they also show in the app simulator).
 * NOTE: for production, validate the X-Twilio-Signature header (not enforced in the sandbox demo).
 */
@RestController
public class WhatsAppWebhookController {

    private final WhatsAppInboundService inbound;

    public WhatsAppWebhookController(WhatsAppInboundService inbound) {
        this.inbound = inbound;
    }

    @PostMapping(value = "/api/whatsapp/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public String webhook(@RequestParam(value = "From", required = false) String from,
                          @RequestParam(value = "Body", required = false) String body) {
        if (from != null && body != null) {
            try { inbound.handle(from, body); } catch (Exception ignored) { /* never fail the webhook */ }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";
    }
}
