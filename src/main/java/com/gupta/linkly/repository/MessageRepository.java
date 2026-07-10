package com.gupta.linkly.repository;

import com.gupta.linkly.entity.Message;
import com.gupta.linkly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByUserOrderByCreatedAtDesc(User user);
}
