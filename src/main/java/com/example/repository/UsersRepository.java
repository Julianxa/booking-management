package com.example.repository;

import com.example.constant.Enums;
import com.example.model.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByRefNo(String refNo);
    @Query("SELECT u.id FROM Users u WHERE u.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);
    @Query("SELECT u.refNo FROM Users u WHERE u.id = :id")
    Optional<String> findRefNoById(Long id);
    Optional<Users> findByUserSub(String userSub);
    Optional<Users> findByEmailAndStatus(String email, Enums.UserStatus status);
    Optional<Users> findByEmail(String email);
    @NotNull
    Optional<Users> findById(@NotNull Long id);

    Optional<Users> findByIdAndRole(Long id, Enums.UserRole role);

    @Modifying
    @Query(value = "UPDATE users SET status = 'INACTIVE', updated_at = :timestamp, deleted_at = :timestamp WHERE id = :ownerUserId", nativeQuery = true)
    void updateStatusToInactiveByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("timestamp") LocalDateTime timestamp);

    Page<Users> findByRole(Enums.UserRole role, Pageable pageable);

    @Query(value="SELECT u FROM users u WHERE " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))", nativeQuery = true)
    Page<Users> findBySearchTerm(@Param("search") String search, Pageable pageable);

    @Query(
            value =
                    "SELECT * FROM users " +
                            "WHERE org_id=:orgId " +
                            "AND (:search IS NULL OR " +
                            "     LOWER(email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                            "     LOWER(first_name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                            "     LOWER(last_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                            "AND (:role IS NULL OR role = :role) " +
                            "ORDER BY created_at DESC",
            nativeQuery = true
    )
    Page<Users> findByOrganizationIdAndFilters(
            @Param("orgId") Long orgId,
            @Param("search") String search,
            @Param("role") Enums.UserRole role,
            Pageable pageable
    );
}

