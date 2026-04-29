package com.example.model.record;

import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.GiftCertificates;

import java.math.BigDecimal;
import java.util.List;

public record GiftCertificateApplicationResult(
        GiftCertificates certificate,
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketTypes,
        BigDecimal discount
) {}