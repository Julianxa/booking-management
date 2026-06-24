package com.example.repository;

import com.example.model.entity.GiftCertificates;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiftCertificatesRepository extends JpaRepository<GiftCertificates, Long> {
    List<GiftCertificates> findByRefNoOrderByIdAsc(String refNo);

    Optional<GiftCertificates> findByPromoCode(String promoCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gc FROM GiftCertificates gc WHERE gc.promoCode = :promoCode")
    Optional<GiftCertificates> findByPromoCodeWithLock(@Param("promoCode") String promoCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gc FROM GiftCertificates gc WHERE gc.id = :id")
    Optional<GiftCertificates> findByIdWithLock(@Param("id") Long id);

    boolean existsByRefNo(String refNo);

    boolean existsByPromoCode(String promoCode);

    Page<GiftCertificates> findByEventId(Long eventId, Pageable pageable);

    @Query(value = """
            SELECT gc.ref_no FROM gift_certificates gc
            WHERE (:eventId IS NULL OR gc.event_id = :eventId)
            GROUP BY gc.ref_no
            """,
            countQuery = """
            SELECT COUNT(DISTINCT gc.ref_no) FROM gift_certificates gc
            WHERE (:eventId IS NULL OR gc.event_id = :eventId)
            """,
            nativeQuery = true)
    Page<String> findDistinctRefNos(@Param("eventId") Long eventId, Pageable pageable);

    List<GiftCertificates> findByRefNoInOrderByRefNoAscIdAsc(List<String> refNos);

}