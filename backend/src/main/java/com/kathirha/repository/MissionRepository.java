package com.kathirha.repository;

import com.kathirha.domain.Mission;
import com.kathirha.domain.MissionStatus;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByUserOrderByCreatedAtDesc(User user);
    List<Mission> findByUserAndStatus(User user, MissionStatus status);
    long countByUserAndStatus(User user, MissionStatus status);
    void deleteByUser(User user);
}
