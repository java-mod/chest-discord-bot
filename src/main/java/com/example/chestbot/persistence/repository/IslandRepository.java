package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.entity.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IslandRepository extends JpaRepository<IslandEntity, Long> {
    Optional<IslandEntity> findByGuildIdAndSlug(Long guildId, String slug);
    Optional<IslandEntity> findFirstByGuildId(Long guildId);
    Optional<IslandEntity> findByJoinCode(String joinCode);

    /** 라이선스를 한 번의 JOIN으로 함께 로드 (N+1 방지) */
    @Query("SELECT i FROM IslandEntity i LEFT JOIN FETCH i.license")
    List<IslandEntity> findAllWithLicense();

    /** 단일 섬 조회 시 라이선스 즉시 로드 (인터셉터 재검증용) */
    @Query("SELECT i FROM IslandEntity i LEFT JOIN FETCH i.license WHERE i.id = :id")
    Optional<IslandEntity> findByIdWithLicense(@Param("id") Long id);

    /** 특정 라이선스가 할당된 섬 조회 (섬 로그인용) */
    Optional<IslandEntity> findByLicense(LicenseEntity license);
}
