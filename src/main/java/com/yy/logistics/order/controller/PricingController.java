package com.yy.logistics.order.controller;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.order.dto.PricingQuoteRequest;
import com.yy.logistics.order.dto.PricingQuoteResponse;
import com.yy.logistics.order.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/pricing")
@Tag(name = "Pricing", description = "运费试算接口")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Operation(summary = "运费试算")
    @GetMapping("/quote")
    public ApiResponse<PricingQuoteResponse> quote(@Valid @ModelAttribute PricingQuoteRequest request) {
        return ApiResponse.success(pricingService.quote(request));
    }
}
