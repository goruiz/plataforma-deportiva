package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;

public interface IAuthService {
    RegisterResponse register(RegisterRequest registerRequest);
}
