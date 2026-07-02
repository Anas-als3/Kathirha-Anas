package com.kathirha.repository;

import com.kathirha.domain.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    List<ShopItem> findByActiveTrueOrderByCostPointsAsc();
}
