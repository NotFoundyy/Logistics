package com.yy.logistics.auth.controller;

import com.yy.logistics.auth.dto.AuthCreateCourierRequest;
import com.yy.logistics.auth.dto.AuthCreateCourierResponse;
import com.yy.logistics.auth.dto.StationOptionResponse;
import com.yy.logistics.auth.service.AuthService;
import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.order.service.OrderService;
import com.yy.logistics.workbench.dto.AdminDashboardStatsResponse;
import com.yy.logistics.workbench.dto.AdminOrderItemResponse;
import com.yy.logistics.workbench.dto.WorkbenchOverviewResponse;
import com.yy.logistics.workbench.service.AdminWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "管理员权限接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AdminController {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AuthService authService;
    private final AdminWorkbenchService adminWorkbenchService;
    private final OrderService orderService;

    public AdminController(AuthService authService, AdminWorkbenchService adminWorkbenchService, OrderService orderService) {
        this.authService = authService;
        this.adminWorkbenchService = adminWorkbenchService;
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员权限检查")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "管理员权限验证通过");
        payload.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(payload);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员概览数据")
    @GetMapping("/overview")
    public ApiResponse<WorkbenchOverviewResponse> overview() {
        return ApiResponse.success(adminWorkbenchService.overview());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员订单列表")
    @GetMapping("/orders")
    public ApiResponse<PageResult<AdminOrderItemResponse>> orders(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页最小为1") @Max(value = 100, message = "每页最大为100") int size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(adminWorkbenchService.listOrders(page, size, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员查看订单详情")
    @GetMapping("/orders/{orderId:\\d+}")
    public ApiResponse<OrderDetailResponse> orderDetail(@PathVariable("orderId") Long orderId) {
        return ApiResponse.success(orderService.getOrderDetailById(orderId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "导出订单Excel")
    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) String status) {
        byte[] fileBytes = adminWorkbenchService.exportOrdersExcel(status);
        String fileName = "orders-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(fileBytes);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员同意退款")
    @PostMapping({"/orders/{orderId}/refund", "/orders/{orderId:\\d+}/refund", "/orders/refund/{orderId}"})
    public ApiResponse<Void> approveRefund(@PathVariable("orderId") Long orderId) {
        orderService.approveRefund(orderId);
        return ApiResponse.success("退款成功", null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员统计看板")
    @GetMapping("/stats")
    public ApiResponse<AdminDashboardStatsResponse> stats() {
        return ApiResponse.success(adminWorkbenchService.dashboardStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询可用网点")
    @GetMapping("/stations")
    public ApiResponse<List<StationOptionResponse>> stations() {
        return ApiResponse.success(authService.listStations());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员新增快递员")
    @PostMapping("/courier")
    public ApiResponse<AuthCreateCourierResponse> createCourier(@Valid @RequestBody AuthCreateCourierRequest request) {
        return ApiResponse.success("快递员创建成功", authService.createCourier(request));
    }
}
