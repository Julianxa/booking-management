package com.example.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.Set;

@Getter
public abstract class AbstractPartialUpdateDto implements PartialUpdateSupport {

    @JsonIgnore
    private Set<String> presentFields = Set.of();

    void setPresentFields(Set<String> presentFields) {
        this.presentFields = Set.copyOf(presentFields);
    }

    @Override
    public boolean hasField(String jsonPropertyName) {
        return presentFields.contains(jsonPropertyName);
    }
}
