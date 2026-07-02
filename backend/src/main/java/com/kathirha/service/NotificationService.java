package com.kathirha.service;

import com.kathirha.domain.DailyQuestion;
import com.kathirha.domain.MessageCategory;
import com.kathirha.domain.Role;
import com.kathirha.domain.User;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.whatsapp.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends the daily financial-literacy question (and other nudges) to users over WhatsApp. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserRepository users;
    private final DailyQuestionService dailyQuestionService;
    private final WhatsAppService whatsapp;

    public NotificationService(UserRepository users, DailyQuestionService dailyQuestionService,
                               WhatsAppService whatsapp) {
        this.users = users;
        this.dailyQuestionService = dailyQuestionService;
        this.whatsapp = whatsapp;
    }

    @Transactional
    public int pushDailyQuestionToAll() {
        int sent = 0;
        for (User u : users.findByRole(Role.USER)) {
            if (pushDailyQuestion(u)) sent++;
        }
        log.info("[notify] pushed daily question to {} users", sent);
        return sent;
    }

    @Transactional
    public boolean pushDailyQuestion(User user) {
        DailyQuestion q = dailyQuestionService.today(user);
        if (q.isAnswered()) return false;
        StringBuilder sb = new StringBuilder("📚 *سؤال اليوم من كثّرها*\n\n").append(q.getPrompt()).append("\n\n");
        char letter = 'A';
        for (String opt : q.getOptions()) {
            sb.append(letter++).append(") ").append(opt).append("\n");
        }
        sb.append("\nرد بـ *A* أو *B* أو *C* أو *D* لتكسب ").append(q.getRewardPoints()).append(" نقطة!");
        whatsapp.send(user, MessageCategory.DAILY_QUESTION, sb.toString(), "question:" + q.getId());
        return true;
    }
}
