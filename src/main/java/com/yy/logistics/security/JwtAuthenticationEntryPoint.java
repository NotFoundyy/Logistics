package com.yy.logistics.security;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonResponseWriter jsonResponseWriter;

    public JwtAuthenticationEntryPoint(JsonResponseWriter jsonResponseWriter) {
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        jsonResponseWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                ApiResponse.fail(ErrorCode.UNAUTHORIZED)
        );
    }
}

