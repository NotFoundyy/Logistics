package com.yy.logistics.order.controller;

import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.auth.support.AuthHelper;
import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.order.dto.OrderCancelRequest;
import com.yy.logistics.order.dto.OrderCreateRequest;
import com.yy.logistics.order.dto.OrderCreateResponse;
import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.order.dto.OrderListItemResponse;
import com.yy.logistics.order.dto.OrderPaymentQrcodeResponse;
import com.yy.logistics.order.dto.OrderReceiverUpdateRequest;
import com.yy.logistics.order.dto.OrderReceiverUpdateResponse;
import com.yy.logistics.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "订单接口")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResponse<OrderCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success("下单成功", orderService.createOrder(loginUser, request));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public ApiResponse<PageResult<OrderListItemResponse>> myOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页最小为1") @Max(value = 100, message = "每页最大为100") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "all") String relation
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(orderService.listMyOrders(loginUser, page, size, status, relation));
    }

    @Operation(summary = "我的订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> detail(
            Authentication authentication,
            @PathVariable("id") Long id
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(orderService.getMyOrderDetail(loginUser, id));
    }

    @Operation(summary = "在线支付-获取二维码")
    @GetMapping("/{id}/payment/qrcode")
    public ApiResponse<OrderPaymentQrcodeResponse> paymentQrcode(
            Authentication authentication,
            @PathVariable("id") Long id
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success(orderService.getPaymentQrcode(loginUser, id));
    }

    @Operation(summary = "在线支付-确认支付成功（模拟）")
    @PostMapping("/{id}/payment/confirm")
    public ApiResponse<Void> confirmPayment(
            Authentication authentication,
            @PathVariable("id") Long id
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        orderService.confirmOnlinePayment(loginUser, id);
        return ApiResponse.success("支付成功", null);
    }

    @Operation(summary = "发起取消订单")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody OrderCancelRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        orderService.cancelMyOrder(loginUser, id, request);
        return ApiResponse.success("取消申请已提交", null);
    }

    @Operation(summary = "修改收件信息并重计费")
    @PutMapping("/{id}/receiver")
    public ApiResponse<OrderReceiverUpdateResponse> updateReceiver(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody OrderReceiverUpdateRequest request
    ) {
        LoginUser loginUser = AuthHelper.requireLoginUser(authentication);
        return ApiResponse.success("收件信息已更新", orderService.updateMyReceiver(loginUser, id, request));
    }
}
