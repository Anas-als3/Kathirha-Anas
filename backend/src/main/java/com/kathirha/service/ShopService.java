package com.kathirha.service;

import com.kathirha.domain.*;
import com.kathirha.repository.RedemptionRepository;
import com.kathirha.repository.ShopItemRepository;
import com.kathirha.service.ai.AiModels;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.ApiExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ShopService {

    private final ShopItemRepository items;
    private final RedemptionRepository redemptions;
    private final PointsService points;
    private final WhatsAppService whatsapp;
    private final SeasonService seasons;

    public ShopService(ShopItemRepository items, RedemptionRepository redemptions, PointsService points,
                       WhatsAppService whatsapp, SeasonService seasons) {
        this.items = items;
        this.redemptions = redemptions;
        this.points = points;
        this.whatsapp = whatsapp;
        this.seasons = seasons;
    }

    public List<ShopItem> list() {
        return items.findByActiveTrueOrderByCostPointsAsc();
    }

    public List<Redemption> redemptionsFor(User user) {
        return redemptions.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Redemption redeem(User user, Long itemId) {
        ShopItem item = items.findById(itemId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Reward not found"));
        if (!item.isActive() || item.getStock() <= 0) {
            throw new ApiExceptions.BadRequestException("This reward is out of stock");
        }
        Long seasonId = item.getPointsType() == PointsType.SEASONAL ? seasons.currentSeasonId() : null;
        points.spend(user, item.getCostPoints(), item.getPointsType(),
                PointsReason.REDEMPTION, "Redeemed " + item.getName(), seasonId);

        item.setStock(item.getStock() - 1);
        items.save(item);

        String code = "KTH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Redemption r = new Redemption();
        r.setUser(user);
        r.setShopItem(item);
        r.setCostPoints(item.getCostPoints());
        r.setPointsType(item.getPointsType());
        r.setCouponCode(code);
        r.setStatus(RedemptionStatus.OWNED);
        r.setDeliveredViaWhatsApp(true);
        redemptions.save(r);

        whatsapp.send(user, MessageCategory.COUPON,
                "🎉 Reward unlocked: " + item.getEmoji() + " " + item.getName()
                        + "\nYour coupon code: *" + code + "*\nShow it at checkout to redeem. Enjoy!",
                "redemption:" + r.getId());
        return r;
    }

    /** AI reward recommendation based on what the user can afford and their behaviour. */
    public AiModels.RewardRecommendation recommend(User user, int normalBalance) {
        List<ShopItem> affordable = list().stream()
                .filter(i -> i.getPointsType() == PointsType.NORMAL && i.getCostPoints() <= normalBalance)
                .sorted(Comparator.comparingInt(ShopItem::getCostPoints).reversed())
                .toList();
        if (affordable.isEmpty()) {
            ShopItem cheapest = list().stream()
                    .min(Comparator.comparingInt(ShopItem::getCostPoints)).orElse(null);
            int need = cheapest == null ? 0 : cheapest.getCostPoints() - normalBalance;
            return new AiModels.RewardRecommendation(
                    cheapest == null ? null : cheapest.getId(),
                    cheapest == null ? "—" : cheapest.getName(),
                    cheapest == null ? "🎁" : cheapest.getEmoji(),
                    "Complete " + Math.max(1, need / 15) + " more missions (~" + Math.max(0, need)
                            + " pts) to unlock your first reward.");
        }
        ShopItem best = affordable.get(0);
        return new AiModels.RewardRecommendation(best.getId(), best.getName(), best.getEmoji(),
                "You have " + normalBalance + " points — enough for " + best.getName()
                        + ". A popular pick among savers like you.");
    }
}
