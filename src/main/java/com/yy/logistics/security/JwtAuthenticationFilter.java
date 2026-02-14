package com.yy.logistics.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.yy.logistics.auth.model.JwtPayload;
import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.auth.service.JwtTokenService;
import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final JsonResponseWriter jsonResponseWriter;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, JsonResponseWriter jsonResponseWriter) {
        this.jwtTokenService = jwtTokenService;
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || path.equals("/api/pricing/quote")
                || path.startsWith("/api/tracking/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            JwtPayload payload = jwtTokenService.parseToken(token);
            List<SimpleGrantedAuthority> authorities = payload.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            LoginUser loginUser = new LoginUser(payload.userId(), payload.account(), payload.phone(), payload.roles());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            jsonResponseWriter.write(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    ApiResponse.fail(ErrorCode.TOKEN_INVALID)
            );
        }
    }
}
