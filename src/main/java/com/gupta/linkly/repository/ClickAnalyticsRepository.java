package com.gupta.linkly.repository;

import com.gupta.linkly.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * All analytics are aggregated by PostgreSQL (GROUP BY) instead of loading every click row
 * into Java memory, so a link with a million clicks costs the same as one with ten.
 */
@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, UUID> {

    interface LabelCount {
        String getLabel();
        long getCount();
    }

    @Query("""
            select coalesce(c.country, 'Unknown') as label, count(c) as count
            from ClickAnalytics c where c.link.id = :linkId
            group by coalesce(c.country, 'Unknown') order by count(c) desc
            """)
    List<LabelCount> countByCountry(@Param("linkId") UUID linkId);

    @Query("""
            select coalesce(c.deviceType, 'Unknown') as label, count(c) as count
            from ClickAnalytics c where c.link.id = :linkId
            group by coalesce(c.deviceType, 'Unknown') order by count(c) desc
            """)
    List<LabelCount> countByDevice(@Param("linkId") UUID linkId);

    @Query("""
            select coalesce(c.browser, 'Unknown') as label, count(c) as count
            from ClickAnalytics c where c.link.id = :linkId
            group by coalesce(c.browser, 'Unknown') order by count(c) desc
            """)
    List<LabelCount> countByBrowser(@Param("linkId") UUID linkId);

    @Query("select count(distinct c.ipHash) from ClickAnalytics c where c.link.id = :linkId and c.ipHash is not null")
    long countUniqueVisitors(@Param("linkId") UUID linkId);

    /** Rows of [day (date), clicks (long)] for the given window. */
    @Query(value = """
            select cast(clicked_at as date) as day, count(*) as clicks
            from click_analytics
            where link_id = :linkId and clicked_at >= :since
            group by cast(clicked_at as date)
            order by day
            """, nativeQuery = true)
    List<Object[]> countPerDaySince(@Param("linkId") UUID linkId, @Param("since") LocalDateTime since);
}
