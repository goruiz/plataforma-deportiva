package com.platform.backend.modules.users.domain.irepositories;

import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;

public interface IAuthRepository {

    RegisterResponse register(
            RegisterRequest registerRequest
    );
}