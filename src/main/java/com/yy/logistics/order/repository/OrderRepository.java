package com.yy.logistics.order.repository;

import com.yy.logistics.order.dto.OrderDetailResponse;
import com.yy.logistics.order.dto.OrderListItemResponse;
import com.yy.logistics.order.dto.TrackingEventResponse;
import com.yy.logistics.order.model.AcceptedTaskSnapshot;
import com.yy.logistics.order.model.OrderPaymentSnapshot;
import com.yy.logistics.order.model.OrderPricingSnapshot;
import com.yy.logistics.order.model.OrderSnapshot;
import com.yy.logistics.order.model.TransitRouteSnapshot;
import com.yy.logistics.order.model.WaybillSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsOrderNo(String orderNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_order WHERE order_no = ?",
                Integer.class,
                orderNo
        );
        return count != null && count > 0;
    }

    public boolean existsWaybillNo(String waybillNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_waybill WHERE waybill_no = ?",
                Integer.class,
                waybillNo
        );
        return count != null && count > 0;
    }

    public Long insertOrder(
            String orderNo,
            Long userId,
            String senderName,
            String senderPhone,
            String senderAddr,
            String receiverName,
            String receiverPhone,
            String receiverAddr,
            Integer serviceType,
            String status,
            BigDecimal feeTotal,
            String remark
    ) {
        String sql = """
                INSERT INTO t_order (
                    order_no, user_id, sender_name, sender_phone, sender_addr,
                    receiver_name, receiver_phone, receiver_addr, service_type, status, fee_total, remark
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderNo);
            ps.setLong(2, userId);
            ps.setString(3, senderName);
            ps.setString(4, senderPhone);
            ps.setString(5, senderAddr);
            ps.setString(6, receiverName);
            ps.setString(7, receiverPhone);
            ps.setString(8, receiverAddr);
            ps.setInt(9, serviceType);
            ps.setString(10, status);
            ps.setBigDecimal(11, feeTotal);
            ps.setString(12, StringUtils.hasText(remark) ? remark : null);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("鍒涘缓璁㈠崟澶辫触锛屾湭杩斿洖涓婚敭");
        }
        return key.longValue();
    }

    public Long insertWaybill(
            String waybillNo,
            Long orderId,
            BigDecimal weight,
            BigDecimal volume,
            BigDecimal chargeWeight,
            BigDecimal feeTotal,
            Integer payType,
            BigDecimal insuredAmount,
            String currentStatus
    ) {
        String sql = """
                INSERT INTO t_waybill (
                    waybill_no, order_id, weight, volume, charge_weight, fee_total,
                    pay_type, insured_amount, current_status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, waybillNo);
            ps.setLong(2, orderId);
            ps.setBigDecimal(3, weight);
            ps.setBigDecimal(4, volume);
            ps.setBigDecimal(5, chargeWeight);
            ps.setBigDecimal(6, feeTotal);
            ps.setInt(7, payType);
            ps.setBigDecimal(8, insuredAmount);
            ps.setString(9, currentStatus);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("鍒涘缓杩愬崟澶辫触锛屾湭杩斿洖涓婚敭");
        }
        return key.longValue();
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

    public long countOrdersByRelation(Long userId, String receiverPhone, String status, String relation) {
        boolean receiverOnly = "receiver".equalsIgnoreCase(relation);
        boolean all = "all".equalsIgnoreCase(relation);

        if (receiverOnly) {
            if (!StringUtils.hasText(status)) {
                Long total = jdbcTemplate.queryForObject("""
                                SELECT COUNT(1)
                                FROM t_order
                                WHERE receiver_phone = ?
                                """,
                        Long.class,
                        receiverPhone
                );
                return total == null ? 0L : total;
            }
            Long total = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM t_order
                            WHERE receiver_phone = ? AND status = ?
                            """,
                    Long.class,
                    receiverPhone,
                    status
            );
            return total == null ? 0L : total;
        }

        if (all) {
            if (!StringUtils.hasText(status)) {
                Long total = jdbcTemplate.queryForObject("""
                                SELECT COUNT(1)
                                FROM t_order
                                WHERE user_id = ? OR receiver_phone = ?
                                """,
                        Long.class,
                        userId,
                        receiverPhone
                );
                return total == null ? 0L : total;
            }
            Long total = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM t_order
                            WHERE (user_id = ? OR receiver_phone = ?)
                              AND status = ?
                            """,
                    Long.class,
                    userId,
                    receiverPhone,
                    status
            );
            return total == null ? 0L : total;
        }

        if (StringUtils.hasText(status)) {
            Long total = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM t_order
                            WHERE user_id = ? AND status = ?
                            """,
                    Long.class,
                    userId,
                    status
            );
            return total == null ? 0L : total;
        }

        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM t_order
                        WHERE user_id = ?
                        """,
                Long.class,
                userId
        );
        return total == null ? 0L : total;
    }

    public List<OrderListItemResponse> findOrdersByRelation(Long userId, String receiverPhone, String status, String relation, int offset, int size) {
        boolean receiverOnly = "receiver".equalsIgnoreCase(relation);
        boolean all = "all".equalsIgnoreCase(relation);

        if (receiverOnly) {
            if (StringUtils.hasText(status)) {
                return jdbcTemplate.query("""
                                SELECT o.id,
                                       o.order_no,
                                       w.waybill_no,
                                       'RECEIVER' AS relation_type,
                                       o.status,
                                       w.pay_type,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                        ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                       o.fee_total,
                                       o.receiver_name,
                                       o.receiver_phone,
                                       o.created_at
                                FROM t_order o
                                LEFT JOIN t_waybill w ON w.order_id = o.id
                                WHERE o.receiver_phone = ? AND o.status = ?
                                ORDER BY o.created_at DESC, o.id DESC
                                LIMIT ? OFFSET ?
                                """,
                        (rs, rowNum) -> new OrderListItemResponse(
                                rs.getLong("id"),
                                rs.getString("order_no"),
                                rs.getString("waybill_no"),
                                rs.getString("relation_type"),
                                rs.getString("status"),
                                rs.getInt("pay_type"),
                                rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                                rs.getBigDecimal("fee_total"),
                                rs.getString("receiver_name"),
                                rs.getString("receiver_phone"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ),
                        receiverPhone,
                        status,
                        size,
                        offset
                );
            }
            return jdbcTemplate.query("""
                            SELECT o.id,
                                   o.order_no,
                                   w.waybill_no,
                                   'RECEIVER' AS relation_type,
                                   o.status,
                                   w.pay_type,
                                   EXISTS (
                                       SELECT 1
                                       FROM t_tracking_event te
                                       WHERE te.waybill_no = w.waybill_no
                                         AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                    ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.fee_total,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.created_at
                            FROM t_order o
                            LEFT JOIN t_waybill w ON w.order_id = o.id
                            WHERE o.receiver_phone = ?
                            ORDER BY o.created_at DESC, o.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new OrderListItemResponse(
                            rs.getLong("id"),
                            rs.getString("order_no"),
                            rs.getString("waybill_no"),
                            rs.getString("relation_type"),
                            rs.getString("status"),
                            rs.getInt("pay_type"),
                            rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                            rs.getBigDecimal("fee_total"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    receiverPhone,
                    size,
                    offset
            );
        }

        if (all) {
            if (StringUtils.hasText(status)) {
                return jdbcTemplate.query("""
                                SELECT o.id,
                                       o.order_no,
                                       w.waybill_no,
                                       CASE WHEN o.user_id = ? THEN 'SENDER' ELSE 'RECEIVER' END AS relation_type,
                                       o.status,
                                       w.pay_type,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                             AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                        ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                       o.fee_total,
                                       o.receiver_name,
                                       o.receiver_phone,
                                       o.created_at
                                FROM t_order o
                                LEFT JOIN t_waybill w ON w.order_id = o.id
                                WHERE (o.user_id = ? OR o.receiver_phone = ?)
                                  AND o.status = ?
                                ORDER BY o.created_at DESC, o.id DESC
                                LIMIT ? OFFSET ?
                                """,
                        (rs, rowNum) -> new OrderListItemResponse(
                                rs.getLong("id"),
                                rs.getString("order_no"),
                                rs.getString("waybill_no"),
                                rs.getString("relation_type"),
                                rs.getString("status"),
                                rs.getInt("pay_type"),
                                rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                                rs.getBigDecimal("fee_total"),
                                rs.getString("receiver_name"),
                                rs.getString("receiver_phone"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ),
                        userId,
                        userId,
                        receiverPhone,
                        status,
                        size,
                        offset
                );
            }
            return jdbcTemplate.query("""
                            SELECT o.id,
                                   o.order_no,
                                   w.waybill_no,
                                   CASE WHEN o.user_id = ? THEN 'SENDER' ELSE 'RECEIVER' END AS relation_type,
                                   o.status,
                                   w.pay_type,
                                   EXISTS (
                                       SELECT 1
                                       FROM t_tracking_event te
                                       WHERE te.waybill_no = w.waybill_no
                                         AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                    ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.fee_total,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.created_at
                            FROM t_order o
                            LEFT JOIN t_waybill w ON w.order_id = o.id
                            WHERE o.user_id = ? OR o.receiver_phone = ?
                            ORDER BY o.created_at DESC, o.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new OrderListItemResponse(
                            rs.getLong("id"),
                            rs.getString("order_no"),
                            rs.getString("waybill_no"),
                            rs.getString("relation_type"),
                            rs.getString("status"),
                            rs.getInt("pay_type"),
                            rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                            rs.getBigDecimal("fee_total"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    userId,
                    userId,
                    receiverPhone,
                    size,
                    offset
            );
        }

        if (StringUtils.hasText(status)) {
            return jdbcTemplate.query("""
                            SELECT o.id,
                                   o.order_no,
                                   w.waybill_no,
                                   'SENDER' AS relation_type,
                                   o.status,
                                   w.pay_type,
                                   EXISTS (
                                       SELECT 1
                                       FROM t_tracking_event te
                                       WHERE te.waybill_no = w.waybill_no
                                         AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                    ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                                   o.fee_total,
                                   o.receiver_name,
                                   o.receiver_phone,
                                   o.created_at
                            FROM t_order o
                            LEFT JOIN t_waybill w ON w.order_id = o.id
                            WHERE o.user_id = ? AND o.status = ?
                            ORDER BY o.created_at DESC, o.id DESC
                            LIMIT ? OFFSET ?
                            """,
                    (rs, rowNum) -> new OrderListItemResponse(
                            rs.getLong("id"),
                            rs.getString("order_no"),
                            rs.getString("waybill_no"),
                            rs.getString("relation_type"),
                            rs.getString("status"),
                            rs.getInt("pay_type"),
                            rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                            rs.getBigDecimal("fee_total"),
                            rs.getString("receiver_name"),
                            rs.getString("receiver_phone"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ),
                    userId,
                    status,
                    size,
                    offset
            );
        }

        return jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               'SENDER' AS relation_type,
                               o.status,
                               w.pay_type,
                               EXISTS (
                                   SELECT 1
                                   FROM t_tracking_event te
                                   WHERE te.waybill_no = w.waybill_no
                                     AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               o.fee_total,
                               o.receiver_name,
                               o.receiver_phone,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        WHERE o.user_id = ?
                        ORDER BY o.created_at DESC, o.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new OrderListItemResponse(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("relation_type"),
                        rs.getString("status"),
                        rs.getInt("pay_type"),
                        rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                        rs.getBigDecimal("fee_total"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                userId,
                size,
                offset
        );
    }

    public Optional<OrderDetailResponse> findOrderDetailByUserAndId(Long userId, Long orderId) {
        List<OrderDetailResponse> list = jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               o.status,
                               w.current_status,
                               o.sender_name,
                               o.sender_phone,
                               o.sender_addr,
                               o.receiver_name,
                               o.receiver_phone,
                               o.receiver_addr,
                               o.service_type,
                               w.pay_type,
                               EXISTS (
                                   SELECT 1
                                   FROM t_tracking_event te
                                   WHERE te.waybill_no = w.waybill_no
                                     AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               w.weight,
                               w.volume,
                               w.charge_weight,
                               o.fee_total,
                               o.remark,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        WHERE o.user_id = ? AND o.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderDetailResponse(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getString("current_status"),
                        rs.getString("sender_name"),
                        rs.getString("sender_phone"),
                        rs.getString("sender_addr"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_addr"),
                        rs.getInt("service_type"),
                        rs.getInt("pay_type"),
                        rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                        rs.getBigDecimal("weight"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("charge_weight"),
                        rs.getBigDecimal("fee_total"),
                        rs.getString("remark"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                userId,
                orderId
        );
        return list.stream().findFirst();
    }

    public Optional<OrderDetailResponse> findOrderDetailByUserOrReceiver(Long userId, String receiverPhone, Long orderId) {
        List<OrderDetailResponse> list = jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               o.status,
                               w.current_status,
                               o.sender_name,
                               o.sender_phone,
                               o.sender_addr,
                               o.receiver_name,
                               o.receiver_phone,
                               o.receiver_addr,
                               o.service_type,
                               w.pay_type,
                               EXISTS (
                                   SELECT 1
                                   FROM t_tracking_event te
                                   WHERE te.waybill_no = w.waybill_no
                                     AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               w.weight,
                               w.volume,
                               w.charge_weight,
                               o.fee_total,
                               o.remark,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        WHERE (o.user_id = ? OR o.receiver_phone = ?)
                          AND o.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderDetailResponse(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getString("current_status"),
                        rs.getString("sender_name"),
                        rs.getString("sender_phone"),
                        rs.getString("sender_addr"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_addr"),
                        rs.getInt("service_type"),
                        rs.getInt("pay_type"),
                        rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                        rs.getBigDecimal("weight"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("charge_weight"),
                        rs.getBigDecimal("fee_total"),
                        rs.getString("remark"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                userId,
                receiverPhone,
                orderId
        );
        return list.stream().findFirst();
    }

    public Optional<OrderDetailResponse> findOrderDetailById(Long orderId) {
        List<OrderDetailResponse> list = jdbcTemplate.query("""
                        SELECT o.id,
                               o.order_no,
                               w.waybill_no,
                               o.status,
                               w.current_status,
                               o.sender_name,
                               o.sender_phone,
                               o.sender_addr,
                               o.receiver_name,
                               o.receiver_phone,
                               o.receiver_addr,
                               o.service_type,
                               w.pay_type,
                               EXISTS (
                                   SELECT 1
                                   FROM t_tracking_event te
                                   WHERE te.waybill_no = w.waybill_no
                                     AND te.event_type IN ('PAYMENT_CONFIRMED', 'COD_PAID_CONFIRMED')
                                ) AS paid,
                                       EXISTS (
                                           SELECT 1
                                           FROM t_tracking_event te
                                           WHERE te.waybill_no = w.waybill_no
                                           AND te.event_type = 'REFUND_SUCCESS'
                                       ) AS refunded,
                               w.weight,
                               w.volume,
                               w.charge_weight,
                               o.fee_total,
                               o.remark,
                               o.created_at
                        FROM t_order o
                        LEFT JOIN t_waybill w ON w.order_id = o.id
                        WHERE o.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderDetailResponse(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getString("current_status"),
                        rs.getString("sender_name"),
                        rs.getString("sender_phone"),
                        rs.getString("sender_addr"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_addr"),
                        rs.getInt("service_type"),
                        rs.getInt("pay_type"),
                        rs.getBoolean("paid"),
                                rs.getBoolean("refunded"),
                        rs.getBigDecimal("weight"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("charge_weight"),
                        rs.getBigDecimal("fee_total"),
                        rs.getString("remark"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                orderId
        );
        return list.stream().findFirst();
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

    public Optional<OrderPricingSnapshot> findOrderPricingSnapshotById(Long orderId) {
        List<OrderPricingSnapshot> list = jdbcTemplate.query("""
                        SELECT o.id,
                               o.user_id,
                               o.order_no,
                               w.waybill_no,
                               o.status AS order_status,
                               w.current_status AS waybill_status,
                               o.sender_addr,
                               o.receiver_addr,
                               o.service_type,
                               w.weight,
                               w.volume,
                               w.insured_amount,
                               w.charge_weight,
                               o.fee_total
                        FROM t_order o
                        JOIN t_waybill w ON w.order_id = o.id
                        WHERE o.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderPricingSnapshot(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("order_status"),
                        rs.getString("waybill_status"),
                        rs.getString("sender_addr"),
                        rs.getString("receiver_addr"),
                        rs.getInt("service_type"),
                        rs.getBigDecimal("weight"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("insured_amount"),
                        rs.getBigDecimal("charge_weight"),
                        rs.getBigDecimal("fee_total")
                ),
                orderId
        );
        return list.stream().findFirst();
    }

    public Optional<OrderPaymentSnapshot> findOrderPaymentSnapshotById(Long orderId) {
        List<OrderPaymentSnapshot> list = jdbcTemplate.query("""
                        SELECT o.id,
                               o.user_id,
                               o.order_no,
                               w.waybill_no,
                               o.status AS order_status,
                               w.pay_type,
                               o.fee_total,
                               o.remark
                        FROM t_order o
                        JOIN t_waybill w ON w.order_id = o.id
                        WHERE o.id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new OrderPaymentSnapshot(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("order_no"),
                        rs.getString("waybill_no"),
                        rs.getString("order_status"),
                        rs.getInt("pay_type"),
                        rs.getBigDecimal("fee_total"),
                        rs.getString("remark")
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

    public int cancelOrder(Long orderId, String remark) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET status = 'CANCELLED',
                            remark = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                remark,
                orderId
        );
    }

    public int updateOrderStatus(Long orderId, String status, String remark) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET status = ?,
                            remark = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status,
                remark,
                orderId
        );
    }

    public int updateOrderStatus(Long orderId, String status) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET status = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status,
                orderId
        );
    }

    public int updateOrderReceiverAndFee(Long orderId, String receiverName, String receiverPhone, String receiverAddr, BigDecimal feeTotal, String remark) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET receiver_name = ?,
                            receiver_phone = ?,
                            receiver_addr = ?,
                            fee_total = ?,
                            remark = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                receiverName,
                receiverPhone,
                receiverAddr,
                feeTotal,
                remark,
                orderId
        );
    }

    public int updateOrderRemark(Long orderId, String remark) {
        return jdbcTemplate.update("""
                        UPDATE t_order
                        SET remark = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                remark,
                orderId
        );
    }

    public int updateWaybillStatusByOrderId(Long orderId, String currentStatus) {
        return jdbcTemplate.update("""
                        UPDATE t_waybill
                        SET current_status = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE order_id = ?
                        """,
                currentStatus,
                orderId
        );
    }

    public int updateWaybillPricingByOrderId(Long orderId, BigDecimal chargeWeight, BigDecimal feeTotal) {
        return jdbcTemplate.update("""
                        UPDATE t_waybill
                        SET charge_weight = ?,
                            fee_total = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE order_id = ?
                        """,
                chargeWeight,
                feeTotal,
                orderId
        );
    }

    public List<TrackingEventResponse> findTrackingEvents(String waybillNo) {
        return jdbcTemplate.query("""
                        SELECT event_time, event_type, station_id, courier_id, description
                        FROM t_tracking_event
                        WHERE waybill_no = ?
                        ORDER BY event_time ASC, id ASC
                        """,
                (rs, rowNum) -> new TrackingEventResponse(
                        rs.getTimestamp("event_time").toLocalDateTime(),
                        rs.getString("event_type"),
                        rs.getObject("station_id", Long.class),
                        rs.getObject("courier_id", Long.class),
                        rs.getString("description")
                ),
                waybillNo
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

    public Optional<Long> findDefaultStationId() {
        List<Long> stationIds = jdbcTemplate.query("""
                        SELECT id
                        FROM t_station
                        WHERE status = 1
                        ORDER BY id ASC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("id")
        );
        return stationIds.stream().findFirst();
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
            throw new IllegalStateException("鍒涘缓浠诲姟澶辫触锛屾湭杩斿洖涓婚敭");
        }
        return key.longValue();
    }

    public Optional<AcceptedTaskSnapshot> findAcceptedTaskByWaybillNo(String waybillNo) {
        List<AcceptedTaskSnapshot> tasks = jdbcTemplate.query("""
                        SELECT id, waybill_no, status, courier_id, accepted_at
                        FROM t_task
                        WHERE waybill_no = ?
                          AND task_type = 2
                          AND status IN ('ACCEPTED', 'FINISHED')
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AcceptedTaskSnapshot(
                        rs.getLong("id"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getLong("courier_id"),
                        rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toLocalDateTime()
                ),
                waybillNo
        );
        return tasks.stream().findFirst();
    }

    public Optional<AcceptedTaskSnapshot> findActiveAcceptedTaskByWaybillNo(String waybillNo) {
        List<AcceptedTaskSnapshot> tasks = jdbcTemplate.query("""
                        SELECT id, waybill_no, status, courier_id, accepted_at
                        FROM t_task
                        WHERE waybill_no = ?
                          AND task_type = 2
                          AND status = 'ACCEPTED'
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AcceptedTaskSnapshot(
                        rs.getLong("id"),
                        rs.getString("waybill_no"),
                        rs.getString("status"),
                        rs.getLong("courier_id"),
                        rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toLocalDateTime()
                ),
                waybillNo
        );
        return tasks.stream().findFirst();
    }

    public Optional<TransitRouteSnapshot> findTransitRouteByWaybillNo(String waybillNo) {
        List<TransitRouteSnapshot> routes = jdbcTemplate.query("""
                        SELECT w.waybill_no, o.sender_addr, o.receiver_addr, w.current_status, w.pay_type
                        FROM t_waybill w
                        JOIN t_order o ON o.id = w.order_id
                        WHERE w.waybill_no = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TransitRouteSnapshot(
                        rs.getString("waybill_no"),
                        rs.getString("sender_addr"),
                        rs.getString("receiver_addr"),
                        rs.getString("current_status"),
                        rs.getInt("pay_type")
                ),
                waybillNo
        );
        return routes.stream().findFirst();
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

    public int finishAcceptedTaskByWaybillNo(String waybillNo) {
        return jdbcTemplate.update("""
                        UPDATE t_task
                        SET status = 'FINISHED',
                            finished_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE waybill_no = ?
                          AND task_type = 2
                          AND status = 'ACCEPTED'
                        """,
                waybillNo
        );
    }

    public List<String> findAcceptedWaybillNosByCourierId(Long courierId) {
        return jdbcTemplate.query("""
                        SELECT waybill_no
                        FROM t_task
                        WHERE task_type = 2
                          AND status = 'ACCEPTED'
                          AND courier_id = ?
                        ORDER BY accepted_at DESC, id DESC
                        """,
                (rs, rowNum) -> rs.getString("waybill_no"),
                courierId
        );
    }
}




