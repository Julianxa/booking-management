package com.example.repository;

import com.example.model.entity.GiftCertificateItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GiftCertificateItemsRepository extends JpaRepository<GiftCertificateItems, String> {
    @Query(value = """
    SELECT *
    FROM gift_certificate_items gci
    WHERE gci.gift_certificate_id = :giftCertificateId
    """, nativeQuery = true)
    Optional<List<GiftCertificateItems>> getEventCertByGiftCertificateId(Long giftCertificateId);

    @Query(value = """
    SELECT *
    FROM gift_certificate_items gci
    WHERE gci.gift_certificate_id = :giftCertificateId
    """, nativeQuery = true)
    Optional<GiftCertificateItems> getValueCertByGiftCertificateId(Long giftCertificateId);
}
