package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Mock WhatsApp outbox/inbox so judges can see every message the product would send. */
@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
public class WhatsAppMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private MessageDirection direction = MessageDirection.OUTBOUND;

    @Enumerated(EnumType.STRING)
    private MessageCategory category = MessageCategory.GENERAL;

    @Column(length = 1000, nullable = false)
    private String body;

    /** Free-form context, e.g. the goal id a rescue prompt refers to. */
    private String meta;

    private Instant createdAt = Instant.now();
}
