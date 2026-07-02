package com.kathirha.repository;

import com.kathirha.domain.Transaction;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByDateDesc(User user);
    List<Transaction> findByUserAndDateGreaterThanEqualOrderByDateDesc(User user, LocalDate from);
    long countByUser(User user);
    void deleteByUser(User user);
}
