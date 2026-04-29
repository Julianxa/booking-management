package com.example.repository;

import com.example.model.entity.GiftCertificateRedemptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GiftCertificateRedemptionRepository extends JpaRepository<GiftCertificateRedemptions, String>  {
    Optional<GiftCertificateRedemptions> findByBookingId(Long bookingId);
}
