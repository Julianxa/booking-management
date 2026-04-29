package com.example.mapper;

import com.example.model.dto.GetUserResponseDTO;
import com.example.model.dto.UpdateUserResponseDTO;
import com.example.model.dto.UserRegistrationRequestDTO;
import com.example.model.entity.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", source = "role", defaultValue = "USER")
    Users toEntity(UserRegistrationRequestDTO dto);

    @Mapping(target = "id", source = "entity.refNo")
    @Mapping(target = "orgId", source = "orgRefNo")
    GetUserResponseDTO toResponseDto(Users entity, String orgRefNo);

    UpdateUserResponseDTO toUpdateResponseDto(Users entity);
}
