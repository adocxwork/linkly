package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Message> findByIdAndUserUsername(UUID id, String username);
}
