package com.yy.logistics.workbench.repository;

import com.yy.logistics.order.model.OrderSnapshot;
import com.yy.logistics.order.model.WaybillSnapshot;
import com.yy.logistics.workbench.model.AdminOrderItem;
import com.yy.logistics.workbench.model.CourierProfile;
import com.yy.logistics.workbench.model.CourierTaskItem;
import com.yy.logistics.workbench.model.DailyOrderCountItem;
import com.yy.logistics.workbench.model.StatusCountItem;
import com.yy.logistics.workbench.model.TaskSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkbenchRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkbenchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countAllOrders() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_order", Long.class);
        return count == null ? 0L : count;
    }

    public long countOrdersByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_order WHERE status = ?",
                Long.class,
                status
        );
        return count == null ? 0L : count;
    }

    public long countTodayOrders() {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_order
                        WHERE DATE(created_at) = CURRENT_DATE
                        """,
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countAllTasks() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_task", Long.class);
        return count == null ? 0L : count;
    }

    public long countTasksByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_task WHERE status = ?",
                Long.class,
                status
        );
        return count == null ? 0L : count;
    }

    public List<StatusCountItem> countOrderStatusGroup() {
        return jdbcTemplate.query("""
                        SELECT status, COUNT(1) AS total
                        FROM t_order
                        GROUP BY status
                        ORDER BY total DESC
                        """,
                (rs, rowNum) -> new StatusCountItem(
                        rs.getString("status"),
                        rs.getLong("total")
                )
        );
    }

    public List<StatusCountItem> countTaskStatusGroup() {
        return jdbcTemplate.query("""
                        SELECT status, COUNT(1) AS total
                        FROM t_task
                        GROUP BY status
                        ORDER BY total DESC
                        """,
                (rs, rowNum) -> new StatusCountItem(
                        rs.getString("status"),
                        rs.getLong("total")
                )
        );
    }

    public List<DailyOrderCountItem> countRecentDailyOrder(int days) {
        int safeDays = Math.max(days, 1);
        int offsetDays = Math.max(safeDays - 1, 0);
        return jdbcTemplate.query("""
                        SELECT DATE(created_at) AS day_key, COUNT(1) AS total
                        FROM t_order
                        WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY)
                        GROUP BY DATE(created_at)
                        ORDER BY day_key ASC
                        """,
                (rs, rowNum) -> new DailyOrderCountItem(
                        rs.getObject("day_key", LocalDate.class),
                        rs.getLong("total")
                ),
                offsetDays
        );
    }

    public long countAdminOrders(String status) {
        if (StringUtils.hasText(status)) {
            if ("PROCESSING".equalsIgnoreCase(status)) {
                Long processingCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM t_order WHERE status IN ('CREATED', 'IN_TRANSIT', 'DELIVERING')",
                        Long.class
                );
                return processingCount == null ? 0L : processingCount;
            }
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM t_order WHERE status = ?",
                    Long.class,
                    status
            );
            return count == null ? 0L : count;
        }
        return countAllOrders();
    }

    public List<AdminOrderItem> findAdminOrders(String status, int offset, int size) {
        if (StringUtils.hasText(status)) {
            if ("PROCESSING".equalsIgnoreCase(status)) {
                return jdbcTemplate.query("""
                                SELECT o.id,
                                       o.order_no,
                                       w.waybill_no,
                                       o.status,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                       o.sender_name,
                                       o.sender_phone,
                                       o.receiver_name,
                                       o.receiver_phone,
                                       o.fee_total,
                                       o.created_at
                                FROM t_order o
                                LEFT JOIN t_waybill w ON w.order_id = o.id
                                WHERE o.status IN ('CREATED', 'IN_TRANSIT', 'DELIVERING')
                                ORDER BY o.created_at DESC, o.id DESC
                                LIMIT ? OFFSET ?
                                """,
                        (rs, rowNum) -> new AdminOrderItem(
                                rs.getLong("id"),
                                rs.getString("order_no"),
                                rs.getString("waybill_no"),
                                rs.getString("status"),
                                rs.getBoolean("refunded"),
                                rs.getString("sender_name"),
                                rs.getString("sender_phone"),
                                rs.getString("receiver_name"),
                                rs.getString("receiver_phone"),
                                rs.getBigDecimal("fee_total"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ),
                        size,
                        offset
                );
            }
            return jdbcTemplate.query("""
                            SELECT o.id,
                                   o.order_no,
                                   w.waybill_no,
                                   o.status,
                                   EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.sender_name,
                                   o.sender_phone,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.fee_total,
                                   o.created_at
                            FROM t_order o
                            LEFT JOIN t_waybill w ON w.order_id = o.id
                            WHERE o.status = ?
                            ORDER BY o.created_at DESC, o.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new AdminOrderItem(
                            rs.getLong("id"),
                            rs.getString("order_no"),
                            rs.getString("waybill_no"),
                            rs.getString("status"),
                            rs.getBoolean("refunded"),
                            rs.getString("sender_name"),
                            rs.getString("sender_phone"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getBigDecimal("fee_total"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    status,
                    size,
                    offset
            );
        }

        return jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               o.status,
                               EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               o.sender_name,
                               o.sender_phone,
                               o.receiver_name,
                               o.receiver_phone,
                               o.fee_total,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        ORDER BY o.created_at DESC, o.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new AdminOrderItem(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getBoolean("refunded"),
                        rs.getString("sender_name"),
                        rs.getString("sender_phone"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getBigDecimal("fee_total"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                size,
                offset
        );
    }

    public List<AdminOrderItem> findAdminOrdersForExport(String status) {
        if (StringUtils.hasText(status)) {
            if ("PROCESSING".equalsIgnoreCase(status)) {
                return jdbcTemplate.query("""
                                SELECT o.id,
                                       o.order_no,
                                       w.waybill_no,
                                       o.status,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                       o.sender_name,
                                       o.sender_phone,
                                       o.receiver_name,
                                       o.receiver_phone,
                                       o.fee_total,
                                       o.created_at
                                FROM t_order o
                                LEFT JOIN t_waybill w ON w.order_id = o.id
                                WHERE o.status IN ('CREATED', 'IN_TRANSIT', 'DELIVERING')
                                ORDER BY o.created_at DESC, o.id DESC
                                """,
                        (rs, rowNum) -> new AdminOrderItem(
                                rs.getLong("id"),
                                rs.getString("order_no"),
                                rs.getString("waybill_no"),
                                rs.getString("status"),
                                rs.getBoolean("refunded"),
                                rs.getString("sender_name"),
                                rs.getString("sender_phone"),
                                rs.getString("receiver_name"),
                                rs.getString("receiver_phone"),
                                rs.getBigDecimal("fee_total"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        )
                );
            }
            return jdbcTemplate.query("""
                            SELECT o.id,
                                   o.order_no,
                                   w.waybill_no,
                                   o.status,
                                   EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.sender_name,
                                   o.sender_phone,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.fee_total,
                                   o.created_at
                            FROM t_order o
                            LEFT JOIN t_waybill w ON w.order_id = o.id
                            WHERE o.status = ?
                            ORDER BY o.created_at DESC, o.id DESC
                            """,
                    (rs, rowNum) -> new AdminOrderItem(
                            rs.getLong("id"),
                            rs.getString("order_no"),
                            rs.getString("waybill_no"),
                            rs.getString("status"),
                            rs.getBoolean("refunded"),
                            rs.getString("sender_name"),
                            rs.getString("sender_phone"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getBigDecimal("fee_total"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    status
            );
        }

        return jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               o.status,
                               EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               o.sender_name,
                               o.sender_phone,
                               o.receiver_name,
                               o.receiver_phone,
                               o.fee_total,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        ORDER BY o.created_at DESC, o.id DESC
                        """,
                (rs, rowNum) -> new AdminOrderItem(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getBoolean("refunded"),
                        rs.getString("sender_name"),
                        rs.getString("sender_phone"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getBigDecimal("fee_total"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }

    public long countRefundedOrders() {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(DISTINCT w.order_id)
                        FROM t_tracking_event te
                        JOIN t_waybill w ON w.waybill_no = te.waybill_no
                        WHERE te.event_type = 'REFUND_SUCCESS'
                        """,
                Long.class
        );
        return count == null ? 0L : count;
    }

    public Optional<OrderSnapshot> findOrderSnapshotById(Long orderId) {
        List<OrderSnapshot> list = jdbcTemplate.query("""
                        SELECT id, user_id, status
                        FROM t_order
                        WHERE id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderSnapshot(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("status")
                ),
                orderId
        );
        return list.stream().findFirst();
    }

    public Optional<WaybillSnapshot> findWaybillByOrderId(Long orderId) {
        List<WaybillSnapshot> list = jdbcTemplate.query("""
                        SELECT waybill_no, current_status
                        FROM t_waybill
                        WHERE order_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new WaybillSnapshot(
                        rs.getString("waybill_no"),
                        rs.getString("current_status")
                ),
                orderId
        );
        return list.stream().findFirst();
    }

    public Optional<WaybillSnapshot> findWaybillByNo(String waybillNo) {
        List<WaybillSnapshot> list = jdbcTemplate.query("""
                        SELECT waybill_no, current_status
                        FROM t_waybill
                        WHERE waybill_no = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new WaybillSnapshot(
                        rs.getString("waybill_no"),
                        rs.getString("current_status")
                ),
                waybillNo
        );
        return list.stream().findFirst();
    }

    public int updateOrderStatus(Long orderId, String status) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status,
                orderId
        );
    }

    public int updateOrderStatusByWaybillNo(String waybillNo, String status) {
        return jdbcTemplate.update("""
                        UPDATE t_order o
                        JOIN t_waybill w ON w.order_id = o.id
                        SET o.status = ?, o.updated_at = CURRENT_TIMESTAMP
                        WHERE w.waybill_no = ?
                        """,
                status,
                waybillNo
        );
    }

    public int updateWaybillStatusByOrderId(Long orderId, String status) {
        return jdbcTemplate.update("""
                        UPDATE t_waybill
                        SET current_status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE order_id = ?
                        """,
                status,
                orderId
        );
    }

    public int updateWaybillStatusByWaybillNo(String waybillNo, String status) {
        return jdbcTemplate.update("""
                        UPDATE t_waybill
                        SET current_status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE waybill_no = ?
                        """,
                status,
                waybillNo
        );
    }

    public void insertTrackingEvent(
            String waybillNo,
            LocalDateTime eventTime,
            String eventType,
            Long stationId,
            Long courierId,
            String description
    ) {
        jdbcTemplate.update("""
                        INSERT INTO t_tracking_event (waybill_no, event_time, event_type, station_id, courier_id, description)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                waybillNo,
                Timestamp.valueOf(eventTime),
                eventType,
                stationId,
                courierId,
                description
        );
    }

    public boolean existsTrackingEvent(String waybillNo, String eventType, LocalDateTime eventTime) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_tracking_event
                        WHERE waybill_no = ? AND event_type = ? AND event_time = ?
                        """,
                Integer.class,
                waybillNo,
                eventType,
                Timestamp.valueOf(eventTime)
        );
        return count != null && count > 0;
    }

    public boolean existsTrackingEventByType(String waybillNo, String eventType) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_tracking_event
                        WHERE waybill_no = ? AND event_type = ?
                        """,
                Integer.class,
                waybillNo,
                eventType
        );
        return count != null && count > 0;
    }

    public boolean existsActiveTask(String waybillNo, Integer taskType) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_task
                        WHERE waybill_no = ?
                          AND task_type = ?
                          AND status IN ('PENDING', 'ACCEPTED')
                        """,
                Integer.class,
                waybillNo,
                taskType
        );
        return count != null && count > 0;
    }

    public Long insertTask(String waybillNo, Integer taskType, Long stationId, String status, LocalDateTime plannedTime) {
        String sql = """
                INSERT INTO t_task (waybill_no, task_type, courier_id, station_id, status, planned_time)
                VALUES (?, ?, NULL, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, waybillNo);
            ps.setInt(2, taskType);
            ps.setLong(3, stationId);
            ps.setString(4, status);
            ps.setTimestamp(5, Timestamp.valueOf(plannedTime));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建任务失败，未返回主键");
        }
        return key.longValue();
    }

    public Optional<Long> findDefaultStationId() {
        List<Long> list = jdbcTemplate.query("""
                        SELECT id
                        FROM t_station
                        WHERE status = 1
                        ORDER BY id ASC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("id")
        );
        return list.stream().findFirst();
    }

    public Optional<CourierProfile> findCourierByUserId(Long userId) {
        List<CourierProfile> list = jdbcTemplate.query("""
                        SELECT id, user_id, station_id, work_no, name, phone
                        FROM t_courier
                        WHERE user_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new CourierProfile(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("station_id"),
                        rs.getString("work_no"),
                        rs.getString("name"),
                        rs.getString("phone")
                ),
                userId
        );
        return list.stream().findFirst();
    }

    public long countCourierTasks(Long courierId, String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            Long count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM t_task t
                            JOIN t_waybill w ON w.waybill_no = t.waybill_no
                            JOIN t_order o ON o.id = w.order_id
                            WHERE t.status = 'PENDING'
                              AND (t.courier_id IS NULL OR t.courier_id = ?)
                              AND o.status <> 'CANCELLED'
                            """,
                    Long.class,
                    courierId
            );
            return count == null ? 0L : count;
        }

        if (StringUtils.hasText(status)) {
            Long count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM t_task
                            WHERE status = ? AND courier_id = ?
                            """,
                    Long.class,
                    status,
                    courierId
            );
            return count == null ? 0L : count;
        }

        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_task t
                        JOIN t_waybill w ON w.waybill_no = t.waybill_no
                        JOIN t_order o ON o.id = w.order_id
                        WHERE (t.status = 'PENDING' AND t.courier_id IS NULL AND o.status <> 'CANCELLED')
                           OR t.courier_id = ?
                        """,
                Long.class,
                courierId
        );
        return count == null ? 0L : count;
    }

    public List<CourierTaskItem> findCourierTasks(Long courierId, String status, int offset, int size) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return jdbcTemplate.query("""
                            SELECT t.id,
                                   t.waybill_no,
                                   o.id AS order_id,
                                   o.order_no,
                                   o.status AS order_status,
                                   EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = t.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.receiver_addr,
                                   w.pay_type,
                                   EXISTS (
                                       SELECT 1
                                       FROM t_tracking_event te
                                       WHERE te.waybill_no = t.waybill_no
                                         AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                   ) AS paid,
                                   t.task_type,
                                   t.status AS task_status,
                                   t.planned_time,
                                   t.accepted_at
                            FROM t_task t
                            JOIN t_waybill w ON w.waybill_no = t.waybill_no
                            JOIN t_order o ON o.id = w.order_id
                            WHERE t.status = 'PENDING'
                              AND (t.courier_id IS NULL OR t.courier_id = ?)
                              AND o.status <> 'CANCELLED'
                            ORDER BY t.created_at DESC, t.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new CourierTaskItem(
                            rs.getLong("id"),
                            rs.getString("waybill_no"),
                            rs.getLong("order_id"),
                            rs.getString("order_no"),
                            rs.getString("order_status"),
                            rs.getBoolean("refunded"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getString("receiver_addr"),
                            rs.getInt("pay_type"),
                            rs.getBoolean("paid"),
                            rs.getInt("task_type"),
                            rs.getString("task_status"),
                            rs.getTimestamp("planned_time") == null ? null : rs.getTimestamp("planned_time").toLocalDateTime(),
                            rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toLocalDateTime()
                    ),
                    courierId,
                    size,
                    offset
            );
        }

        if (StringUtils.hasText(status)) {
            return jdbcTemplate.query("""
                            SELECT t.id,
                                   t.waybill_no,
                                   o.id AS order_id,
                                   o.order_no,
                                   o.status AS order_status,
                                   EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = t.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.receiver_addr,
                                   w.pay_type,
                                   EXISTS (
                                       SELECT 1
                                       FROM t_tracking_event te
                                       WHERE te.waybill_no = t.waybill_no
                                         AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                   ) AS paid,
                                   t.task_type,
                                   t.status AS task_status,
                                   t.planned_time,
                                   t.accepted_at
                            FROM t_task t
                            JOIN t_waybill w ON w.waybill_no = t.waybill_no
                            JOIN t_order o ON o.id = w.order_id
                            WHERE t.status = ?
                              AND t.courier_id = ?
                            ORDER BY t.created_at DESC, t.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new CourierTaskItem(
                            rs.getLong("id"),
                            rs.getString("waybill_no"),
                            rs.getLong("order_id"),
                            rs.getString("order_no"),
                            rs.getString("order_status"),
                            rs.getBoolean("refunded"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getString("receiver_addr"),
                            rs.getInt("pay_type"),
                            rs.getBoolean("paid"),
                            rs.getInt("task_type"),
                            rs.getString("task_status"),
                            rs.getTimestamp("planned_time") == null ? null : rs.getTimestamp("planned_time").toLocalDateTime(),
                            rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toLocalDateTime()
                    ),
                    status,
                    courierId,
                    size,
                    offset
            );
        }

        return jdbcTemplate.query("""
                        SELECT t.id,
                               t.waybill_no,
                               o.id AS order_id,
                               o.order_no,
                               o.status AS order_status,
                               EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = t.waybill_no
                                             AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               o.receiver_name,
                               o.receiver_phone,
                               o.receiver_addr,
                               w.pay_type,
                               EXISTS (
                                   SELECT 1
                                   FROM t_tracking_event te
                                   WHERE te.waybill_no = t.waybill_no
                                     AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                               ) AS paid,
                               t.task_type,
                               t.status AS task_status,
                               t.planned_time,
                               t.accepted_at
                        FROM t_task t
                        JOIN t_waybill w ON w.waybill_no = t.waybill_no
                        JOIN t_order o ON o.id = w.order_id
                        WHERE (t.status = 'PENDING' AND t.courier_id IS NULL AND o.status <> 'CANCELLED')
                           OR t.courier_id = ?
                        ORDER BY t.created_at DESC, t.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new CourierTaskItem(
                        rs.getLong("id"),
                        rs.getString("waybill_no"),
                        rs.getLong("order_id"),
                        rs.getString("order_no"),
                        rs.getString("order_status"),
                        rs.getBoolean("refunded"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_addr"),
                        rs.getInt("pay_type"),
                        rs.getBoolean("paid"),
                        rs.getInt("task_type"),
                        rs.getString("task_status"),
                        rs.getTimestamp("planned_time") == null ? null : rs.getTimestamp("planned_time").toLocalDateTime(),
                        rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toLocalDateTime()
                ),
                courierId,
                size,
                offset
        );
    }

    public Optional<TaskSnapshot> findTaskSnapshotById(Long taskId) {
        List<TaskSnapshot> list = jdbcTemplate.query("""
                        SELECT t.id,
                               o.id AS order_id,
                               t.waybill_no,
                               o.status AS order_status,
                               w.pay_type,
                               t.task_type,
                               t.status,
                               t.courier_id
                        FROM t_task t
                        JOIN t_waybill w ON w.waybill_no = t.waybill_no
                        JOIN t_order o ON o.id = w.order_id
                        WHERE t.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TaskSnapshot(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("waybill_no"),
                        rs.getString("order_status"),
                        rs.getInt("pay_type"),
                        rs.getInt("task_type"),
                        rs.getString("status"),
                        rs.getObject("courier_id", Long.class)
                ),
                taskId
        );
        return list.stream().findFirst();
    }

    public Optional<TaskSnapshot> findAcceptedDeliveryTaskByOrderId(Long orderId) {
        List<TaskSnapshot> list = jdbcTemplate.query("""
                        SELECT t.id,
                               o.id AS order_id,
                               t.waybill_no,
                               o.status AS order_status,
                               w.pay_type,
                               t.task_type,
                               t.status,
                               t.courier_id
                        FROM t_task t
                        JOIN t_waybill w ON w.waybill_no = t.waybill_no
                        JOIN t_order o ON o.id = w.order_id
                        WHERE o.id = ?
                          AND t.task_type = 2
                          AND t.status = 'ACCEPTED'
                        ORDER BY t.id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TaskSnapshot(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("waybill_no"),
                        rs.getString("order_status"),
                        rs.getInt("pay_type"),
                        rs.getInt("task_type"),
                        rs.getString("status"),
                        rs.getObject("courier_id", Long.class)
                ),
                orderId
        );
        return list.stream().findFirst();
    }

    public boolean existsCourierOrderAccess(Long courierId, Long orderId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_task t
                        JOIN t_waybill w ON w.waybill_no = t.waybill_no
                        WHERE w.order_id = ?
                          AND (
                              t.courier_id = ?
                              OR (t.status = 'PENDING' AND t.courier_id IS NULL)
                          )
                        """,
                Integer.class,
                orderId,
                courierId
        );
        return count != null && count > 0;
    }

    public int acceptTask(Long taskId, Long courierId) {
        return jdbcTemplate.update("""
                        UPDATE t_task
                        SET courier_id = ?,
                            status = 'ACCEPTED',
                            accepted_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND status = 'PENDING'
                          AND (courier_id IS NULL OR courier_id = ?)
                        """,
                courierId,
                taskId,
                courierId
        );
    }

    public int updateTaskStatusToFinished(Long taskId) {
        return jdbcTemplate.update("""
                        UPDATE t_task
                        SET status = 'FINISHED',
                            finished_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                taskId
        );
    }

    public int finishActiveTasksByWaybillNo(String waybillNo) {
        return jdbcTemplate.update("""
                        UPDATE t_task
                        SET status = 'FINISHED',
                            finished_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE waybill_no = ?
                          AND status IN ('PENDING', 'ACCEPTED')
                        """,
                waybillNo
        );
    }

    public int updateOrderRemark(Long orderId, String remark) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET remark = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                remark,
                orderId
        );
    }
}

