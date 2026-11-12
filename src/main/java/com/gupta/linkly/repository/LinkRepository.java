package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {

    /** Everything the redirect endpoint needs, in one query (no lazy loading of the user). */
    interface RedirectTarget {
        UUID getId();
        String getOriginalUrl();
        Boolean getActive();
        Boolean getOwnerSuspended();
    }

    @Query("""
            select l.id as id, l.originalUrl as originalUrl, l.active as active, u.isSuspended as ownerSuspended
            from Link l join l.user u
            where l.shortUrl = :shortUrl
            """)
    Optional<RedirectTarget> findRedirectTarget(@Param("shortUrl") String shortUrl);

    List<Link> findByUserIdOrderBySortOrderAscCreatedAtDesc(UUID userId);

    List<Link> findByUserIdAndActiveTrueOrderBySortOrderAscCreatedAtDesc(UUID userId);

    /** Ownership check and fetch in one query: returns empty if the link belongs to someone else. */
    Optional<Link> findByIdAndUserUsername(UUID id, String username);

    boolean existsByShortUrl(String shortUrl);

    long countByUserId(UUID userId);

    @Query("select coalesce(sum(l.clickCount), 0) from Link l where l.user.id = :userId")
    long sumClickCountByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(min(l.sortOrder), 0) from Link l where l.user.id = :userId")
    int findMinSortOrder(@Param("userId") UUID userId);

    @Query("select l.shortUrl from Link l where l.user.id = :userId")
    List<String> findShortUrlsByUserId(@Param("userId") UUID userId);

    @Query("select l.id from Link l where l.id in :ids")
    List<UUID> findExistingIds(@Param("ids") Collection<UUID> ids);

    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + :delta where l.id = :id")
    int incrementClickCount(@Param("id") UUID id, @Param("delta") long delta);
}
