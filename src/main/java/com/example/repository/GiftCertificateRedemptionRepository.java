package com.example.repository;

import com.example.model.entity.GiftCertificateRedemptions;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiftCertificateRedemptionRepository extends JpaRepository<GiftCertificateRedemptions, Long> {
    Optional<GiftCertificateRedemptions> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gcr FROM GiftCertificateRedemptions gcr WHERE gcr.bookingId = :bookingId")
    Optional<GiftCertificateRedemptions> findByBookingIdWithLock(@Param("bookingId") Long bookingId);

    @Query("""
            SELECT gc.promoCode
            FROM GiftCertificateRedemptions gcr
            JOIN GiftCertificates gc ON gc.id = gcr.giftCertificateId
            WHERE gcr.bookingId = :bookingId
            """)
    Optional<String> findPromoCodeByBookingId(@Param("bookingId") Long bookingId);

        @Query(value = """
            SELECT gcr.booking_id AS bookingId, gc.promo_code AS promoCode
            FROM gift_certificate_redemptions gcr
            JOIN gift_certificates gc ON gc.id = gcr.gift_certificate_id
            WHERE gcr.booking_id IN :bookingIds
            """, nativeQuery = true)
        List<Object[]> findPromoCodesByBookingIds(@Param("bookingIds") List<Long> bookingIds);
}
