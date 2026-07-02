package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Persisted AI output (deterministic or OpenAI) so every AI result is stored and auditable. */
@Entity
@Table(name = "ai_insights")
@Getter
@Setter
public class AiInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for global/admin insights. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightKind kind;

    private String title;

    @Column(length = 2000)
    private String body;

    /** Structured payload as JSON, for the frontend to render rich widgets. */
    @Column(length = 4000)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    private AiSource source = AiSource.DETERMINISTIC;

    private Instant createdAt = Instant.now();
}
