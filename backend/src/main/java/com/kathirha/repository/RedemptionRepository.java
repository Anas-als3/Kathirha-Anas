package com.kathirha.repository;

import com.kathirha.domain.Redemption;
import com.kathirha.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    // Eagerly fetch shopItem so views can read it after the entity is detached
    // (open-in-view is disabled).
    @EntityGraph(attributePaths = "shopItem")
    List<Redemption> findByUserOrderByCreatedAtDesc(User user);

    @Query("select r from Redemption r join fetch r.shopItem")
    List<Redemption> findAllWithItem();

    long count();
    void deleteByUser(User user);
}
