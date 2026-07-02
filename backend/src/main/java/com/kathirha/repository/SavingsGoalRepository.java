package com.kathirha.repository;

import com.kathirha.domain.GoalStatus;
import com.kathirha.domain.SavingsGoal;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserOrderByCreatedAtDesc(User user);
    List<SavingsGoal> findByUserAndStatus(User user, GoalStatus status);
    void deleteByUser(User user);
}
