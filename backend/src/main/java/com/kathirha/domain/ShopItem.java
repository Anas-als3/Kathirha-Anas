package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shop_items")
@Getter
@Setter
public class ShopItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 300)
    private String description;

    /** e.g. FOOD, CINEMA, SHOPPING, SEASONAL. */
    private String category;

    private int costPoints;

    @Enumerated(EnumType.STRING)
    private PointsType pointsType = PointsType.NORMAL;

    /** Non-null = season-exclusive item. */
    private Long seasonId;

    private int stock = 100;
    private String emoji = "🎁";
    private boolean active = true;
}
