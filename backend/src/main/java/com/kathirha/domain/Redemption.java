package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "redemptions")
@Getter
@Setter
public class Redemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id")
    private ShopItem shopItem;

    private String couponCode;
    private int costPoints;

    @Enumerated(EnumType.STRING)
    private PointsType pointsType = PointsType.NORMAL;

    @Enumerated(EnumType.STRING)
    private RedemptionStatus status = RedemptionStatus.OWNED;

    private boolean deliveredViaWhatsApp = false;
    private Instant createdAt = Instant.now();
}
