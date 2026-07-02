package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** A mock cashback card the AI can recommend based on the user's real spending mix. */
@Entity
@Table(name = "cashback_cards")
@Getter
@Setter
public class CashbackCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Spending category this card rewards. */
    @Enumerated(EnumType.STRING)
    private TransactionCategory rewardCategory;

    @Column(precision = 5, scale = 2)
    private BigDecimal cashbackPercent;

    @Column(precision = 10, scale = 2)
    private BigDecimal annualFee = BigDecimal.ZERO;

    @Column(length = 300)
    private String description;

    private String emoji = "💳";
}
