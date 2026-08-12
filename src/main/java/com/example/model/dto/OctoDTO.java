package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


public final class OctoDTO {
    private OctoDTO() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Contact {
        private String name;
        private String emailAddress;
        private String phoneNumber;
        private List<String> locales;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Supplier {
        private String id;
        private String name;
        private String endpoint;
        private Contact contact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Pricing {
        private Integer original;
        private Integer retail;
        private Integer net;
        private String currency;
        private Integer currencyPrecision;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Unit {
        private String id;
        private String internalName;
        private String type;
        private List<String> requiredContactFields;
        private List<Pricing> pricingFrom;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptionRestrictions {
        private Integer minUnits;
        private Integer maxUnits;
        private Boolean minPaxCount;
        private Integer minAdults;
        private Integer maxAdults;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Option {
        private String id;
        private Boolean defaultOption;
        private String internalName;
        private OptionRestrictions restrictions;
        private List<Unit> units;

        @com.fasterxml.jackson.annotation.JsonProperty("default")
        public Boolean getDefaultOption() {
            return defaultOption;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("default")
        public void setDefaultOption(Boolean defaultOption) {
            this.defaultOption = defaultOption;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Product {
        private String id;
        private String internalName;
        private String reference;
        private String locale;
        private String timeZone;
        private Boolean allowFreesale;
        private Boolean instantConfirmation;
        private Boolean instantDelivery;
        private Boolean availabilityRequired;
        private String availabilityType;
        private List<String> deliveryFormats;
        private List<String> deliveryMethods;
        private String redemptionMethod;
        private List<Option> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UnitPricing {
        private String unitId;
        private Pricing pricing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Availability {
        private String id;
        private String localDateTimeStart;
        private String localDateTimeEnd;
        private Boolean allDay;
        private Boolean available;
        private String status;
        private Integer vacancies;
        private Integer capacity;
        private Integer maxUnits;
        private Integer maxUnit;
        private List<UnitPricing> unitPricing;
        private Pricing pricing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityUnitRequest {
        private String id;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityRequest {
        private String productId;
        private String optionId;
        private String localDateStart;
        private String localDateEnd;
        private String availabilityId;
        private List<AvailabilityUnitRequest> units;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityCalendarRequest {
        private String productId;
        private String optionId;
        private String localDateStart;
        private String localDateEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvailabilityCalendarDay {
        private String localDate;
        private Boolean available;
        private String status;
        private Integer vacancies;
        private Integer capacity;
        private Integer openings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitItemRequest {
        private String uuid;
        private String unitId;
        private Contact resellerReference;
        private Contact contact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingReservationRequest {
        private String uuid;
        private String productId;
        private String optionId;
        private String availabilityId;
        private List<UnitItemRequest> unitItems;
        private Contact holder;
        private String notes;
        private String resellerReference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingConfirmRequest {
        private String emailReceipt;
        private String resellerReference;
        private Contact contact;
        private Map<String, Object> additionalField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingCancelRequest {
        private String reason;
        private String force;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryOption {
        private String deliveryFormat;
        private String deliveryValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Ticket {
        private String redemptionMethod;
        private List<DeliveryOption> deliveryOptions;
        private String utcRedeemedAt;
        private String utcUpdatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UnitItem {
        private String uuid;
        private String unitId;
        private String unitType;
        private String status;
        private Contact contact;
        private Ticket ticket;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Booking {
        private String id;
        private String uuid;
        private Boolean testMode;
        private String resellerReference;
        private String supplierReference;
        private String status;
        private String utcExpiresAt;
        private String utcConfirmedAt;
        private Boolean cancellable;
        private Boolean freeCancellationAvailable;
        private String productId;
        private String optionId;
        private Availability availability;
        private Contact contact;
        private List<UnitItem> unitItems;
        private String voucher;
        private List<DeliveryOption> deliveryOptions;
        private Pricing pricing;
        private String notes;
    }
}
