package com.yy.logistics.order.model;

public record OrderSnapshot(
        Long id,
        Long userId,
        String status
) {
}
