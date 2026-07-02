package com.kathirha.service.bank;

import com.kathirha.domain.TransactionCategory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Heuristic categorizer for REAL bank transactions (description/merchant + sign).
 * Credits are income (SALARY if it looks like pay, else OTHER and ignored from spending);
 * debits are matched to a spending category by keyword.
 */
@Component
public class TransactionCategorizer {

    public TransactionCategory categorize(String text, BigDecimal amount) {
        String t = text == null ? "" : text.toLowerCase();

        if (amount != null && amount.signum() > 0) {
            if (has(t, "salary", "payroll", "wage", "salaire", "راتب")) return TransactionCategory.SALARY;
            return TransactionCategory.OTHER; // non-salary credit — excluded from spending by sign
        }
        if (has(t, "saving", "savings", "invest", "deposit to savings", "transfer to savings")) return TransactionCategory.SAVINGS_TRANSFER;
        if (has(t, "market", "supermarket", "grocery", "grocer", "tamimi", "danube", "panda", "carrefour", "lulu", "bqala")) return TransactionCategory.GROCERIES;
        if (has(t, "jahez", "hungerstation", "talabat", "deliveroo", "uber eats", "ubereats", "mrsool", "toyou", "kfc", "mcdonald", "burger", "pizza", "restaurant", "food")) return TransactionCategory.FOOD_DELIVERY;
        if (has(t, "starbucks", "costa", "coffee", "barn", "dunkin", "cafe", "café", "caffe", "half million", "tim hortons")) return TransactionCategory.COFFEE;
        if (has(t, "uber", "careem", "fuel", "petrol", "gas station", "adnoc", "petromin", "taxi", "metro", "transport", "parking")) return TransactionCategory.TRANSPORT;
        if (has(t, "cinema", "vox", "amc", "netflix", "spotify", "steam", "playstation", "xbox", "game", "entertain", "ticket", "boulevard")) return TransactionCategory.ENTERTAINMENT;
        if (has(t, "amazon", "noon", "jarir", "ikea", "zara", "h&m", "mall", "store", "shop", "centrepoint", "extra", "retail")) return TransactionCategory.SHOPPING;
        if (has(t, "stc", "mobily", "zain", "electric", "water", "internet", "bill", "utility", "insurance", "rent", "subscription")) return TransactionCategory.BILLS;
        return TransactionCategory.OTHER;
    }

    private static boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }
}
