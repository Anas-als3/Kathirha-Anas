package com.kathirha.service.whatsapp;

import com.kathirha.domain.DailyQuestion;
import com.kathirha.domain.GoalStatus;
import com.kathirha.domain.MessageCategory;
import com.kathirha.domain.SavingsGoal;
import com.kathirha.domain.User;
import com.kathirha.repository.DailyQuestionRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.DailyQuestionService;
import com.kathirha.service.GoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Routes an inbound WhatsApp message (from the Twilio webhook) to the right action:
 * answer the daily question (reply A/B/C/D), choose a goal-rescue option (reply 1/2),
 * else a friendly fallback. Replies are sent back via {@link WhatsAppService}.
 */
@Service
public class WhatsAppInboundService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundService.class);

    private final UserRepository users;
    private final DailyQuestionRepository questions;
    private final DailyQuestionService dailyQuestionService;
    private final GoalService goals;
    private final WhatsAppService whatsapp;

    public WhatsAppInboundService(UserRepository users, DailyQuestionRepository questions,
                                  DailyQuestionService dailyQuestionService, GoalService goals,
                                  WhatsAppService whatsapp) {
        this.users = users;
        this.questions = questions;
        this.dailyQuestionService = dailyQuestionService;
        this.goals = goals;
        this.whatsapp = whatsapp;
    }

    @Transactional
    public String handle(String fromPhone, String rawBody) {
        String phone = fromPhone == null ? "" : fromPhone.replace("whatsapp:", "").trim();
        String body = rawBody == null ? "" : rawBody.trim();
        Optional<User> uOpt = users.findByPhone(phone);
        if (uOpt.isEmpty()) {
            log.info("[whatsapp-in] no account for {}", phone);
            return "لم نجد حساب كثّرها مرتبطًا بالرقم " + phone + ". أنشئ حسابك في التطبيق أولًا 🌱";
        }
        User user = uOpt.get();
        whatsapp.recordInbound(user, MessageCategory.GENERAL, body, null);

        // 1) Daily question answer — reply with a LETTER A/B/C/D
        Optional<DailyQuestion> q = questions.findByUserAndQuestionDate(user, LocalDate.now());
        if (q.isPresent() && !q.get().isAnswered()) {
            Integer idx = parseLetter(body, q.get().getOptions().size());
            if (idx != null) {
                var r = dailyQuestionService.answer(user, idx);
                String reply = (r.correct() ? "✅ إجابة صحيحة! +" + r.pointsAwarded() + " نقطة." : "❌ ليست الإجابة الصحيحة.")
                        + " " + r.explanation();
                whatsapp.send(user, MessageCategory.DAILY_QUESTION, reply, null);
                return reply;
            }
        }

        // 2) Goal rescue — reply 1 (extend) or 2 (increase)
        Optional<SavingsGoal> behind = goals.listFor(user).stream()
                .filter(g -> g.getStatus() == GoalStatus.BEHIND).findFirst();
        if (behind.isPresent() && (body.equals("1") || body.equals("2")
                || body.equalsIgnoreCase("extend") || body.equalsIgnoreCase("increase"))) {
            goals.applyRescue(user, behind.get().getId(), body); // sends its own confirmation
            return "تم — هدفك عاد إلى المسار الصحيح ✅";
        }

        // 3) Fallback
        String reply = "شكرًا لك! أرسل A/B/C/D للإجابة على سؤال اليوم، أو 1/2 للرد على خيار إنقاذ الهدف. "
                + "افتح كثّرها للمزيد 🌱";
        whatsapp.send(user, MessageCategory.GENERAL, reply, null);
        return reply;
    }

    /** Maps 'A'..'D' (case-insensitive) to 0..3 if within the option count; else null. */
    private static Integer parseLetter(String body, int optionCount) {
        if (body == null || body.isEmpty()) return null;
        char c = Character.toUpperCase(body.charAt(0));
        if (c < 'A' || c > 'Z') return null;
        int idx = c - 'A';
        return idx < optionCount ? idx : null;
    }
}
