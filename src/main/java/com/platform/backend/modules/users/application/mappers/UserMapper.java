package com.platform.backend.modules.users.application.mappers;

import com.platform.backend.modules.users.domain.entities.RolesEntity;
import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.modules.users.presentation.responses.UserRoleResponse.UserRoleResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "password", source = "password")
    UsersEntity toEntity(RegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    UsersEntity toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget UsersEntity entity, UpdateProfileRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    // The organization is changed by the service, which checks who is asking: mapping it here would let anyone move an account.
    @Mapping(target = "organizationId", ignore = true)
    void updateEntity(@MappingTarget UsersEntity entity, UpdateUserRequest request);

    UpdateProfileResponse toUpdateProfileResponse(UsersEntity entity);

    UserResponse toUserResponse(UsersEntity entity);

    // The roles are not a property of the entity: the service reads those of the whole page at once and sets them.
    UserListItemResponse toUserListItemResponse(UsersEntity entity);

    UserRoleResponse toUserRoleResponse(RolesEntity role);
}
