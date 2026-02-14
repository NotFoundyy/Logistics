package com.yy.logistics.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.yy.logistics.auth.model.JwtPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final String secret;
    private final String issuer;
    private final long expireSeconds;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenService(
            @Value("${security.jwt.secret:logistics-default-secret-change-in-production}") String secret,
            @Value("${security.jwt.issuer:logistics-api}") String issuer,
            @Value("${security.jwt.expire-seconds:86400}") long expireSeconds
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.expireSeconds = expireSeconds;
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String createToken(Long userId, String account, String phone, List<String> roles) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(userId))
                .withClaim("account", account)
                .withClaim("phone", phone)
                .withClaim("roles", roles)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expireSeconds)))
                .sign(algorithm);
    }

    public JwtPayload parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JWTVerificationException("Token为空");
        }

        DecodedJWT jwt = verifier.verify(token);
        String subject = jwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new JWTVerificationException("Token缺少主体信息");
        }

        Long userId = Long.parseLong(subject);
        String account = jwt.getClaim("account").asString();
        String phone = jwt.getClaim("phone").asString();
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        if (roles == null) {
            roles = List.of();
        }
        return new JwtPayload(userId, account, phone, roles);
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public String getSecret() {
        return secret;
    }
}
