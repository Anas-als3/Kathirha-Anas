package com.kathirha.config;

import com.kathirha.domain.*;
import com.kathirha.repository.CashbackCardRepository;
import com.kathirha.repository.SeasonRepository;
import com.kathirha.repository.ShopItemRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.IncomeService;
import com.kathirha.service.PointsService;
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
    private final PointsService points;

    public DataSeeder(UserRepository users, PasswordEncoder encoder, CashbackCardRepository cards,
                      ShopItemRepository shop, SeasonRepository seasons, TransactionService transactionService,
                      IncomeService incomeService, PointsService points) {
        this.users = users;
        this.encoder = encoder;
        this.cards = cards;
        this.shop = shop;
        this.seasons = seasons;
        this.transactionService = transactionService;
        this.incomeService = incomeService;
        this.points = points;
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
        seedUsers(season);
        log.info("Seed complete: {} users.", users.count());
    }

    private void seedCards() {
        // realistic market rates — essentials earn more than indulgences, so cashback never
        // glorifies discretionary spending (the product rewards saving, not spending)
        card("بطاقة موفّر البقالة", TransactionCategory.GROCERIES, "4.00", "0", "استرداد نقدي 4% على كل مشتريات البقالة", "🛒");
        card("بطاقة الذوّاقة", TransactionCategory.FOOD_DELIVERY, "2.50", "120", "استرداد نقدي 2.5% على توصيل الطعام", "🍔");
        card("بطاقة عشّاق القهوة", TransactionCategory.COFFEE, "3.00", "60", "استرداد نقدي 3% في المقاهي", "☕");
        card("بطاقة المشاوير", TransactionCategory.TRANSPORT, "3.00", "0", "استرداد نقدي 3% على المواصلات والوقود", "🚗");
        card("بطاقة المتسوّق", TransactionCategory.SHOPPING, "2.00", "0", "استرداد نقدي 2% على التسوق", "🛍️");
        card("البطاقة اليومية", TransactionCategory.BILLS, "1.50", "0", "استرداد نقدي 1.5% على الفواتير", "🧾");
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

        // premium tier — partner-funded rewards; riyal value per point drops as the ladder climbs
        item("قسيمة نون الكبرى بقيمة 75 ريال", "قسيمة شريك تصلك على WhatsApp", "SHOPPING", 1000, PointsType.NORMAL, null, "🛍️");
        item("بطاقة هدايا جرير بقيمة 100 ريال", "إلكترونيات وكتب — بتمويل الشريك", "SHOPPING", 1400, PointsType.NORMAL, null, "🎁");
        item("خصم طيران أديل 150 ريال", "خصم شريك على رحلتك القادمة", "TRAVEL", 1500, PointsType.NORMAL, null, "✈️");

        // card skins — cosmetic designs bought with wallet points, applied to the user's hero card
        // (names must match SHOP_SKINS keys in frontend/src/lib/skins.js; art in /card-art)
        item("نبض البوليفارد", "سهرة نيون من قلب الرياض — لمن يدّخر بإيقاع المدينة", "SKIN", 600, PointsType.NORMAL, null, "🌃");
        item("قوس العلا", "صخرُ العلا وقمرُها — تاريخٌ يطلّ من نافذة القوس", "SKIN", 800, PointsType.NORMAL, null, "🏜️");
        item("لؤلؤ الخليج", "من أعماق الخليج — صبرُ المحّار هو الذي يصنع اللؤلؤ", "SKIN", 800, PointsType.NORMAL, null, "🌊");
        item("أفق الرياض", "أفق العاصمة عند الغروب — طموحٌ بارتفاع الأبراج", "SKIN", 1000, PointsType.NORMAL, null, "🌆");
        item("سعفة المجد", "خضرةُ النخل وكرمُه — عطاءٌ يثمر مع كل وديعة", "SKIN", 1200, PointsType.NORMAL, null, "🌴");
        item("دار الذهب", "زخرفةٌ ونقشٌ وذهبٌ خالص — فخامة الدور العريقة", "SKIN", 1500, PointsType.NORMAL, null, "✨");
    }

    private void seedUsers(Season season) {
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

        Long sid = season.getId();
        // League score = saving points (rate/40% × 1000, capped) + earned WALLET activity points.
        // Seasonal points power the battle pass only. Leagues: برونزي < 750 ≤ فضي < 1750 ≤ ذهبي < 3250 ≤ ماسي

        // ---- Primary demo user — SILVER, mid-table (coffee-leaker, ~22% ≈ 550 save pts) ----
        // 370 activity so أنس is Silver even at plain login; the demo-seed welcome bonus then skips (balance ≥ 200).
        boost(saver("+966500000001", "أنس السبيعي", "anas@kathirha.app", 9000,
                MockOpenBankingProvider.Preset.COFFEE_HEAVY, 0.22), 370, 820, sid);   // ≈ 918

        // ---- SILVER league (750–1749) ----
        boost(saver("+966500000004", "نورة الدوسري", "noura@kathirha.app", 9500,
                MockOpenBankingProvider.Preset.BALANCED, 0.30), 490, 610, sid);       // ≈ 1240
        boost(saver("+966500000009", "عبدالله السالم", "abdullah@kathirha.app", 6000,
                MockOpenBankingProvider.Preset.BALANCED, 0.25), 350, 300, sid);       // ≈ 975
        boost(saver("+966500000010", "جود الحمد", "joud@kathirha.app", 10000,
                MockOpenBankingProvider.Preset.COFFEE_HEAVY, 0.28), 410, 380, sid);   // ≈ 1110
        boost(saver("+966500000011", "أمل الصاعدي", "amal@kathirha.app", 8000,
                MockOpenBankingProvider.Preset.BALANCED, 0.44), 470, 260, sid);       // capped 1000 → ≈ 1470
        boost(saver("+966500000005", "فيصل الغامدي", "faisal@kathirha.app", 13000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.18), 330, 270, sid);     // ≈ 780

        // ---- GOLD league (1750–3249) ----
        boost(saver("+966500000003", "لينا القحطاني", "lina@kathirha.app", 11000,
                MockOpenBankingProvider.Preset.BALANCED, 0.45), 1250, 1420, sid);     // capped 1000 → ≈ 2250
        boost(saver("+966500000002", "خالد الحربي", "khalid@kathirha.app", 16000,
                MockOpenBankingProvider.Preset.BALANCED, 0.27), 1150, 900, sid);      // ≈ 1825
        boost(saver("+966500000007", "عمر الزهراني", "omar@kathirha.app", 18000,
                MockOpenBankingProvider.Preset.BALANCED, 0.33), 1050, 700, sid);      // ≈ 1875
        boost(saver("+966500000008", "سلطان المطيري", "sultan@kathirha.app", 22000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.20), 1450, 950, sid);    // ≈ 1950
        boost(saver("+966500000012", "طلال النعيمي", "talal@kathirha.app", 20000,
                MockOpenBankingProvider.Preset.BALANCED, 0.24), 1700, 1200, sid);     // ≈ 2300

        // ---- BRONZE league (< 750) ----
        boost(saver("+966500000013", "هند المالكي", "hind@kathirha.app", 12000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.08), 80, 120, sid);      // ≈ 280
        boost(saver("+966500000014", "مشعل العوفي", "meshal@kathirha.app", 5000,
                MockOpenBankingProvider.Preset.COFFEE_HEAVY, 0.15), 40, 60, sid);     // ≈ 415
        boost(saver("+966500000006", "منصور الشهري", "mansour@kathirha.app", 8500,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.13), 30, 80, sid);       // ≈ 355
        boost(saver("+966500000015", "يزيد الراشد", "yazeed@kathirha.app", 9000,
                MockOpenBankingProvider.Preset.FOOD_HEAVY, 0.10), 60, 90, sid);       // ≈ 310

        // ---- DIAMOND league (≥ 3250) ----
        boost(saver("+966500000016", "ريم العنزي", "reem@kathirha.app", 7000,
                MockOpenBankingProvider.Preset.BALANCED, 0.36), 2600, 1600, sid);     // ≈ 3500
        boost(saver("+966500000017", "فاطمة البقمي", "fatimah@kathirha.app", 15000,
                MockOpenBankingProvider.Preset.BALANCED, 0.38), 2500, 1500, sid);     // ≈ 3450
    }

    private User saver(String phone, String name, String email, int income,
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
        return u;
    }

    /** Earned activity points (missions/quiz/streaks) + seasonal points — makes leaderboards and the battle pass feel alive. */
    private void boost(User u, int activity, int seasonal, Long seasonId) {
        if (activity > 0) {
            points.award(u, activity, PointsType.NORMAL, PointsReason.CHALLENGE, "مهام وتحدّيات مكتملة", null);
        }
        if (seasonal > 0) {
            points.award(u, seasonal, PointsType.SEASONAL, PointsReason.CHALLENGE, "تحدّيات الموسم", seasonId);
        }
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
