package com.example.repository;

import com.example.model.entity.GiftCertificateItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftCertificateItemsRepository extends JpaRepository<GiftCertificateItems, String> {
    List<GiftCertificateItems> findByGiftCertificatesId(Long giftCertificateId);
}
