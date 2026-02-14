package com.yy.logistics.demo.controller;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import com.yy.logistics.demo.dto.DemoSubmitRequest;
import com.yy.logistics.demo.dto.DemoSubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "用于校验统一返回体/异常处理/参数校验的演示接口")
@SecurityRequirement(name = "bearerAuth")
public class DemoController {

    @Operation(summary = "连通性检查（请求参数校验示例）")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping(
            @RequestParam
            @NotBlank(message = "message不能为空")
            String message
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("echo", message);
        payload.put("serverTime", LocalDateTime.now().toString());
        return ApiResponse.success(payload);
    }

    @Operation(summary = "运费试算（方法参数校验示例）")
    @GetMapping("/quote")
    public ApiResponse<Map<String, Object>> quote(
            @RequestParam
            @DecimalMin(value = "0.1", message = "重量不能小于0.1kg")
            @DecimalMax(value = "100.0", message = "重量不能超过100kg")
            BigDecimal weightKg
    ) {
        BigDecimal totalFee = new BigDecimal("10.00").add(weightKg.multiply(new BigDecimal("2.50")));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("weightKg", weightKg);
        payload.put("estimatedFee", totalFee);
        return ApiResponse.success(payload);
    }

    @Operation(summary = "演示下单（请求体校验示例）")
    @PostMapping("/submit")
    public ApiResponse<DemoSubmitResponse> submit(@Valid @RequestBody DemoSubmitRequest request) {
        // 演示业务规则：重量超过50kg时主动抛出业务异常，走统一异常处理。
        if (request.weightKg().compareTo(new BigDecimal("50")) > 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "演示限制：单件重量不能超过50kg");
        }
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        DemoSubmitResponse response = new DemoSubmitResponse("OD" + timestamp, "WB" + timestamp, "CREATED");
        return ApiResponse.success("下单成功（演示）", response);
    }

    @Operation(summary = "主动抛出业务异常（测试全局异常处理）")
    @GetMapping("/biz-error")
    public ApiResponse<Void> bizError() {
        throw new BizException(ErrorCode.BUSINESS_ERROR, "这是一个演示业务异常");
    }
}
