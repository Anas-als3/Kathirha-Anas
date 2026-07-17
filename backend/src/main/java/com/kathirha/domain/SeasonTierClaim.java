package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One claimed battle-pass tier per user per season. */
@Entity
@Table(name = "season_tier_claims",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "seasonId", "tier"}))
@Getter
@Setter
public class SeasonTierClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private Long seasonId;
    private int tier;

    private Instant createdAt = Instant.now();
}
