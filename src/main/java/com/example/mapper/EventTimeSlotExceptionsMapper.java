package com.example.mapper;

import com.example.model.dto.UpdateEventStatusRequestDTO;
import com.example.model.entity.EventTimeSlotExceptions;
import com.example.model.entity.Events;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventTimeSlotExceptionsMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "exceptionDate", source = "dto.eventDate")
    @Mapping(target = "exceptionTime", source = "dto.eventTime")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EventTimeSlotExceptions toEntity(UpdateEventStatusRequestDTO dto, Events event);
}
