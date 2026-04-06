package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.IslandBankLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IslandBankLogRepository extends JpaRepository<IslandBankLogEntity, Long> {

    Page<IslandBankLogEntity> findByIslandIdOrderByCreatedAtDesc(Long islandId, Pageable pageable);

    @Query("SELECT b FROM IslandBankLogEntity b WHERE b.island.id = :islandId " +
           "AND (:playerName IS NULL OR LOWER(b.playerName) LIKE LOWER(CONCAT('%', :playerName, '%'))) " +
           "AND (:transactionType IS NULL OR b.transactionType = :transactionType) " +
           "ORDER BY b.createdAt DESC")
    Page<IslandBankLogEntity> searchByIslandId(@Param("islandId") Long islandId,
                                               @Param("playerName") String playerName,
                                               @Param("transactionType") String transactionType,
                                               Pageable pageable);
}
