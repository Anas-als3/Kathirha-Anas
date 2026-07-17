package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phone;

    private String email;
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    private IntegrityStatus integrityStatus = IntegrityStatus.NEW;

    private boolean phoneVerified = false;
    private boolean bankVerified = false;
    private boolean competitiveOptIn = true;

    /** كثّرها+ premium rewards track — unlocked via salary transfer, savings balance, or subscription. */
    private boolean plusActive = false;

    /** Verified monthly income detected from salary credits. */
    private BigDecimal baselineIncome;

    @Enumerated(EnumType.STRING)
    private IncomeLeague incomeLeague;

    @Enumerated(EnumType.STRING)
    private SpendingPersonality spendingPersonality;

    private Integer savingsHealthScore;

    private int currentStreak = 0;
    private int longestStreak = 0;
    private int streakFreezes = 2;

    /** Mock OTP used by the WhatsApp sign-up flow. */
    private String otpCode;

    private Instant createdAt = Instant.now();
}
