package com.kathirha.repository;

import com.kathirha.domain.AiInsight;
import com.kathirha.domain.InsightKind;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {
    List<AiInsight> findTop20ByUserOrderByCreatedAtDesc(User user);
    Optional<AiInsight> findFirstByUserAndKindOrderByCreatedAtDesc(User user, InsightKind kind);
    void deleteByUser(User user);
}
