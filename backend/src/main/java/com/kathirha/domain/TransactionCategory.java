package com.kathirha.domain;

/**
 * Transaction categories. {@code spending} marks an expense (debit) that reduces savings rate.
 * SALARY is income; SAVINGS_TRANSFER is money moved into savings (counts as saved, not spent).
 */
public enum TransactionCategory {
    SALARY(false, false, "الراتب", "💰"),
    SAVINGS_TRANSFER(false, true, "الادّخار", "🏦"),
    GROCERIES(true, false, "البقالة", "🛒"),
    FOOD_DELIVERY(true, false, "توصيل الطعام", "🍔"),
    COFFEE(true, false, "القهوة", "☕"),
    TRANSPORT(true, false, "المواصلات", "🚗"),
    ENTERTAINMENT(true, false, "الترفيه", "🎬"),
    SHOPPING(true, false, "التسوق", "🛍️"),
    BILLS(true, false, "الفواتير", "🧾"),
    OTHER(true, false, "أخرى", "📌");

    public final boolean spending;
    public final boolean savings;
    public final String label;
    public final String emoji;

    TransactionCategory(boolean spending, boolean savings, String label, String emoji) {
        this.spending = spending;
        this.savings = savings;
        this.label = label;
        this.emoji = emoji;
    }
}
