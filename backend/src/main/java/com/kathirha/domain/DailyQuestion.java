package com.kathirha.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** A personalized daily financial-literacy question (one per user per day). */
@Entity
@Table(name = "daily_questions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_date"}))
@Getter
@Setter
public class DailyQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    @Column(length = 400, nullable = false)
    private String prompt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", length = 200)
    private java.util.List<String> options = new java.util.ArrayList<>();

    private int correctIndex;

    @Column(length = 600)
    private String explanation;

    private int rewardPoints = 15;

    private boolean answered = false;
    private Integer answeredIndex;
    private boolean answeredCorrect = false;
}
