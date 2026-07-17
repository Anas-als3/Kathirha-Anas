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

    /**
     * Earn-only total for the achievement leaderboard: positive NORMAL deltas only —
     * spending never reduces it, seasonal points stay inside the season window, and
     * battle-pass gifts (SEASON_GIFT) are wallet-only so pass rewards can never buy rank.
     */
    @Query("select coalesce(sum(p.delta), 0) from PointsLedgerEntry p where p.user = :user and p.delta > 0 and p.pointsType = com.kathirha.domain.PointsType.NORMAL and p.reason <> com.kathirha.domain.PointsReason.SEASON_GIFT")
    int earnedTotal(@Param("user") User user);

    /**
     * Battle-pass XP: positive SEASONAL deltas only. Spending seasonal points in the shop
     * never demotes the pass level or re-locks claimed tiers.
     */
    @Query("select coalesce(sum(p.delta), 0) from PointsLedgerEntry p where p.user = :user and p.delta > 0 and p.pointsType = com.kathirha.domain.PointsType.SEASONAL")
    int seasonalEarnedTotal(@Param("user") User user);

    void deleteByUser(User user);
}
