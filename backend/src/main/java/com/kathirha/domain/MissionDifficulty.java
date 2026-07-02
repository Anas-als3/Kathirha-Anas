package com.kathirha.domain;

/** Harder missions reward more points (see baseReward). */
public enum MissionDifficulty {
    EASY(10), MEDIUM(25), HARD(50);

    public final int baseReward;
    MissionDifficulty(int baseReward) { this.baseReward = baseReward; }
}
