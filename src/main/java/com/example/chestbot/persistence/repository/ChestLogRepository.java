package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.ChestLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChestLogRepository extends JpaRepository<ChestLogEntity, Long> {

    Page<ChestLogEntity> findByIslandIdOrderByCreatedAtDesc(Long islandId, Pageable pageable);

    java.util.List<ChestLogEntity> findTop100ByIslandIdOrderByCreatedAtDesc(Long islandId);

    @Query("SELECT c FROM ChestLogEntity c WHERE c.island.id = :islandId " +
           "AND (:playerName IS NULL OR LOWER(c.playerName) LIKE LOWER(CONCAT('%', :playerName, '%'))) " +
           "AND (:chestKey IS NULL OR LOWER(c.chestKey) LIKE LOWER(CONCAT('%', :chestKey, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<ChestLogEntity> searchByIslandId(@Param("islandId") Long islandId,
                                          @Param("playerName") String playerName,
                                          @Param("chestKey") String chestKey,
                                          Pageable pageable);
}
