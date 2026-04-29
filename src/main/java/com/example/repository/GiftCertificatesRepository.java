package com.example.repository;

import com.example.model.entity.GiftCertificates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GiftCertificatesRepository extends JpaRepository<GiftCertificates, String> {
    @Query("SELECT gc.promoCode FROM GiftCertificates gc WHERE gc.id = :id")
    String findPromoCodeById(Long id);

    Optional<GiftCertificates> findByPromoCode(String promoCode);

    boolean existsByRefNo(String refNo);

    boolean existsByPromoCode(String promoCode);

    Page<GiftCertificates> findByEventId(Long eventId, Pageable pageable);
}