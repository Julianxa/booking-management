package com.example.repository;

import com.example.model.entity.GiftCertificateRedemptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GiftCertificateRedemptionRepository extends JpaRepository<GiftCertificateRedemptions, String> {
    Optional<GiftCertificateRedemptions> findByBookingId(Long bookingId);

    @Query("""
            SELECT gc.promoCode
            FROM GiftCertificateRedemptions gcr
            JOIN GiftCertificates gc ON gc.id = gcr.giftCertificateId
            WHERE gcr.bookingId = :bookingId
            """)
    Optional<String> findPromoCodeByBookingId(@Param("bookingId") Long bookingId);
}
