package com.kathirha.domain;

public enum SpendingPersonality {
    CAUTIOUS_SAVER("مدّخر حذر"),
    IMPULSE_SPENDER("منفق اندفاعي"),
    FOOD_DELIVERY_HEAVY("عاشق توصيل الطعام"),
    COFFEE_LEAKER("مسرّب القهوة"),
    GOAL_ORIENTED_SAVER("مدّخر هادف"),
    BALANCED("متوازن");

    public final String label;
    SpendingPersonality(String label) { this.label = label; }
}
