package com.kathirha.repository;

import com.kathirha.domain.PointsLedgerEntry;
import com.kathirha.domain.PointsType;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedgerEntry, Long> {
    List<PointsLedgerEntry> findTop50ByUserOrderByCreatedAtDesc(User user);

    @Query("select coalesce(sum(p.delta), 0) from PointsLedgerEntry p where p.user = :user and p.pointsType = :type")
    int balance(@Param("user") User user, @Param("type") PointsType type);

    /** Earn-only total: positive deltas only, so spending never reduces the leaderboard score. */
    @Query("select coalesce(sum(p.delta), 0) from PointsLedgerEntry p where p.user = :user and p.delta > 0")
    int earnedTotal(@Param("user") User user);

    void deleteByUser(User user);
}
