package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByUserOrderBySortOrderAscCreatedAtDesc(User user);
    Optional<Link> findByShortUrl(String shortUrl);
    boolean existsByShortUrl(String shortUrl);
    long countByUser(User user);
    @Modifying
    @Query("UPDATE Link l SET l.clickCount = l.clickCount + 1 WHERE l.id = :id")
    void incrementClickCount(@Param("id") UUID id);
}
