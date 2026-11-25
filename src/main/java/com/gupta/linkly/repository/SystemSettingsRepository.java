package com.gupta.linkly.repository;

import com.gupta.linkly.entity.SystemSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SystemSettings s where s.id = 'DEFAULT'")
    Optional<SystemSettings> findDefaultForUpdate();
}
