package com.kathirha.service.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.Transaction;
import com.kathirha.domain.TransactionCategory;
import com.kathirha.domain.User;
import com.kathirha.repository.TransactionRepository;
import com.kathirha.service.IncomeService;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Real open-banking flow via GoCardless: list banks, create the consent link, then import and
 * categorize the user's actual transactions and detect their verified income.
 */
@Service
public class RealBankService {

    private final GoCardlessClient client;
    private final TransactionCategorizer categorizer;
    private final TransactionRepository transactions;
    private final IncomeService incomeService;
    private final KathirhaProperties props;

    public RealBankService(GoCardlessClient client, TransactionCategorizer categorizer,
                           TransactionRepository transactions, IncomeService incomeService,
                           KathirhaProperties props) {
        this.client = client;
        this.categorizer = categorizer;
        this.transactions = transactions;
        this.incomeService = incomeService;
        this.props = props;
    }

    public boolean enabled() {
        return client.isConfigured();
    }

    private void requireEnabled() {
        if (!enabled()) {
            throw new ApiExceptions.BadRequestException(
                    "Real open banking is not configured. Set BANK_PROVIDER=gocardless and GoCardless secrets, "
                            + "or use the mock import / Instant Demo.");
        }
    }

    public List<GoCardlessClient.Institution> institutions() {
        requireEnabled();
        return client.institutions(props.getBank().getGocardless().getCountry());
    }

    public GoCardlessClient.Requisition createLink(User user, String institutionId) {
        requireEnabled();
        String ref = "kathirha-" + user.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return client.createRequisition(institutionId, ref);
    }

    @Transactional
    public int importFromRequisition(User user, String requisitionId) {
        requireEnabled();
        List<String> accounts = client.requisitionAccounts(requisitionId);
        if (accounts.isEmpty()) {
            throw new ApiExceptions.BadRequestException(
                    "No authorized accounts yet — finish the bank authorization in the opened link first.");
        }
        List<Transaction> parsed = new ArrayList<>();
        for (String accountId : accounts) {
            JsonNode booked = client.accountTransactions(accountId).path("transactions").path("booked");
            if (booked.isArray()) {
                for (JsonNode n : booked) {
                    Transaction t = parse(user, n);
                    if (t != null) parsed.add(t);
                }
            }
        }
        if (parsed.isEmpty()) {
            throw new ApiExceptions.BadRequestException("No transactions found on the linked account.");
        }
        // If nothing looks like salary, promote the largest credit so income detection works.
        boolean hasSalary = parsed.stream().anyMatch(t -> t.getCategory() == TransactionCategory.SALARY);
        if (!hasSalary) {
            parsed.stream().filter(t -> t.getAmount().signum() > 0)
                    .max(Comparator.comparing(Transaction::getAmount))
                    .ifPresent(t -> t.setCategory(TransactionCategory.SALARY));
        }
        // Replace any prior (mock or earlier) transactions for a clean real import.
        transactions.deleteByUser(user);
        transactions.saveAll(parsed);
        incomeService.detectAndVerify(user);
        return parsed.size();
    }

    private Transaction parse(User user, JsonNode n) {
        String amtStr = n.path("transactionAmount").path("amount").asText(null);
        if (amtStr == null || amtStr.isBlank()) return null;
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            return null;
        }
        String dateStr = firstNonBlank(n.path("bookingDate").asText(""), n.path("valueDate").asText(""));
        if (dateStr.isBlank()) return null;
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
        String desc = firstNonBlank(
                n.path("remittanceInformationUnstructured").asText(""),
                n.path("creditorName").asText(""),
                n.path("debtorName").asText(""),
                "Transaction");
        String merchant = firstNonBlank(
                n.path("creditorName").asText(""),
                n.path("debtorName").asText(""),
                desc);

        Transaction t = new Transaction();
        t.setUser(user);
        t.setDate(date);
        t.setDescription(desc);
        t.setMerchant(merchant);
        t.setAmount(amount);
        t.setCategory(categorizer.categorize(desc + " " + merchant, amount));
        return t;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return "";
    }
}
