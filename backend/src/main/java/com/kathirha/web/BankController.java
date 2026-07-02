package com.kathirha.web;

import com.kathirha.config.KathirhaProperties;
import com.kathirha.domain.User;
import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.IncomeService;
import com.kathirha.service.LeaderboardService;
import com.kathirha.service.TransactionService;
import com.kathirha.service.bank.GoCardlessClient;
import com.kathirha.service.bank.MockOpenBankingProvider;
import com.kathirha.service.bank.RealBankService;
import com.kathirha.web.dto.Requests;
import com.kathirha.web.dto.Views;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final MockOpenBankingProvider bank;
    private final TransactionService transactionService;
    private final IncomeService incomeService;
    private final AccountService accounts;
    private final RealBankService realBank;
    private final KathirhaProperties props;
    private final LeaderboardService leaderboard;

    public BankController(MockOpenBankingProvider bank, TransactionService transactionService,
                          IncomeService incomeService, AccountService accounts,
                          RealBankService realBank, KathirhaProperties props,
                          LeaderboardService leaderboard) {
        this.bank = bank;
        this.transactionService = transactionService;
        this.incomeService = incomeService;
        this.accounts = accounts;
        this.realBank = realBank;
        this.props = props;
        this.leaderboard = leaderboard;
    }

    @PostMapping("/connect")
    public MockOpenBankingProvider.ConsentResult connect(@AuthenticationPrincipal AppUserDetails principal) {
        return bank.connect(accounts.require(principal));
    }

    @PostMapping("/import")
    public Map<String, Object> importTransactions(@AuthenticationPrincipal AppUserDetails principal,
                                                  @RequestBody(required = false) Requests.ImportRequest req) {
        User user = accounts.require(principal);
        BigDecimal income = req != null && req.monthlyIncome() != null ? req.monthlyIncome() : BigDecimal.valueOf(8000);
        int months = req != null && req.months() != null ? req.months() : 3;
        MockOpenBankingProvider.Preset preset = parsePreset(req == null ? null : req.preset());

        int imported = transactionService.importTransactions(user, income, months, preset);
        BigDecimal baseline = incomeService.detectAndVerify(user);
        User refreshed = accounts.require(principal);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("imported", imported);
        out.put("baselineIncome", baseline);
        out.put("league", leaderboard.leagueOf(refreshed).label);
        out.put("bankVerified", refreshed.isBankVerified());
        return out;
    }

    @GetMapping("/transactions")
    public List<Views.TransactionView> transactions(@AuthenticationPrincipal AppUserDetails principal) {
        return transactionService.forUser(accounts.require(principal)).stream()
                .map(Views.TransactionView::of).toList();
    }

    @GetMapping("/spending")
    public Object spending(@AuthenticationPrincipal AppUserDetails principal) {
        return transactionService.breakdown(accounts.require(principal));
    }

    // ---- Real open banking (GoCardless) — active when BANK_PROVIDER=gocardless ----------

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("provider", props.getBank().getProvider(), "realEnabled", realBank.enabled());
    }

    /** List banks available via the configured aggregator (sandbox uses GB). */
    @GetMapping("/institutions")
    public List<GoCardlessClient.Institution> institutions() {
        return realBank.institutions();
    }

    /** Create the bank authorization link; the user opens it to consent. */
    @PostMapping("/link")
    public GoCardlessClient.Requisition link(@AuthenticationPrincipal AppUserDetails principal,
                                             @Valid @RequestBody Requests.BankLinkRequest req) {
        return realBank.createLink(accounts.require(principal), req.institutionId());
    }

    /** After consent, import + categorize real transactions and detect income. */
    @PostMapping("/complete")
    public Map<String, Object> complete(@AuthenticationPrincipal AppUserDetails principal,
                                        @Valid @RequestBody Requests.BankCompleteRequest req) {
        User user = accounts.require(principal);
        int imported = realBank.importFromRequisition(user, req.requisitionId());
        User refreshed = accounts.require(principal);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("imported", imported);
        out.put("baselineIncome", refreshed.getBaselineIncome());
        out.put("league", leaderboard.leagueOf(refreshed).label);
        out.put("bankVerified", refreshed.isBankVerified());
        return out;
    }

    private MockOpenBankingProvider.Preset parsePreset(String preset) {
        if (preset == null) return MockOpenBankingProvider.Preset.BALANCED;
        try {
            return MockOpenBankingProvider.Preset.valueOf(preset.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MockOpenBankingProvider.Preset.BALANCED;
        }
    }
}
