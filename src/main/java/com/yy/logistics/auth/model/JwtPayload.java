package com.yy.logistics.auth.model;

import java.util.List;

public record JwtPayload(
        Long userId,
        String account,
        String phone,
        List<String> roles
) {
}
