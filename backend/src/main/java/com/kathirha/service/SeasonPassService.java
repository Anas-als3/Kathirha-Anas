package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.RedemptionRepository;
import com.kathirha.repository.SeasonTierClaimRepository;
import com.kathirha.repository.ShopItemRepository;
import com.kathirha.repository.UserRepository;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The seasonal battle pass: seasonal points climb a 7-tier reward track.
 * Rewards are REAL — wallet points, streak freezes, actual coupons delivered over WhatsApp —
 * and the leaderboard score is never affected by claiming (earn-only stays earn-only).
 */
@Service
public class SeasonPassService {

    /** kind: POINTS (value = wallet points) · FREEZE · COUPON (itemName) · BADGE · CARD */
    public record TierDef(int tier, int threshold, String title, String desc, String kind, int value, String itemName) {}

    private static final List<TierDef> TIERS = List.of(
            new TierDef(1, 100, "نقاط محفظة", "+50 نقطة تُضاف لمحفظتك فورًا", "POINTS", 50, null),
            new TierDef(2, 250, "قسيمة قهوة Barn's", "قسيمة حقيقية تصلك على WhatsApp", "COUPON", 0, "قسيمة قهوة Barn's"),
            new TierDef(3, 400, "تجميد سلسلة", "يحمي سلسلتك يومًا كاملًا عند الانشغال", "FREEZE", 1, null),
            new TierDef(4, 550, "نقاط محفظة", "+100 نقطة تُضاف لمحفظتك فورًا", "POINTS", 100, null),
            new TierDef(5, 700, "قسيمة HungerStation", "قسيمة توصيل طعام تصلك على WhatsApp", "COUPON", 0, "قسيمة HungerStation بقيمة 25 ريال"),
            new TierDef(6, 850, "شارة الموسم", "شارة الموسم الحصرية في ملفك", "BADGE", 0, null),
            new TierDef(7, 1000, "بطاقة موسم الصيف", "الجائزة الكبرى: بطاقة الموسم الحصرية تُضاف إلى بطاقاتك — مع استرداد وخصومات لدى شركاء الموسم", "CARD", 0, null)
    );

    /** كثّرها+ premium track — better WALLET-side rewards at the same thresholds. Never touches the score. */
    private static final List<TierDef> PLUS_TIERS = List.of(
            new TierDef(1, 100, "نقاط مضاعفة", "+100 نقطة تُضاف لمحفظتك فورًا", "POINTS", 100, null),
            new TierDef(2, 250, "قسيمة Noon", "قسيمة تسوق 30 ريال تصلك على WhatsApp", "COUPON", 0, "قسيمة Noon بقيمة 30 ريال"),
            new TierDef(3, 400, "تجميدان للسلسلة", "حماية يومين كاملين لسلسلتك", "FREEZE", 2, null),
            new TierDef(4, 550, "نقاط مضاعفة", "+200 نقطة تُضاف لمحفظتك فورًا", "POINTS", 200, null),
            new TierDef(5, 700, "بطاقة هدايا Jarir", "قسيمة 50 ريال تصلك على WhatsApp", "COUPON", 0, "بطاقة هدايا Jarir بقيمة 50 ريال"),
            new TierDef(6, 850, "بطاقة الموسم الذهبية", "نسخة كثّرها+ الذهبية من بطاقة الموسم — لملفك وبطاقاتك", "BADGE", 0, null),
            new TierDef(7, 1000, "استرداد مضاعف", "مضاعفة استرداد بطاقة الموسم لدى شركاء الموسم", "CARD", 0, null)
    );

    /** Plus-track claims are stored with tier ids 101–107 so one claims table serves both tracks. */
    private static final int PLUS_OFFSET = 100;

    private final SeasonService seasons;
    private final PointsService points;
    private final SeasonTierClaimRepository claims;
    private final UserRepository users;
    private final ShopItemRepository shopItems;
    private final RedemptionRepository redemptions;
    private final WhatsAppService whatsapp;

    public SeasonPassService(SeasonService seasons, PointsService points, SeasonTierClaimRepository claims,
                             UserRepository users, ShopItemRepository shopItems,
                             RedemptionRepository redemptions, WhatsAppService whatsapp) {
        this.seasons = seasons;
        this.points = points;
        this.claims = claims;
        this.users = users;
        this.shopItems = shopItems;
        this.redemptions = redemptions;
        this.whatsapp = whatsapp;
    }

