package com.example.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

public final class EventSortUtils {
    private EventSortUtils() {
    }

    public static Pageable forEventList(int page, int size, String sortBy, Sort.Direction direction) {
        String field = resolveSortField(sortBy);
        Sort.Direction resolvedDirection = direction != null ? direction : Sort.Direction.ASC;

        if ("sequenceNo".equals(field)) {
            Sort nullsLast = JpaSort.unsafe(Sort.Direction.ASC, "CASE WHEN sequenceNo IS NULL THEN 1 ELSE 0 END");
            Sort sequenceSort = Sort.by(resolvedDirection, "sequenceNo");
            return PageRequest.of(page, size, nullsLast.and(sequenceSort));
        }

        return PageRequest.of(page, size, Sort.by(resolvedDirection, field));
    }

    private static String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "sequenceNo";
        }
        if ("sequence_no".equals(sortBy)) {
            return "sequenceNo";
        }
        return sortBy;
    }
}
