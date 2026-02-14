package com.yy.logistics.auth.model;

public record AuthAddress(
        Long id,
        Long userId,
        String contactName,
        String contactPhone,
        String province,
        String city,
        String district,
        String detail,
        Integer isDefault
) {
}
