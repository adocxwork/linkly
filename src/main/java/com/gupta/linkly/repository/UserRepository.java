package com.gupta.linkly.repository;

import com.gupta.linkly.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(com.gupta.linkly.entity.Role role);

    List<User> findAll(Sort sort);

    /** Single DELETE; links, messages and clicks go via ON DELETE CASCADE in the database. */
    @Modifying
    @Query("delete from User u where u.id = :id")
    void hardDelete(@Param("id") UUID id);
}
