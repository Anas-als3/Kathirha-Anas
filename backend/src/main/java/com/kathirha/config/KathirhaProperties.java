package com.kathirha.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Strongly-typed binding for all {@code kathirha.*} configuration. */
@Component
@ConfigurationProperties(prefix = "kathirha")
@Getter
@Setter
public class KathirhaProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Ai ai = new Ai();
    private Whatsapp whatsapp = new Whatsapp();
    private Economy economy = new Economy();
    private Bank bank = new Bank();
    private Email email = new Email();

    @Getter @Setter
    public static class Bank {
        /** mock | gocardless (Nordigen sandbox) | neotek (future KSA production) */
        private String provider = "mock";
        private GoCardless gocardless = new GoCardless();

        @Getter @Setter
        public static class GoCardless {
            private String secretId = "";
            private String secretKey = "";
            private String baseUrl = "https://bankaccountdata.gocardless.com/api/v2";
            private String redirectUrl = "http://localhost:5173/bank/callback";
            /** Country code for institution listing (sandbox works under GB). */
            private String country = "GB";
        }
    }

    @Getter @Setter
    public static class Email {
        private boolean enabled = false;
        private String from = "Kathirha <no-reply@kathirha.app>";
    }

    @Getter @Setter
    public static class Jwt {
        private String secret = "kathirha-dev-secret-please-override-with-32plus-byte-value";
        private long expirationMs = 86_400_000L;
    }

    @Getter @Setter
    public static class Cors {
        /** Comma-separated list of allowed origins. */
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter @Setter
    public static class Ai {
        /** auto | openai | deterministic */
        private String provider = "auto";
        private Openai openai = new Openai();

        @Getter @Setter
        public static class Openai {
            private String apiKey = "";
            private String model = "gpt-4o-mini";
            private String baseUrl = "https://api.openai.com/v1";
            private int timeoutSeconds = 20;
        }
    }

    @Getter @Setter
    public static class Whatsapp {
        /** mock | twilio */
        private String provider = "mock";
        private Twilio twilio = new Twilio();

        @Getter @Setter
        public static class Twilio {
            private String accountSid = "";
            private String authToken = "";
            private String from = "whatsapp:+14155238886";
        }
    }

    @Getter @Setter
    public static class Economy {
        private int savingsCapPercent = 40;
        private BigDecimal defaultInflationPercent = new BigDecimal("2.3");
        private BigDecimal policyRatePercent = new BigDecimal("5.0");
        private String currency = "SAR";
    }
}
