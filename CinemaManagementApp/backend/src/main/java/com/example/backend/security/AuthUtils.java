package com.example.backend.security;

import com.example.backend.entities.User;
import com.example.backend.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public class AuthUtils {

    private AuthUtils() {}

    public static User requireAuthUser(HttpServletRequest request) {
        Object obj = request.getAttribute(AuthTokenFilter.AUTH_USER_ATTR);
        if (obj == null) {
            throw new UnauthorizedException("No authenticated user in request");
        }
        return (User) obj;
    }
}
