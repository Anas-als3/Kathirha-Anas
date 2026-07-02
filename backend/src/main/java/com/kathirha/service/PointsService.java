package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.PointsLedgerRepository;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dual-wallet points economy (NORMAL yearly + SEASONAL) backed by a tamper-evident ledger.
 * Note: leaderboard score is separately capped at 40% of income — extra points only grow the wallet.
 */
@Service
public class PointsService {

    private final PointsLedgerRepository ledger;

    public PointsService(PointsLedgerRepository ledger) {
        this.ledger = ledger;
    }

    @Transactional
    public int award(User user, int amount, PointsType type, PointsReason reason, String description, Long seasonId) {
        int balanceAfter = ledger.balance(user, type) + amount;
        PointsLedgerEntry e = new PointsLedgerEntry();
        e.setUser(user);
        e.setDelta(amount);
        e.setBalanceAfter(balanceAfter);
        e.setPointsType(type);
        e.setReason(reason);
        e.setDescription(description);
        e.setSeasonId(seasonId);
        ledger.save(e);
        return balanceAfter;
    }

    @Transactional
    public int spend(User user, int amount, PointsType type, PointsReason reason, String description, Long seasonId) {
        int current = ledger.balance(user, type);
        if (current < amount) {
            throw new ApiExceptions.BadRequestException(
                    "Not enough " + type.name().toLowerCase() + " points (have " + current + ", need " + amount + ")");
        }
        return award(user, -amount, type, reason, description, seasonId);
    }

    public int balance(User user, PointsType type) {
        return ledger.balance(user, type);
    }

    public List<PointsLedgerEntry> recent(User user) {
        return ledger.findTop50ByUserOrderByCreatedAtDesc(user);
    }
}
