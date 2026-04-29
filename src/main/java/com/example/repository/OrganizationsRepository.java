package com.example.repository;

import com.example.constant.Enums;
import com.example.model.entity.Organizations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrganizationsRepository extends JpaRepository<Organizations, Long> {
        @Query(value = """
            SELECT * FROM organizations
            WHERE (LOWER(name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(industry) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(company_type) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(company_group) LIKE LOWER(CONCAT('%', :search, '%')))
              AND status != 'INACTIVE'
            """, nativeQuery = true)
        Page<Organizations> findBySearchTerm(@Param("search") String search, Pageable pageable);

        @Query("SELECT o.id FROM Organizations o WHERE o.refNo = :orgRefNo")
        Optional<Long> findIdByRefNo(String orgRefNo);

        Optional<Organizations> findByRefNo(String refNo);

        @Query("SELECT o.refNo FROM Organizations o WHERE o.id = :id")
        Optional<String> findRefNoById(Long id);

        boolean existsByRefNo(String refNo);

        @Modifying
        @Transactional
        @Query("""
            UPDATE Organizations
            SET status = :status,
                updatedAt = :deletedAt,
                deletedAt = :deletedAt
            WHERE refNo = :organizationRefNo
            """)
        void updateStatusByOrganizationRefNo(String organizationRefNo, Enums.OrganizationStatus status, LocalDateTime deletedAt);

        @Query("""
            SELECT o FROM Organizations o
            WHERE o.status != 'INACTIVE'
            """)
        Page<Organizations> findAllActive(Pageable pageable);
}
