package com.gupta.linkly.repository;

import com.gupta.linkly.entity.ClickAnalytics;
import com.gupta.linkly.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, UUID> {
    List<ClickAnalytics> findByLink(Link link);
}
