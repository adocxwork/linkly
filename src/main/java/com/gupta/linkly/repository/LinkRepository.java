package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByUserOrderBySortOrderAscCreatedAtDesc(User user);
    Optional<Link> findByShortUrl(String shortUrl);
    boolean existsByShortUrl(String shortUrl);
    long countByUser(User user);
}
