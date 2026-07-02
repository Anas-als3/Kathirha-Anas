package com.kathirha.repository;

import com.kathirha.domain.User;
import com.kathirha.domain.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findByUserOrderByCreatedAtAsc(User user);
    void deleteByUser(User user);
}
