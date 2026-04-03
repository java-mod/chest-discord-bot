package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.ChestDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChestDefinitionRepository extends JpaRepository<ChestDefinitionEntity, Long> {
    List<ChestDefinitionEntity> findAllByConfigVersionIdOrderByIdAsc(Long configVersionId);
}
