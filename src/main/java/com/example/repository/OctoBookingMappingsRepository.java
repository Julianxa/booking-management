package com.example.repository;

import com.example.model.entity.OctoBookingMappings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OctoBookingMappingsRepository extends JpaRepository<OctoBookingMappings, Long> {

    Optional<OctoBookingMappings> findByOctoUuid(String octoUuid);

    Optional<OctoBookingMappings> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("""
            SELECT m FROM OctoBookingMappings m
            WHERE (:resellerReference IS NULL OR m.resellerReference = :resellerReference)
              AND (:supplierReference IS NULL OR m.bookingRefNo = :supplierReference)
              AND (:localDateStart IS NULL OR m.createdAt >= :localDateStart)
              AND (:localDateEnd IS NULL OR m.createdAt < :localDateEnd)
            ORDER BY m.createdAt DESC
            """)
    List<OctoBookingMappings> findFiltered(
            @Param("resellerReference") String resellerReference,
            @Param("supplierReference") String supplierReference,
            @Param("localDateStart") ZonedDateTime localDateStart,
            @Param("localDateEnd") ZonedDateTime localDateEnd);
}
