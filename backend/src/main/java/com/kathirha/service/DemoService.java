package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.*;
import com.kathirha.security.JwtService;
import com.kathirha.service.bank.MockOpenBankingProvider;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.ApiExceptions;
import com.kathirha.web.dto.Views;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Judge Demo Mode — one call builds a full, realistic scenario for a user: imported transactions,
 * verified income & league, AI missions, an active (behind) goal, leaderboard rank, points to spend,
 * and queued WhatsApp messages. Returns a token so the frontend can log straight in.
 */
@Service
public class DemoService {

    public static final String DEFAULT_DEMO_PHONE = "+966500000001";

    /** Only these seeded demo USER accounts may be driven by the public demo endpoints. */
    private static final java.util.Set<String> ALLOWED_DEMO_PHONES = java.util.Set.of(
            "+966500000001", "+966500000002", "+966500000003", "+966500000004",
            "+966500000005", "+966500000006", "+966500000007", "+966500000008");

    private final UserRepository users;
    private final TransactionService transactionService;
    private final IncomeService incomeService;
    private final MissionService missionService;
    private final GoalService goalService;
    private final DailyQuestionService dailyQuestionService;
    private final DashboardService dashboardService;
    private final PointsService pointsService;
    private final LeaderboardService leaderboardService;
    private final WhatsAppService whatsapp;
    private final JwtService jwt;
    private final AccountService accounts;

    private final MissionRepository missionRepo;
    private final SavingsGoalRepository goalRepo;
    private final RedemptionRepository redemptionRepo;
    private final WhatsAppMessageRepository whatsappRepo;
    private final PointsLedgerRepository pointsRepo;
    private final AiInsightRepository insightRepo;
    private final DailyQuestionRepository questionRepo;
    private final SeasonTierClaimRepository tierClaimRepo;

    public DemoService(UserRepository users, TransactionService transactionService, IncomeService incomeService,
                       MissionService missionService, GoalService goalService, DailyQuestionService dailyQuestionService,
                       DashboardService dashboardService, PointsService pointsService, LeaderboardService leaderboardService,
                       WhatsAppService whatsapp,
                       JwtService jwt, AccountService accounts, MissionRepository missionRepo,
                       SavingsGoalRepository goalRepo, RedemptionRepository redemptionRepo,
                       WhatsAppMessageRepository whatsappRepo, PointsLedgerRepository pointsRepo,
                       AiInsightRepository insightRepo, DailyQuestionRepository questionRepo,
                       SeasonTierClaimRepository tierClaimRepo) {
        this.users = users;
        this.transactionService = transactionService;
        this.incomeService = incomeService;
        this.missionService = missionService;
        this.goalService = goalService;
        this.dailyQuestionService = dailyQuestionService;
        this.dashboardService = dashboardService;
        this.pointsService = pointsService;
        this.leaderboardService = leaderboardService;
        this.whatsapp = whatsapp;
        this.jwt = jwt;
        this.accounts = accounts;
        this.missionRepo = missionRepo;
        this.goalRepo = goalRepo;
        this.redemptionRepo = redemptionRepo;
        this.whatsappRepo = whatsappRepo;
        this.pointsRepo = pointsRepo;
        this.insightRepo = insightRepo;
        this.questionRepo = questionRepo;
        this.tierClaimRepo = tierClaimRepo;
    }

    @Transactional
    public Map<String, Object> seedScenario(String phone) {
        User user = resolve(phone);

        if (transactionService.countFor(user) == 0) {
            transactionService.importTransactions(user, BigDecimal.valueOf(9000), 3,
                    MockOpenBankingProvider.Preset.COFFEE_HEAVY, 0.22);
        }
        incomeService.detectAndVerify(user);
        dashboardService.build(user);
        missionService.ensure(user);

        if (goalService.listFor(user).isEmpty()) {
            SavingsGoal g = goalService.create(user, "آيفون 16 برو",
                    new BigDecimal("5499"), LocalDate.now().plusMonths(6));
            goalService.contribute(user, g.getId(), new BigDecimal("800"));
            // Simulate that time has passed (tighter deadline) so the goal is genuinely behind
            // and the rescue options are meaningful (increase > current pace, extend adds months).
            SavingsGoal seeded = goalService.get(user, g.getId());
            seeded.setTargetDate(LocalDate.now().plusMonths(2));
            goalService.markBehind(seeded);
        }

        if (pointsService.balance(user, PointsType.NORMAL) < 200) {
            pointsService.award(user, 250, PointsType.NORMAL, PointsReason.ADJUSTMENT, "مكافأة ترحيبية", null);
        }

        dailyQuestionService.today(user);

        if (whatsapp.inbox(user).size() < 3) {
            whatsapp.send(user, MessageCategory.WELCOME,
                    "أهلًا " + safeName(user) + "! تم التحقق من حسابك في كثّرها ✅ لنبدأ تنمية مدّخراتك.", null);
            whatsapp.send(user, MessageCategory.DAILY_QUESTION,
                    "📚 سؤال اليوم جاهز — 60 ثانية فقط. جاوب في التطبيق واكسب نقاطك!", null);
            whatsapp.send(user, MessageCategory.MISSION_NUDGE,
                    "💡 مهمة جديدة: وفّر قهوتين هذا الأسبوع وحوّل 50 ريالًا لمدّخراتك. تقدر!", null);
        }

        user = resolve(phone); // reload latest
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", jwt.generateToken(user));
        out.put("user", accounts.meView(user));
        out.put("scenario", summary(user));
        return out;
    }

    @Transactional
    public void reset(String phone) {
        User user = resolve(phone);
        missionRepo.deleteByUser(user);
        goalRepo.deleteByUser(user);
        redemptionRepo.deleteByUser(user);
        whatsappRepo.deleteByUser(user);
        pointsRepo.deleteByUser(user);
        insightRepo.deleteByUser(user);
        questionRepo.deleteByUser(user);
        tierClaimRepo.deleteByUser(user);
        user.setCurrentStreak(0);
        user.setLongestStreak(0);
        user.setStreakFreezes(2);
        user.setPlusActive(false);
        user.setSpendingPersonality(null);
        user.setSavingsHealthScore(null);
        users.save(user);
    }

    private Map<String, Object> summary(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("transactions", transactionService.countFor(user));
        m.put("baselineIncome", user.getBaselineIncome());
        m.put("league", leaderboardService.leagueOf(user).label);
        m.put("activeMissions", missionService.activeFor(user).size());
        List<SavingsGoal> goals = goalService.listFor(user);
        m.put("goal", goals.isEmpty() ? null : Views.GoalView.of(goals.get(0)));
        m.put("normalPoints", pointsService.balance(user, PointsType.NORMAL));
        m.put("whatsAppMessages", whatsapp.inbox(user).size());
        return m;
    }

    private User resolve(String phone) {
        String p = (phone == null || phone.isBlank()) ? DEFAULT_DEMO_PHONE : phone.trim();
        // The demo endpoints are public; never let them mint a token for an arbitrary
        // account (e.g. admin or a real registered user) — only the seeded demo savers.
        if (!ALLOWED_DEMO_PHONES.contains(p)) {
            throw new ApiExceptions.BadRequestException("Demo mode is limited to the seeded demo accounts");
        }
        User user = users.findByPhone(p)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Demo user " + p + " not found"));
        if (user.getRole() != Role.USER) {
            throw new ApiExceptions.BadRequestException("Demo mode is limited to demo user accounts");
        }
        return user;
    }

    private static String safeName(User u) {
        if (u.getDisplayName() == null) return "saver";
        return u.getDisplayName().split(" ")[0];
    }
}
