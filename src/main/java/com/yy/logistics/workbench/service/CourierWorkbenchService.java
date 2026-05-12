package com.yy.logistics.workbench.service;

import com.yy.logistics.auth.model.LoginUser;
import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.order.repository.OrderRepository;
import com.yy.logistics.order.service.TrackingService;
import com.yy.logistics.workbench.dto.CourierCancelOrderRequest;
import com.yy.logistics.workbench.dto.CourierCancelReviewRequest;
import com.yy.logistics.workbench.dto.CourierSignRequest;
import com.yy.logistics.workbench.dto.CourierTaskItemResponse;
import com.yy.logistics.workbench.dto.WorkbenchOverviewResponse;
import com.yy.logistics.workbench.model.CourierProfile;
import com.yy.logistics.workbench.model.CourierTaskItem;
import com.yy.logistics.workbench.model.TaskSnapshot;
import com.yy.logistics.workbench.repository.WorkbenchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CourierWorkbenchService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    private static final String STATUS_CANCEL_PENDING = "CANCEL_PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_SIGNED = "SIGNED";
    private static final String STATUS_DELIVERING = "DELIVERING";

    private static final String EVENT_SIGNED = "SIGNED";
    private static final String EVENT_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String EVENT_COD_PAID_CONFIRMED = "COD_PAID_CONFIRMED";

    private final WorkbenchRepository workbenchRepository;
    private final OrderRepository orderRepository;
    private final TrackingService trackingService;

    public CourierWorkbenchService(
            WorkbenchRepository workbenchRepository,
            OrderRepository orderRepository,
            TrackingService trackingService
    ) {
        this.workbenchRepository = workbenchRepository;
        this.orderRepository = orderRepository;
        this.trackingService = trackingService;
    }

    public WorkbenchOverviewResponse overview(LoginUser loginUser) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        trackingService.refreshAcceptedTasksByCourierId(courierProfile.courierId());

        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("pendingTasks", workbenchRepository.countCourierTasks(courierProfile.courierId(), STATUS_PENDING));
        metrics.put("acceptedTasks", workbenchRepository.countCourierTasks(courierProfile.courierId(), STATUS_ACCEPTED));
        metrics.put("finishedTasks", workbenchRepository.countCourierTasks(courierProfile.courierId(), STATUS_FINISHED));
        metrics.put("allTasks", workbenchRepository.countCourierTasks(courierProfile.courierId(), null));
        return new WorkbenchOverviewResponse(metrics);
    }

    public PageResult<CourierTaskItemResponse> listTasks(LoginUser loginUser, int page, int size, String status) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        trackingService.refreshAcceptedTasksByCourierId(courierProfile.courierId());

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;

        long total = workbenchRepository.countCourierTasks(courierProfile.courierId(), normalizedStatus);
        List<CourierTaskItemResponse> records = workbenchRepository.findCourierTasks(
                        courierProfile.courierId(), normalizedStatus, offset, safeSize
                ).stream()
                .map(this::toCourierTaskItemResponse)
                .toList();
        return new PageResult<>(safePage, safeSize, total, records);
    }

    public OrderDetailResponse getOrderDetail(LoginUser loginUser, Long orderId) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        if (!workbenchRepository.existsCourierOrderAccess(courierProfile.courierId(), orderId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "no permission to view this order");
        }
        return orderRepository.findOrderDetailById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(LoginUser loginUser, Long taskId) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        TaskSnapshot taskSnapshot = workbenchRepository.findTaskSnapshotById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND));

        String taskStatus = normalizeStatus(taskSnapshot.status());
        String orderStatus = normalizeStatus(taskSnapshot.orderStatus());

        if (!STATUS_PENDING.equals(taskStatus)) {
            if (STATUS_ACCEPTED.equals(taskStatus) && courierProfile.courierId().equals(taskSnapshot.courierId())) {
                return;
            }
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "current task cannot be accepted");
        }

        if (STATUS_CANCEL_PENDING.equals(orderStatus) || STATUS_CANCELLED.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order is in cancel flow, cannot accept");
        }

        int affected = workbenchRepository.acceptTask(taskId, courierProfile.courierId());
        if (affected <= 0) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "task has been accepted by another courier");
        }

        workbenchRepository.updateOrderStatusByWaybillNo(taskSnapshot.waybillNo(), "IN_TRANSIT");
        workbenchRepository.updateWaybillStatusByWaybillNo(taskSnapshot.waybillNo(), "IN_TRANSIT");

        LocalDateTime pickupTime = nextAvailableEventTime(taskSnapshot.waybillNo(), "PICKED_UP");
        workbenchRepository.insertTrackingEvent(
                taskSnapshot.waybillNo(),
                pickupTime,
                "PICKED_UP",
                courierProfile.stationId(),
                courierProfile.courierId(),
                "快递员已接单并开始运输"
        );

        LocalDateTime transitTime = nextAvailableEventTime(taskSnapshot.waybillNo(), "IN_TRANSIT");
        if (!transitTime.isAfter(pickupTime)) {
            transitTime = pickupTime.plusSeconds(1);
        }
        workbenchRepository.insertTrackingEvent(
                taskSnapshot.waybillNo(),
                transitTime,
                "IN_TRANSIT",
                courierProfile.stationId(),
                courierProfile.courierId(),
                "包裹已离开发件站点，正在运输中"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewCancelRequest(LoginUser loginUser, Long orderId, CourierCancelReviewRequest request) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        TaskSnapshot taskSnapshot = workbenchRepository.findAcceptedDeliveryTaskByOrderId(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_STATUS_INVALID, "no accepted delivery task for this order"));
        if (!courierProfile.courierId().equals(taskSnapshot.courierId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "only assigned courier can review cancellation");
        }

        if (!STATUS_CANCEL_PENDING.equals(normalizeStatus(taskSnapshot.orderStatus()))) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order is not in cancel pending status");
        }

        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "no additional comment";
        if (Boolean.TRUE.equals(request.approved())) {
            workbenchRepository.updateOrderStatus(orderId, STATUS_CANCELLED);
            workbenchRepository.updateOrderRemark(orderId, "courier approved cancellation, note: " + reason);
            workbenchRepository.updateWaybillStatusByOrderId(orderId, STATUS_CANCELLED);
            workbenchRepository.finishActiveTasksByWaybillNo(taskSnapshot.waybillNo());
            workbenchRepository.insertTrackingEvent(
                    taskSnapshot.waybillNo(),
                    LocalDateTime.now(),
                    STATUS_CANCELLED,
                    courierProfile.stationId(),
                    courierProfile.courierId(),
                    "courier approved cancellation and informed user. note: " + reason
            );
            return;
        }

        String resumeStatus = workbenchRepository.findWaybillByOrderId(orderId)
                .map(snapshot -> StringUtils.hasText(snapshot.currentStatus()) ? snapshot.currentStatus() : STATUS_IN_TRANSIT)
                .orElse(STATUS_IN_TRANSIT);
        workbenchRepository.updateOrderStatus(orderId, resumeStatus);
        workbenchRepository.updateOrderRemark(orderId, "courier rejected cancellation, note: " + reason);
        workbenchRepository.insertTrackingEvent(
                taskSnapshot.waybillNo(),
                LocalDateTime.now(),
                "CANCEL_REJECTED",
                courierProfile.stationId(),
                courierProfile.courierId(),
                "courier rejected cancellation. note: " + reason
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderByCourier(LoginUser loginUser, Long taskId, CourierCancelOrderRequest request) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        TaskSnapshot taskSnapshot = workbenchRepository.findTaskSnapshotById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND));

        String taskStatus = normalizeStatus(taskSnapshot.status());
        String orderStatus = normalizeStatus(taskSnapshot.orderStatus());

        if (!STATUS_ACCEPTED.equals(taskStatus)) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "only accepted task can cancel order");
        }
        if (!courierProfile.courierId().equals(taskSnapshot.courierId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "only assigned courier can cancel this order");
        }
        if (STATUS_SIGNED.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "signed order cannot be cancelled");
        }
        if (STATUS_CANCELLED.equals(orderStatus)) {
            return;
        }

        String reason = request.reason().trim();
        workbenchRepository.updateOrderStatus(taskSnapshot.orderId(), STATUS_CANCELLED);
        workbenchRepository.updateOrderRemark(taskSnapshot.orderId(), "courier cancelled order, reason: " + reason);
        workbenchRepository.updateWaybillStatusByOrderId(taskSnapshot.orderId(), STATUS_CANCELLED);
        workbenchRepository.finishActiveTasksByWaybillNo(taskSnapshot.waybillNo());
        workbenchRepository.insertTrackingEvent(
                taskSnapshot.waybillNo(),
                LocalDateTime.now(),
                "CANCELLED_BY_COURIER",
                courierProfile.stationId(),
                courierProfile.courierId(),
                "courier cancelled order and informed user, reason: " + reason
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void signOrder(LoginUser loginUser, Long taskId, CourierSignRequest request) {
        CourierProfile courierProfile = requireCourier(loginUser.userId());
        trackingService.refreshAcceptedTasksByCourierId(courierProfile.courierId());

        TaskSnapshot taskSnapshot = workbenchRepository.findTaskSnapshotById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND));

        String taskStatus = normalizeStatus(taskSnapshot.status());
        String orderStatus = normalizeStatus(taskSnapshot.orderStatus());

        if (!STATUS_ACCEPTED.equals(taskStatus)) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "only accepted task can sign");
        }
        if (!courierProfile.courierId().equals(taskSnapshot.courierId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "only assigned courier can sign this order");
        }
        if (STATUS_CANCELLED.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "cancelled order cannot be signed");
        }
        if (STATUS_CANCEL_PENDING.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "cancel review pending, cannot sign");
        }
        if (STATUS_SIGNED.equals(orderStatus)) {
            return;
        }
        if (!STATUS_DELIVERING.equals(orderStatus)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order is not in delivering status, cannot sign");
        }

        boolean paidConfirmed = Boolean.TRUE.equals(request.paidConfirmed());
        if (taskSnapshot.payType() != null && taskSnapshot.payType() == 1) {
            if (!workbenchRepository.existsTrackingEventByType(taskSnapshot.waybillNo(), EVENT_PAYMENT_CONFIRMED)) {
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "online order is not paid yet");
            }
        } else if (taskSnapshot.payType() != null && taskSnapshot.payType() == 2) {
            if (!paidConfirmed) {
                throw new BizException(ErrorCode.BAD_REQUEST, "COD order requires payment confirmation before sign");
            }
            if (!workbenchRepository.existsTrackingEventByType(taskSnapshot.waybillNo(), EVENT_COD_PAID_CONFIRMED)) {
                LocalDateTime paidEventTime = nextAvailableEventTime(taskSnapshot.waybillNo(), EVENT_COD_PAID_CONFIRMED);
                workbenchRepository.insertTrackingEvent(
                        taskSnapshot.waybillNo(),
                        paidEventTime,
                        EVENT_COD_PAID_CONFIRMED,
                        courierProfile.stationId(),
                        courierProfile.courierId(),
                        "courier confirmed COD payment"
                );
            }
        }

        if (!workbenchRepository.existsTrackingEventByType(taskSnapshot.waybillNo(), EVENT_SIGNED)) {
            LocalDateTime signTime = nextAvailableEventTime(taskSnapshot.waybillNo(), EVENT_SIGNED);
            String signDesc = StringUtils.hasText(request.remark())
                    ? "receiver signed, note: " + request.remark().trim()
                    : "receiver signed";
            workbenchRepository.insertTrackingEvent(
                    taskSnapshot.waybillNo(),
                    signTime,
                    EVENT_SIGNED,
                    courierProfile.stationId(),
                    courierProfile.courierId(),
                    signDesc
            );
        }

        workbenchRepository.updateOrderStatus(taskSnapshot.orderId(), STATUS_SIGNED);
        workbenchRepository.updateWaybillStatusByWaybillNo(taskSnapshot.waybillNo(), STATUS_SIGNED);
        workbenchRepository.updateTaskStatusToFinished(taskSnapshot.taskId());
    }

    private CourierProfile requireCourier(Long userId) {
        return workbenchRepository.findCourierByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_MATCH, "current account has no courier profile"));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime nextAvailableEventTime(String waybillNo, String eventType) {
        LocalDateTime eventTime = LocalDateTime.now();
        while (workbenchRepository.existsTrackingEvent(waybillNo, eventType, eventTime)) {
            eventTime = eventTime.plusSeconds(1);
        }
        return eventTime;
    }

    private CourierTaskItemResponse toCourierTaskItemResponse(CourierTaskItem item) {
        return new CourierTaskItemResponse(
                item.taskId(),
                item.waybillNo(),
                item.orderId(),
                item.orderNo(),
                item.orderStatus(),
                item.refunded(),
                item.receiverName(),
                item.receiverPhone(),
                item.receiverAddr(),
                item.payType(),
                item.paid(),
                item.taskType(),
                item.taskStatus(),
                item.plannedTime(),
                item.acceptedAt()
        );
    }
}
