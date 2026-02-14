package com.yy.logistics.workbench.controller;

import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.auth.support.AuthHelper;
import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.workbench.dto.CourierCancelOrderRequest;
import com.yy.logistics.workbench.dto.CourierCancelReviewRequest;
import com.yy.logistics.workbench.dto.CourierSignRequest;
import com.yy.logistics.workbench.dto.CourierTaskItemResponse;
import com.yy.logistics.workbench.dto.WorkbenchOverviewResponse;
import com.yy.logistics.workbench.service.CourierWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/courier")
@Tag(name = "Courier", description = "快递员工作台接口")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('COURIER')")
public class CourierWorkbenchController {

    private final CourierWorkbenchService courierWorkbenchService;

    public CourierWorkbenchController(CourierWorkbenchService courierWorkbenchService) {
        this.courierWorkbenchService = courierWorkbenchService;
    }

    @Operation(summary = "快递员概览数据")
    @GetMapping("/overview")
    public ApiResponse<WorkbenchOverviewResponse> overview(Authentication authentication) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(courierWorkbenchService.overview(loginUser));
    }

    @Operation(summary = "快递员任务列表")
    @GetMapping("/tasks")
    public ApiResponse<PageResult<CourierTaskItemResponse>> tasks(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页最小为1") @Max(value = 100, message = "每页最大为100") int size,
            @RequestParam(required = false) String status
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(courierWorkbenchService.listTasks(loginUser, page, size, status));
    }

    @Operation(summary = "快递员接单")
    @PostMapping("/tasks/{taskId}/accept")
    public ApiResponse<Void> accept(Authentication authentication, @PathVariable("taskId") Long taskId) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        courierWorkbenchService.acceptTask(loginUser, taskId);
        return ApiResponse.success("接单成功", null);
    }

    @Operation(summary = "快递员查看订单详情")
    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderDetailResponse> orderDetail(
            Authentication authentication,
            @PathVariable("orderId") Long orderId
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(courierWorkbenchService.getOrderDetail(loginUser, orderId));
    }

    @Operation(summary = "快递员审核用户取消申请")
    @PostMapping("/orders/{orderId}/cancel-review")
    public ApiResponse<Void> reviewCancel(
            Authentication authentication,
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody CourierCancelReviewRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        courierWorkbenchService.reviewCancelRequest(loginUser, orderId, request);
        return ApiResponse.success("审核完成", null);
    }

    @Operation(summary = "快递员主动取消订单并通知用户")
    @PostMapping("/tasks/{taskId}/cancel-order")
    public ApiResponse<Void> cancelOrder(
            Authentication authentication,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody CourierCancelOrderRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        courierWorkbenchService.cancelOrderByCourier(loginUser, taskId, request);
        return ApiResponse.success("订单已取消并通知用户", null);
    }

    @Operation(summary = "快递员确认签收")
    @PostMapping("/tasks/{taskId}/sign")
    public ApiResponse<Void> signOrder(
            Authentication authentication,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody CourierSignRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        courierWorkbenchService.signOrder(loginUser, taskId, request);
        return ApiResponse.success("签收完成", null);
    }
}
