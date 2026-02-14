package com.yy.logistics.workbench.service;

import com.yy.logistics.common.api.PageResult;
import com.yy.logistics.workbench.dto.AdminDashboardStatsResponse;
import com.yy.logistics.workbench.dto.AdminOrderItemResponse;
import com.yy.logistics.workbench.dto.AdminOrderTrendItemResponse;
import com.yy.logistics.workbench.dto.WorkbenchOverviewResponse;
import com.yy.logistics.workbench.model.AdminOrderItem;
import com.yy.logistics.workbench.repository.WorkbenchRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminWorkbenchService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkbenchRepository workbenchRepository;

    public AdminWorkbenchService(WorkbenchRepository workbenchRepository) {
        this.workbenchRepository = workbenchRepository;
    }

    public WorkbenchOverviewResponse overview() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("totalOrders", workbenchRepository.countAllOrders());
        metrics.put("todayOrders", workbenchRepository.countTodayOrders());
        metrics.put("createdOrders", workbenchRepository.countOrdersByStatus("CREATED"));
        metrics.put("cancelPendingOrders", workbenchRepository.countOrdersByStatus("CANCEL_PENDING"));
        metrics.put("inTransitOrders", workbenchRepository.countOrdersByStatus("IN_TRANSIT"));
        metrics.put("deliveringOrders", workbenchRepository.countOrdersByStatus("DELIVERING"));
        metrics.put("signedOrders", workbenchRepository.countOrdersByStatus("SIGNED"));
        metrics.put("cancelledOrders", workbenchRepository.countOrdersByStatus("CANCELLED"));
        metrics.put("refundedOrders", workbenchRepository.countRefundedOrders());
        metrics.put("pendingTasks", workbenchRepository.countTasksByStatus("PENDING"));
        metrics.put("acceptedTasks", workbenchRepository.countTasksByStatus("ACCEPTED"));
        metrics.put("finishedTasks", workbenchRepository.countTasksByStatus("FINISHED"));
        return new WorkbenchOverviewResponse(metrics);
    }

    public AdminDashboardStatsResponse dashboardStats() {
        Map<String, Long> orderStatusStats = new LinkedHashMap<>();
        workbenchRepository.countOrderStatusGroup().forEach(item -> orderStatusStats.put(item.status(), item.total()));

        Map<String, Long> taskStatusStats = new LinkedHashMap<>();
        workbenchRepository.countTaskStatusGroup().forEach(item -> taskStatusStats.put(item.status(), item.total()));

        Map<LocalDate, Long> trendMap = new LinkedHashMap<>();
        workbenchRepository.countRecentDailyOrder(7).forEach(item -> trendMap.put(item.day(), item.total()));

        List<AdminOrderTrendItemResponse> trend = buildLast7Days().stream()
                .map(date -> new AdminOrderTrendItemResponse(date.toString(), trendMap.getOrDefault(date, 0L)))
                .toList();

        return new AdminDashboardStatsResponse(orderStatusStats, taskStatusStats, trend);
    }

    public PageResult<AdminOrderItemResponse> listOrders(int page, int size, String status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String normalizedStatus = normalizeStatus(status);

        long total = workbenchRepository.countAdminOrders(normalizedStatus);
        List<AdminOrderItemResponse> records = workbenchRepository.findAdminOrders(normalizedStatus, offset, safeSize)
                .stream()
                .map(this::toAdminOrderItemResponse)
                .toList();
        return new PageResult<>(safePage, safeSize, total, records);
    }

    public byte[] exportOrdersExcel(String status) {
        String normalizedStatus = normalizeStatus(status);
        List<AdminOrderItem> rows = workbenchRepository.findAdminOrdersForExport(normalizedStatus);

        StringBuilder csv = new StringBuilder(2048);
        csv.append("订单ID,订单号,运单号,订单状态,退款状态,寄件人,寄件手机,收件人,收件手机,金额,创建时间\n");
        for (AdminOrderItem item : rows) {
            csv.append(csvCell(item.id())).append(',')
                    .append(csvCell(item.orderNo())).append(',')
                    .append(csvCell(item.waybillNo())).append(',')
                    .append(csvCell(item.status())).append(',')
                    .append(csvCell(Boolean.TRUE.equals(item.refunded()) ? "已退款" : "未退款")).append(',')
                    .append(csvCell(item.senderName())).append(',')
                    .append(csvCell(item.senderPhone())).append(',')
                    .append(csvCell(item.receiverName())).append(',')
                    .append(csvCell(item.receiverPhone())).append(',')
                    .append(csvCell(item.feeTotal())).append(',')
                    .append(csvCell(item.createdAt() == null ? "" : item.createdAt().format(DATE_TIME_FORMATTER)))
                    .append('\n');
        }
        // UTF-8 BOM，避免 Windows Excel 打开中文乱码。
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }

    private List<LocalDate> buildLast7Days() {
        LocalDate today = LocalDate.now();
        return List.of(
                today.minusDays(6),
                today.minusDays(5),
                today.minusDays(4),
                today.minusDays(3),
                today.minusDays(2),
                today.minusDays(1),
                today
        );
    }

    private AdminOrderItemResponse toAdminOrderItemResponse(AdminOrderItem item) {
        return new AdminOrderItemResponse(
                item.id(),
                item.orderNo(),
                item.waybillNo(),
                item.status(),
                item.refunded(),
                item.senderName(),
                item.senderPhone(),
                item.receiverName(),
                item.receiverPhone(),
                item.feeTotal(),
                item.createdAt()
        );
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
