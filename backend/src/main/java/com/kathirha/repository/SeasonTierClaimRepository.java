package com.kathirha.repository;

import com.kathirha.domain.SeasonTierClaim;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonTierClaimRepository extends JpaRepository<SeasonTierClaim, Long> {
    List<SeasonTierClaim> findByUserAndSeasonId(User user, Long seasonId);
    boolean existsByUserAndSeasonIdAndTier(User user, Long seasonId, int tier);
    void deleteByUser(User user);
}
