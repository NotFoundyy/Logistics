package com.yy.logistics.security;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonResponseWriter jsonResponseWriter;

    public JwtAccessDeniedHandler(JsonResponseWriter jsonResponseWriter) {
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        jsonResponseWriter.write(
                response,
                HttpStatus.FORBIDDEN.value(),
                ApiResponse.fail(ErrorCode.FORBIDDEN)
        );
    }
}

