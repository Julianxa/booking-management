package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class OctoDTO {
    private OctoDTO() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        /** Klook/OCTO field; also accepts legacy {@code name} on deserialize. */
        @com.fasterxml.jackson.annotation.JsonAlias("name")
        private String fullName;
        private String firstName;
        private String lastName;
        private String emailAddress;
        private String phoneNumber;
        private List<String> locales;
        private String postalCode;
        private String country;
        private String notes;
    }

    /** Supplier contact shape per Klook/OCTO GET /supplier. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierContact {
        private String website;
        private String email;
        private String telephone;
        private String address;
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
        private SupplierContact contact;
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
    public static class OpeningHours {
        private String from;
        private String to;
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
        private String utcCutoffAt;
        private Boolean allDay;
        private Boolean available;
        private String status;
        private Integer vacancies;
        private Integer capacity;
        private Integer maxUnits;
        private List<OpeningHours> openingHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AvailabilityUnitRequest {
        private String id;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AvailabilityRequest {
        private String productId;
        private String optionId;
        private String localDateStart;
        private String localDateEnd;
        private List<AvailabilityUnitRequest> units;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnitItemRequest {
        private String uuid;
        private String unitId;
    }

    /**
     * POST /bookings reservation body per Klook OpenAPI.
     * Required: productId, optionId, availabilityId, unitItems.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookingReservationRequest {
        private String uuid;
        private String productId;
        private String optionId;
        private String availabilityId;
        private Integer expirationMinutes;
        private String notes;
        private List<UnitItemRequest> unitItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfirmUnitItemRequest {
        private String uuid;
        private String unitId;
        private String resellerReference;
        private Contact contact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookingConfirmRequest {
        private Boolean emailReceipt;
        private String resellerReference;
        private Contact contact;
        private List<ConfirmUnitItemRequest> unitItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookingCancelRequest {
        private String reason;
        private Boolean force;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cancellation {
        private String refund;
        private String reason;
        private String utcCancelledAt;
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
        private String utcRedeemedAt;
        private List<DeliveryOption> deliveryOptions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UnitItem {
        private String uuid;
        private String unitId;
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
        private String utcCreatedAt;
        private String utcUpdatedAt;
        private String utcExpiresAt;
        private String utcRedeemedAt;
        private String utcConfirmedAt;
        private Boolean cancellable;
        private Cancellation cancellation;
        private String productId;
        private String optionId;
        private Availability availability;
        private Contact contact;
        private List<String> deliveryMethods;
        private List<UnitItem> unitItems;
        private String notes;
    }
}
