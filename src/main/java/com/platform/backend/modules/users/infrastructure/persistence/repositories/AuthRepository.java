package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.irepositories.IAuthRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersJpaRepository;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AuthRepository {

}