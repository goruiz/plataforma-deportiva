package com.platform.backend.modules.users.application.mappers;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
        componentModel = "spring", // Genera el mapper como un Bean de Spring (para poder inyectarlo con @Autowired o constructor)
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,   // Ignora los campos del destino (Entity) que no se mapean desde el origen
        unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE    // Ignora los campos del origen (Request) que no existen en el destino
)
public interface UserMapper {


    //Convierte RegisterRequest → UsersEntity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "password", source = "password")// Se encriptará después
    UsersEntity toEntity(RegisterRequest request);



    //Actualizar entidad existente
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget UsersEntity entity, RegisterRequest request);
}