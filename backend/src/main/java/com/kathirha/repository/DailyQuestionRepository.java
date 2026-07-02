package com.kathirha.repository;

import com.kathirha.domain.DailyQuestion;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {
    Optional<DailyQuestion> findByUserAndQuestionDate(User user, LocalDate questionDate);
    void deleteByUser(User user);
}
