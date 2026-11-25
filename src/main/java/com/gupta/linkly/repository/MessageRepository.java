package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    @Query(value = """
            SELECT * FROM messages
            WHERE user_id = :userId AND (created_at, id) < (:before, :beforeId)
            ORDER BY created_at DESC, id DESC
            """, nativeQuery = true)
    List<Message> findOlder(@Param("userId") UUID userId, @Param("before") LocalDateTime before,
                           @Param("beforeId") UUID beforeId, Pageable pageable);

    Optional<Message> findByIdAndUserUsername(UUID id, String username);
}
