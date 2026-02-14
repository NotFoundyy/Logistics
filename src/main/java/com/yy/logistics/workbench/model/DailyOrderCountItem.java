package com.yy.logistics.workbench.model;

import java.time.LocalDate;

public record DailyOrderCountItem(
        LocalDate day,
        Long total
) {
}
