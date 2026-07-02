package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Tamper-evident points ledger: every change recorded with running balance. */
@Entity
@Table(name = "points_ledger")
@Getter
@Setter
public class PointsLedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private int delta;
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    private PointsType pointsType = PointsType.NORMAL;

    @Enumerated(EnumType.STRING)
    private PointsReason reason;

    private String description;
    private Long seasonId;

    private Instant createdAt = Instant.now();
}
