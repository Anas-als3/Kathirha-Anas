package com.kathirha.service.email;

import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email notifications via SMTP (Mailtrap by default). Completely inert unless
 * {@code kathirha.email.enabled=true} AND a mail host is configured — so the app runs fine
 * with no email setup. Never throws to callers.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final KathirhaProperties props;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, KathirhaProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendWelcome(User user) {
        String name = user.getDisplayName() == null ? "saver" : user.getDisplayName().split(" ")[0];
        send(user.getEmail(), "Welcome to Kathirha 🌱",
                "Ahlan " + name + ",\n\n"
                        + "Welcome to Kathirha — saving, turned into a game worth winning.\n"
                        + "Link your bank to get your AI savings coach, earn points, and climb the fairness leaderboard.\n\n"
                        + "— The Kathirha team");
    }

    public void sendEncouragement(User user, String message) {
        send(user.getEmail(), "Keep growing your savings 🌱", message);
    }

    public void send(String to, String subject, String text) {
        if (!props.getEmail().isEnabled() || to == null || to.isBlank()) return;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("email enabled but no SMTP host configured — skipping send to {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getEmail().getFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            sender.send(msg);
            log.info("[email] sent '{}' to {}", subject, to);
        } catch (Exception e) {
            log.warn("[email] send to {} failed: {}", to, e.getMessage());
        }
    }
}
