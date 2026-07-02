package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal targetAmount;

    /** Target grossed up by an inflation buffer so the money keeps its real value. */
    @Column(precision = 14, scale = 2)
    private BigDecimal inflationAdjustedTarget;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    private GoalStatus status = GoalStatus.ON_TRACK;

    // AI-generated plan
    @Column(precision = 14, scale = 2)
    private BigDecimal monthlySaving;
    @Column(precision = 14, scale = 2)
    private BigDecimal weeklySaving;
    private String riskLevel;
    @Column(length = 600)
    private String strategy;

    private Instant createdAt = Instant.now();
}
