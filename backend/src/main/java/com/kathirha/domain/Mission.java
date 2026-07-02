package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "missions")
@Getter
@Setter
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private MissionType type = MissionType.DAILY;

    @Enumerated(EnumType.STRING)
    private MissionDifficulty difficulty = MissionDifficulty.EASY;

    private int rewardPoints;

    @Enumerated(EnumType.STRING)
    private PointsType pointsType = PointsType.NORMAL;

    @Enumerated(EnumType.STRING)
    private MissionStatus status = MissionStatus.ACTIVE;

    /** Spending category this mission targets (e.g. COFFEE), if any. */
    @Enumerated(EnumType.STRING)
    private TransactionCategory targetCategory;

    @Column(precision = 14, scale = 2)
    private BigDecimal targetAmount;

    private boolean aiGenerated = true;

    /** Season this mission belongs to, for seasonal challenges (null = normal). */
    private Long seasonId;

    private LocalDate dueDate;
    private Instant createdAt = Instant.now();
    private Instant completedAt;
}
