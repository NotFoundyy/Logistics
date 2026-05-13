package com.yy.logistics.order.service;

import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import com.yy.logistics.order.dto.OrderCancelRequest;
import com.yy.logistics.order.dto.OrderCreateRequest;
import com.yy.logistics.order.dto.OrderCreateResponse;
import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.order.dto.OrderListItemResponse;
import com.yy.logistics.order.dto.OrderPaymentQrcodeResponse;
import com.yy.logistics.order.dto.OrderReceiverUpdateRequest;
import com.yy.logistics.order.dto.OrderReceiverUpdateResponse;
import com.yy.logistics.order.dto.PricingQuoteRequest;
import com.yy.logistics.order.dto.PricingQuoteResponse;
import com.yy.logistics.order.model.AcceptedTaskSnapshot;
import com.yy.logistics.order.model.OrderPaymentSnapshot;
import com.yy.logistics.order.model.OrderPricingSnapshot;
import com.yy.logistics.order.model.OrderSnapshot;
import com.yy.logistics.order.model.WaybillSnapshot;
import com.yy.logistics.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final int DISPATCH_TASK_TYPE = 2;

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    private static final String STATUS_DELIVERING = "DELIVERING";
    private static final String STATUS_SIGNED = "SIGNED";
    private static final String STATUS_CANCEL_PENDING = "CANCEL_PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String EVENT_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String EVENT_COD_PAID_CONFIRMED = "COD_PAID_CONFIRMED";
    private static final String EVENT_REFUND_SUCCESS = "REFUND_SUCCESS";

    private static final String TASK_STATUS_ACCEPTED = "ACCEPTED";
    private static final String TASK_STATUS_PENDING = "PENDING";

    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final TrackingService trackingService;

    public OrderService(OrderRepository orderRepository, PricingService pricingService, TrackingService trackingService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.trackingService = trackingService;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(LoginUser loginUser, OrderCreateRequest request) {
        PricingQuoteResponse quote = pricingService.quoteForOrder(request);
        String orderNo = generateUniqueNo("OD", orderRepository::existsOrderNo);
        String waybillNo = generateUniqueNo("WB", orderRepository::existsWaybillNo);

        Long orderId = orderRepository.insertOrder(
                orderNo,
                loginUser.userId(),
                request.senderName(),
                request.senderPhone(),
                request.senderAddr(),
                request.receiverName(),
                request.receiverPhone(),
                request.receiverAddr(),
                request.serviceType(),
                STATUS_CREATED,
                quote.totalFee(),
                request.remark()
        );

        orderRepository.insertWaybill(
                waybillNo,
                orderId,
                request.weight(),
                request.volume(),
                quote.chargeWeight(),
                quote.totalFee(),
                request.payType(),
                request.insuredAmount(),
                STATUS_CREATED
        );

        orderRepository.insertTrackingEvent(
                waybillNo,
                LocalDateTime.now(),
                STATUS_CREATED,
                null,
                null,
                "订单已创建，等待快递员接单"
        );

        if (!orderRepository.existsActiveTask(waybillNo, DISPATCH_TASK_TYPE)) {
            Long stationId = orderRepository.findDefaultStationId()
                    .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "no available station configured"));
            orderRepository.insertTask(
                    waybillNo,
                    DISPATCH_TASK_TYPE,
                    stationId,
                    TASK_STATUS_PENDING,
                    LocalDateTime.now().plusMinutes(20)
            );
        }

        return new OrderCreateResponse(orderId, orderNo, waybillNo, STATUS_CREATED, quote.totalFee(), request.payType(), false);
    }

    public PageResult<OrderListItemResponse> listMyOrders(LoginUser loginUser, int page, int size, String status, String relation) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        String normalizedRelation = normalizeRelation(relation);
        String receiverPhone = StringUtils.hasText(loginUser.phone()) ? loginUser.phone().trim() : "__NO_PHONE__";

        long total = orderRepository.countOrdersByRelation(loginUser.userId(), receiverPhone, normalizedStatus, normalizedRelation);
        List<OrderListItemResponse> records = orderRepository.findOrdersByRelation(
                loginUser.userId(),
                receiverPhone,
                normalizedStatus,
                normalizedRelation,
                offset,
                safeSize
        );
        records.stream()
                .map(OrderListItemResponse::waybillNo)
                .filter(StringUtils::hasText)
                .forEach(trackingService::refreshAutoProgress);
        records = orderRepository.findOrdersByRelation(
                loginUser.userId(),
                receiverPhone,
                normalizedStatus,
                normalizedRelation,
                offset,
                safeSize
        );
        return new PageResult<>(safePage, safeSize, total, records);
    }

    public OrderDetailResponse getMyOrderDetail(LoginUser loginUser, Long orderId) {
        String receiverPhone = StringUtils.hasText(loginUser.phone()) ? loginUser.phone().trim() : "__NO_PHONE__";
        return orderRepository.findOrderDetailByUserOrReceiver(loginUser.userId(), receiverPhone, orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderDetailResponse getOrderDetailById(Long orderId) {
        return orderRepository.findOrderDetailById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderPaymentQrcodeResponse getPaymentQrcode(LoginUser loginUser, Long orderId) {
        OrderPaymentSnapshot snapshot = requireMyOrderPaymentSnapshot(loginUser, orderId);
        if (snapshot.payType() == null || snapshot.payType() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "COD order does not need online QR payment");
        }
        if (STATUS_CANCELLED.equals(snapshot.orderStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order already cancelled, cannot pay");
        }
        if (orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_PAYMENT_CONFIRMED)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order already paid");
        }

        String qrCodeText = "LOGISTICS_PAY|"
                + snapshot.orderNo() + "|"
                + snapshot.waybillNo() + "|"
                + snapshot.feeTotal() + "|"
                + System.currentTimeMillis();

        return new OrderPaymentQrcodeResponse(
                snapshot.orderId(),
                snapshot.orderNo(),
                snapshot.waybillNo(),
                snapshot.feeTotal(),
                qrCodeText,
                300
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmOnlinePayment(LoginUser loginUser, Long orderId) {
        OrderPaymentSnapshot snapshot = requireMyOrderPaymentSnapshot(loginUser, orderId);
        if (snapshot.payType() == null || snapshot.payType() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "only online pay orders can confirm online payment");
        }
        if (STATUS_CANCELLED.equals(snapshot.orderStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order already cancelled, cannot pay");
        }

        if (orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_PAYMENT_CONFIRMED)) {
            return;
        }

        orderRepository.insertTrackingEvent(
                snapshot.waybillNo(),
                LocalDateTime.now(),
                EVENT_PAYMENT_CONFIRMED,
                null,
                null,
                "在线支付已完成，金额：" + snapshot.feeTotal()
        );

        String paymentRemark = "online payment success, amount: " + snapshot.feeTotal();
        orderRepository.updateOrderRemark(snapshot.orderId(), appendRemark(snapshot.remark(), paymentRemark));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelMyOrder(LoginUser loginUser, Long orderId, OrderCancelRequest request) {
        OrderSnapshot snapshot = orderRepository.findOrderSnapshotById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
        if (!snapshot.userId().equals(loginUser.userId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        String orderStatus = normalizeStatus(snapshot.status());
        if (STATUS_CANCELLED.equals(orderStatus)) {
            return;
        }
        if (STATUS_SIGNED.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "signed order cannot be cancelled");
        }
        if (STATUS_CANCEL_PENDING.equals(orderStatus)) {
            return;
        }

        WaybillSnapshot waybillSnapshot = orderRepository.findWaybillByOrderId(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.WAYBILL_NOT_FOUND));
        String reason = request.reason().trim();

        if (STATUS_CREATED.equals(orderStatus)) {
            String finalRemark = "user cancelled order, reason: " + reason;
            orderRepository.cancelOrder(orderId, finalRemark);
            orderRepository.updateWaybillStatusByOrderId(orderId, STATUS_CANCELLED);
            orderRepository.finishActiveTasksByWaybillNo(waybillSnapshot.waybillNo());
            orderRepository.insertTrackingEvent(
                    waybillSnapshot.waybillNo(),
                    LocalDateTime.now(),
                    STATUS_CANCELLED,
                    null,
                    null,
                    "user cancelled order at created phase, reason: " + reason
            );
            return;
        }

        if (!STATUS_IN_TRANSIT.equals(orderStatus) && !STATUS_DELIVERING.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "current status does not allow cancel");
        }

        AcceptedTaskSnapshot taskSnapshot = orderRepository.findActiveAcceptedTaskByWaybillNo(waybillSnapshot.waybillNo())
                .orElse(null);
        if (taskSnapshot == null) {
            String finalRemark = "user cancelled order, reason: " + reason;
            orderRepository.cancelOrder(orderId, finalRemark);
            orderRepository.updateWaybillStatusByOrderId(orderId, STATUS_CANCELLED);
            orderRepository.finishActiveTasksByWaybillNo(waybillSnapshot.waybillNo());
            orderRepository.insertTrackingEvent(
                    waybillSnapshot.waybillNo(),
                    LocalDateTime.now(),
                    STATUS_CANCELLED,
                    null,
                    null,
                    "user cancelled order and system auto approved, reason: " + reason
            );
            return;
        }
        if (!TASK_STATUS_ACCEPTED.equals(normalizeStatus(taskSnapshot.taskStatus()))) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "task status does not allow cancel");
        }

        orderRepository.updateOrderStatus(orderId, STATUS_CANCEL_PENDING, "user requested cancellation, reason: " + reason);
        orderRepository.insertTrackingEvent(
                waybillSnapshot.waybillNo(),
                LocalDateTime.now(),
                "CANCEL_REQUESTED",
                null,
                taskSnapshot.courierId(),
                "user requested cancellation, waiting courier review. reason: " + reason
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderReceiverUpdateResponse updateMyReceiver(LoginUser loginUser, Long orderId, OrderReceiverUpdateRequest request) {
        OrderPricingSnapshot snapshot = orderRepository.findOrderPricingSnapshotById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
        if (!snapshot.userId().equals(loginUser.userId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (STATUS_CANCELLED.equals(snapshot.orderStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "cancelled order cannot update receiver");
        }
        if (STATUS_SIGNED.equals(snapshot.orderStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "signed order cannot update receiver");
        }
        if (STATUS_CANCEL_PENDING.equals(snapshot.orderStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "cancel review pending, cannot update receiver");
        }

        PricingQuoteResponse quote = pricingService.quote(new PricingQuoteRequest(
                snapshot.senderAddr(),
                request.receiverAddr(),
                snapshot.weight(),
                snapshot.volume(),
                snapshot.serviceType(),
                snapshot.insuredAmount()
        ));

        BigDecimal oldFee = snapshot.feeTotal();
        BigDecimal newFee = quote.totalFee();
        BigDecimal delta = newFee.subtract(oldFee);

        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "user updates receiver address";
        String remark = reason + ", recalculated fee: " + oldFee + " -> " + newFee;

        orderRepository.updateOrderReceiverAndFee(
                orderId,
                request.receiverName().trim(),
                request.receiverPhone().trim(),
                request.receiverAddr().trim(),
                newFee,
                remark
        );
        orderRepository.updateWaybillPricingByOrderId(orderId, quote.chargeWeight(), newFee);

        orderRepository.insertTrackingEvent(
                snapshot.waybillNo(),
                LocalDateTime.now(),
                "RECEIVER_UPDATED",
                null,
                null,
                "收件信息已修改，费用已重新计算：" + newFee + "（差额：" + delta + "）"
        );

        return new OrderReceiverUpdateResponse(snapshot.orderNo(), snapshot.waybillNo(), oldFee, newFee, delta);
    }

    private String generateUniqueNo(String prefix, Predicate<String> existsChecker) {
        for (int i = 0; i < 20; i++) {
            String candidate = prefix
                    + LocalDateTime.now().format(NO_TIME_FORMATTER)
                    + String.format("%03d", (int) (Math.random() * 1000));
            if (!existsChecker.test(candidate)) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "failed to generate unique code");
    }

    private String normalizeRelation(String relation) {
        if (!StringUtils.hasText(relation)) {
            return "all";
        }
        String normalized = relation.trim().toLowerCase(Locale.ROOT);
        if ("sender".equals(normalized) || "receiver".equals(normalized) || "all".equals(normalized)) {
            return normalized;
        }
        return "all";
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private OrderPaymentSnapshot requireMyOrderPaymentSnapshot(LoginUser loginUser, Long orderId) {
        OrderPaymentSnapshot snapshot = orderRepository.findOrderPaymentSnapshotById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
        if (!snapshot.userId().equals(loginUser.userId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return snapshot;
    }

    private String appendRemark(String currentRemark, String appendText) {
        if (!StringUtils.hasText(currentRemark)) {
            return appendText;
        }
        return currentRemark + "; " + appendText;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        OrderPaymentSnapshot snapshot = orderRepository.findOrderPaymentSnapshotById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));

        if (!STATUS_CANCELLED.equals(normalizeStatus(snapshot.orderStatus()))) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "仅已取消订单可退款");
        }

        boolean paid = orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_PAYMENT_CONFIRMED)
                || orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_COD_PAID_CONFIRMED);
        if (!paid) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该订单未支付，无需退款");
        }

        if (orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_REFUND_SUCCESS)) {
            return;
        }

        try {
            LocalDateTime refundEventTime = nextAvailableEventTime(snapshot.waybillNo(), EVENT_REFUND_SUCCESS);
            orderRepository.insertTrackingEvent(
                    snapshot.waybillNo(),
                    refundEventTime,
                    EVENT_REFUND_SUCCESS,
                    null,
                    null,
                    "admin approved refund, amount: " + snapshot.feeTotal()
            );
        } catch (DataIntegrityViolationException ex) {
            // 幂等处理：并发点击导致重复插入时，若已存在退款事件则视为成功。
            if (orderRepository.existsTrackingEventByType(snapshot.waybillNo(), EVENT_REFUND_SUCCESS)) {
                return;
            }
            log.error("退款写入失败(orderId={})", orderId, ex);
            throw new BizException(ErrorCode.BUSINESS_ERROR, "退款处理失败，请稍后重试");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("退款处理异常(orderId={})", orderId, ex);
            throw new BizException(ErrorCode.BUSINESS_ERROR, "退款处理失败，请稍后重试");
        }
    }

    private LocalDateTime nextAvailableEventTime(String waybillNo, String eventType) {
        LocalDateTime eventTime = LocalDateTime.now().withNano(0);
        while (orderRepository.existsTrackingEvent(waybillNo, eventType, eventTime)) {
            eventTime = eventTime.plusSeconds(1);
        }
        return eventTime;
    }
}
