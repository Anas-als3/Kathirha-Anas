package com.kathirha.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fails fast in the {@code prod} profile if the JWT secret was left at its insecure default or is
 * too short. Harmless in dev (in-memory demo). Prevents shipping a forgeable-token deployment.
 */
@Component
public class ProdSecretsGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final String DEFAULT_SECRET =
            "kathirha-dev-secret-please-override-with-32plus-byte-value";

    private final Environment env;
    private final KathirhaProperties props;

    public ProdSecretsGuard(Environment env, KathirhaProperties props) {
        this.env = env;
        this.props = props;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (!prod) return;
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.isBlank() || secret.equals(DEFAULT_SECRET) || secret.length() < 32) {
            throw new IllegalStateException(
                    "Refusing to start in 'prod' with a weak/default JWT secret. "
                            + "Set JWT_SECRET to a random value of at least 32 characters.");
        }
    }
}
