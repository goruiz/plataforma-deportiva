package com.platform.backend.modules.users.presentation.requests.LoginRequest;

/**
 * Identifies which client app is performing the login, so UsersAuthService can
 * enforce that only admins reach the admin panel and admins never authenticate
 * through the end-user mobile app.
 */
public enum ClientType {
    ADMIN_PANEL,
    MOBILE_APP
}
