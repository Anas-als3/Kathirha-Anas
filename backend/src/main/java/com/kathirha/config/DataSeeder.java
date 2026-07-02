package com.kathirha.config;

import com.kathirha.domain.*;
import com.kathirha.repository.CashbackCardRepository;
import com.kathirha.repository.SeasonRepository;
import com.kathirha.repository.ShopItemRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.IncomeService;
import com.kathirha.service.TransactionService;
import com.kathirha.service.bank.MockOpenBankingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Seeds a realistic, judge-ready dataset on startup: cards, rewards, a live season, demo users. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CashbackCardRepository cards;
    private final ShopItemRepository shop;
    private final SeasonRepository seasons;
    private final TransactionService transactionService;
    private final IncomeService incomeService;

    public DataSeeder(UserRepository users, PasswordEncoder encoder, CashbackCardRepository cards,
                      ShopItemRepository shop, SeasonRepository seasons, TransactionService transactionService,
                      IncomeService incomeService) {
        this.users = users;
        this.encoder = encoder;
        this.cards = cards;
        this.shop = shop;
        this.seasons = seasons;
        this.transactionService = transactionService;
        this.incomeService = incomeService;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            log.info("Data already present — skipping seed.");
            return;
        }
        log.info("Seeding Kathirha demo data...");
        seedCards();
        Season season = seedSeason();
        seedShop(season);
        seedUsers();
        log.info("Seed complete: {} users.", users.count());
    }

    private void seedCards() {
        card("بطاقة موفّر البقالة", TransactionCategory.GROCERIES, "5.00", "0", "استرداد نقدي 5% على كل مشتريات البقالة", "🛒");
        card("بطاقة الذوّاقة", TransactionCategory.FOOD_DELIVERY, "8.00", "120", "استرداد نقدي 8% على توصيل الطعام", "🍔");
        card("بطاقة عشّاق القهوة", TransactionCategory.COFFEE, "10.00", "60", "استرداد نقدي 10% في المقاهي", "☕");
        card("بطاقة المشاوير", TransactionCategory.TRANSPORT, "4.00", "0", "استرداد نقدي 4% على المواصلات والوقود", "🚗");
        card("بطاقة المتسوّق", TransactionCategory.SHOPPING, "3.00", "0", "استرداد نقدي 3% على التسوق", "🛍️");
        card("البطاقة اليومية", TransactionCategory.BILLS, "2.00", "0", "استرداد نقدي 2% على الفواتير", "🧾");
    }

    private Season seedSeason() {
        Season s = new Season();
        s.setName("صيف الادّخار");
        s.setTheme("Summer");
        s.setDescription("اهزم الحر وكثّر مدخراتك — مكافآت حصرية طوال الموسم.");
        s.setStartDate(LocalDate.now().minusDays(10));
        s.setEndDate(LocalDate.now().plusDays(50));
        s.setActive(true);
        return seasons.save(s);
    }

    private void seedShop(Season season) {
        item("قسيمة HungerStation بقيمة 25 ريال", "خصم على توصيل الطعام", "FOOD", 150, PointsType.NORMAL, null, "🍔");
        item("قسيمة قهوة Barn's", "قهوتك علينا", "FOOD", 100, PointsType.NORMAL, null, "☕");
        item("تذكرة سينما VOX", "تذكرة سينما واحدة", "CINEMA", 250, PointsType.NORMAL, null, "🎬");
        item("قسيمة Noon بقيمة 30 ريال", "قسيمة تسوق", "SHOPPING", 200, PointsType.NORMAL, null, "🛒");
        item("بطاقة هدايا Jarir بقيمة 50 ريال", "إلكترونيات وكتب", "SHOPPING", 400, PointsType.NORMAL, null, "🛍️");
        item("صندوق مفاجآت صيف الادّخار", "مفاجأة حصرية للموسم", "SEASONAL", 300, PointsType.SEASONAL, season.getId(), "🏖️");
    }

    private void seedUsers() {
        // Admin
        User admin = new User();
        admin.setPhone("admin");
        admin.setDisplayName("إدارة كثّرها");
        admin.setEmail("admin@kathirha.app");
        admin.setPasswordHash(encoder.encode("admin1234"));
        admin.setRole(Role.ADMIN);
        admin.setIntegrityStatus(IntegrityStatus.VERIFIED);
        admin.setCompetitiveOptIn(false);
        users.save(admin);

        // Primary demo user (SILVER league, coffee-leaker, ~22% savings) -> ranks mid-table
        saver("+966500000001", "سارة العتيبي", "sara@kathirha.app", 9000,
                MockOpenBankingProvider.Preset.COFFEE_HEAVY, 0.22);

        // SILVER league competitors
        saver("+966500000003", "لينا القحطاني", "lina@kathirha.app", 11000,
                MockOpenBankingProvider.Preset.BALANCED, 0.45); // hits the 40% cap
        saver("+966500000004", "نورة الدوسري", "noura@kathirha.app", 9500,
                MockOpenBankingProvider.Preset.BALANCED, 0.30);
        saver("+966500000005", "فيصل الغامدي", "faisal@kathirha.app", 13000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.18);
        saver("+966500000006", "منصور الشهري", "mansour@kathirha.app", 8500,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.13);

        // Second demo user (GOLD league)
        saver("+966500000002", "خالد الحربي", "khalid@kathirha.app", 16000,
                MockOpenBankingProvider.Preset.BALANCED, 0.27);
        // GOLD competitors
        saver("+966500000007", "عمر الزهراني", "omar@kathirha.app", 18000,
                MockOpenBankingProvider.Preset.BALANCED, 0.33);
        saver("+966500000008", "سلطان المطيري", "sultan@kathirha.app", 22000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.20);
    }

    private void saver(String phone, String name, String email, int income,
                       MockOpenBankingProvider.Preset preset, double rate) {
        User u = new User();
        u.setPhone(phone);
        u.setDisplayName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("demo1234"));
        u.setRole(Role.USER);
        u.setCompetitiveOptIn(true);
        users.save(u);
        transactionService.importTransactions(u, BigDecimal.valueOf(income), 3, preset, rate);
        incomeService.detectAndVerify(u);
    }

    private void card(String name, TransactionCategory cat, String pct, String fee, String desc, String emoji) {
        CashbackCard c = new CashbackCard();
        c.setName(name);
        c.setRewardCategory(cat);
        c.setCashbackPercent(new BigDecimal(pct));
        c.setAnnualFee(new BigDecimal(fee));
        c.setDescription(desc);
        c.setEmoji(emoji);
        cards.save(c);
    }

    private void item(String name, String desc, String category, int cost, PointsType type, Long seasonId, String emoji) {
        ShopItem s = new ShopItem();
        s.setName(name);
        s.setDescription(desc);
        s.setCategory(category);
        s.setCostPoints(cost);
        s.setPointsType(type);
        s.setSeasonId(seasonId);
        s.setEmoji(emoji);
        s.setActive(true);
        shop.save(s);
    }
}