    public Map<String, Object> pass(User user) {
        Season season = seasons.current()
                .orElseThrow(() -> new ApiExceptions.NotFoundException("لا يوجد موسم نشط حاليًا"));
        int seasonal = points.seasonalEarned(user);
        Set<Integer> claimed = new HashSet<>();
        for (SeasonTierClaim c : claims.findByUserAndSeasonId(user, season.getId())) claimed.add(c.getTier());

        int level = 0;
        Integer nextThreshold = null;
        for (TierDef t : TIERS) {
            if (seasonal >= t.threshold()) level = t.tier();
            else if (nextThreshold == null) nextThreshold = t.threshold();
        }

        long daysLeft = season.getEndDate() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), season.getEndDate()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("seasonId", season.getId());
        out.put("seasonName", season.getName());
        out.put("description", season.getDescription());
        out.put("daysLeft", daysLeft);
        out.put("seasonalPoints", seasonal);
        out.put("level", level);
        out.put("maxLevel", TIERS.size());
        out.put("nextThreshold", nextThreshold);
        out.put("grandPrizeThreshold", TIERS.get(TIERS.size() - 1).threshold());
        out.put("plusActive", user.isPlusActive());
        out.put("tiers", tierViews(TIERS, seasonal, claimed, 0));
        out.put("plusTiers", tierViews(PLUS_TIERS, seasonal, claimed, PLUS_OFFSET));
        return out;
    }

    private List<Map<String, Object>> tierViews(List<TierDef> defs, int seasonal, Set<Integer> claimed, int offset) {
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (TierDef t : defs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tier", t.tier() + offset);
            m.put("threshold", t.threshold());
            m.put("title", t.title());
            m.put("desc", t.desc());
            m.put("kind", t.kind());
            m.put("unlocked", seasonal >= t.threshold());
            m.put("claimed", claimed.contains(t.tier() + offset));
            tiers.add(m);
        }
        return tiers;
    }

    /** Demo activation of كثّرها+ — in production this checks salary transfer / savings balance / subscription. */
    @Transactional
    public Map<String, Object> activatePlus(User user) {
        user.setPlusActive(true);
        users.save(user);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "تم تفعيل كثّرها+ 🌟 — مسار المكافآت المميّز أصبح مفتوحًا");
        out.put("pass", pass(user));
        return out;
    }

    @Transactional
    public Map<String, Object> claim(User user, int tier) {
        Season season = seasons.current()
                .orElseThrow(() -> new ApiExceptions.NotFoundException("لا يوجد موسم نشط حاليًا"));
        boolean plus = tier > PLUS_OFFSET;
        if (plus && !user.isPlusActive()) {
            throw new ApiExceptions.BadRequestException("هذا المسار خاص بمشتركي كثّرها+ — فعّله أولًا");
        }
        int baseTier = plus ? tier - PLUS_OFFSET : tier;
        List<TierDef> defs = plus ? PLUS_TIERS : TIERS;
        TierDef def = defs.stream().filter(t -> t.tier() == baseTier).findFirst()
                .orElseThrow(() -> new ApiExceptions.NotFoundException("هذا المستوى غير موجود"));
        int seasonal = points.seasonalEarned(user);
        if (seasonal < def.threshold()) {
            throw new ApiExceptions.BadRequestException(
                    "تحتاج " + (def.threshold() - seasonal) + " نقطة موسمية إضافية لفتح هذا المستوى");
        }
        if (claims.existsByUserAndSeasonIdAndTier(user, season.getId(), tier)) {
            throw new ApiExceptions.BadRequestException("استلمت مكافأة هذا المستوى بالفعل");
        }

        String message;
        switch (def.kind()) {
            case "POINTS" -> {
                // SEASON_GIFT is wallet-only: excluded from earnedTotal so pass rewards never buy rank.
                String track = plus ? " (كثّرها+)" : "";
                points.award(user, def.value(), PointsType.NORMAL, PointsReason.SEASON_GIFT,
                        "مكافأة المستوى " + baseTier + track + " من " + season.getName(), season.getId());
                message = "أُضيفت " + def.value() + " نقطة إلى محفظتك 🎉";
            }
            case "FREEZE" -> {
                user.setStreakFreezes(user.getStreakFreezes() + def.value());
                users.save(user);
                message = "حصلت على تجميد سلسلة إضافي ❄️ — رصيدك الآن " + user.getStreakFreezes();
            }
            case "COUPON" -> {
                ShopItem item = shopItems.findByActiveTrueOrderByCostPointsAsc().stream()
                        .filter(i -> i.getName().equals(def.itemName())).findFirst()
                        .orElseThrow(() -> new ApiExceptions.NotFoundException("القسيمة غير متاحة حاليًا"));
                String code = "KTH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                Redemption r = new Redemption();
                r.setUser(user);
                r.setShopItem(item);
                r.setCostPoints(0);
                r.setPointsType(PointsType.SEASONAL);
                r.setCouponCode(code);
                r.setStatus(RedemptionStatus.OWNED);
                r.setDeliveredViaWhatsApp(true);
                redemptions.save(r);
                whatsapp.send(user, MessageCategory.COUPON,
                        "🎁 مكافأة الموسم: " + item.getEmoji() + " " + item.getName()
                                + "\nرمز القسيمة: *" + code + "*\nأبرِزه عند الدفع. بالهناء!",
                        "redemption:" + r.getId());
                message = "وصلتك القسيمة على WhatsApp — الرمز: " + code;
            }
            case "BADGE" -> message = "شارة الموسم الحصرية أصبحت في ملفك 🏅";
            case "CARD" -> {
                whatsapp.send(user, MessageCategory.COUPON,
                        "👑 مبروك! فتحت بطاقة " + season.getName()
                                + " — استرداد وخصومات لدى شركاء الموسم بانتظارك.", null);
                message = "مبروك! فتحت بطاقة الموسم 👑 — الجائزة الكبرى";
            }
            default -> message = "تم الاستلام";
        }

        SeasonTierClaim claim = new SeasonTierClaim();
        claim.setUser(user);
        claim.setSeasonId(season.getId());
        claim.setTier(tier);
        claims.save(claim);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", message);
        out.put("pass", pass(user));
        return out;
    }
}
