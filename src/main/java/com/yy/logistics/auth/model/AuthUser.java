package com.yy.logistics.auth.model;

public record AuthUser(
        Long id,
        String username,
        String phone,
        String email,
        String passwordHash,
        Integer status
) {
}
