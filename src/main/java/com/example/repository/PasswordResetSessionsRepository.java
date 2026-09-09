package com.example.repository;

import com.example.model.entity.PasswordResetSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetSessionsRepository extends JpaRepository<PasswordResetSessions, Long> {
    Optional<PasswordResetSessions> findByTokenHash(String tokenHash);

    long countByEmailAndCreatedAtAfter(String email, ZonedDateTime createdAfter);

    @Modifying
    @Query("UPDATE PasswordResetSessions s SET s.status = :superseded, s.updatedAt = :now "
            + "WHERE s.email = :email AND s.status IN (:activeStatuses)")
    int supersedeActiveByEmail(
            @Param("email") String email,
            @Param("now") ZonedDateTime now,
            @Param("superseded") PasswordResetSessions.Status superseded,
            @Param("activeStatuses") java.util.Collection<PasswordResetSessions.Status> activeStatuses);
}
