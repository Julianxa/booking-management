package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetListGiftCertificateResponseDTO {
    @Schema(description = "Gift Certificate on current page")
    List<CreateGiftCertificateResponseDTO> content;

    @Schema(description = "Pageable request details")
    PageableDetail pageable;

    @Schema(description = "Is this the last page?", example = "true")
    boolean last;

    @Schema(description = "Total pages", example = "8")
    int totalPages;

    @Schema(description = "Total events", example = "142")
    long totalElements;

    @Schema(description = "Page size", example = "20")
    int size;

    @Schema(description = "Current page number (0-based)", example = "0")
    int number;

    @Schema(description = "Sort summary (often duplicates pageable.sort)")
    SortDetail sort;

    @Schema(description = "Is this the first page?", example = "true")
    boolean first;

    @Schema(description = "Number of elements on this page", example = "20")
    int numberOfElements;

    @Schema(description = "Is the result empty?", example = "false")
    boolean empty;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @Getter
    @Setter
    public static class PageableDetail {
        SortDetail sort;
        @Schema(example = "0")
        long offset;
        @Schema(example = "0")
        int pageNumber;
        @Schema(example = "20")
        int pageSize;
        @Schema(example = "true")
        boolean paged;
        @Schema(example = "false")
        boolean unpaged;
    }

    @Setter
    @Getter
    public static class SortDetail {
        @Schema(example = "false")
        boolean empty;
        @Schema(example = "true")
        boolean sorted;
        @Schema(example = "false")
        boolean unsorted;
    }
}
