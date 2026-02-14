package com.yy.logistics.auth.repository;

import com.yy.logistics.auth.dto.StationOptionResponse;
import com.yy.logistics.auth.model.AuthAddress;
import com.yy.logistics.auth.model.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthUserRepository {

    private static final RowMapper<AuthUser> USER_ROW_MAPPER = (rs, rowNum) -> new AuthUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getInt("status")
    );

    private static final RowMapper<AuthAddress> ADDRESS_ROW_MAPPER = (rs, rowNum) -> new AuthAddress(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("contact_name"),
            rs.getString("contact_phone"),
            rs.getString("province"),
            rs.getString("city"),
            rs.getString("district"),
            rs.getString("detail"),
            rs.getInt("is_default")
    );

    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthUser> findByAccount(String account) {
        String sql = """
                SELECT id, username, phone, email, password_hash, status
                FROM t_user
                WHERE phone = ? OR email = ? OR username = ?
                ORDER BY id ASC
                LIMIT 1
                """;
        List<AuthUser> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, account, account, account);
        return users.stream().findFirst();
    }

    public Optional<AuthUser> findByAccountAndUsername(String account, String username) {
        String sql = """
                SELECT id, username, phone, email, password_hash, status
                FROM t_user
                WHERE (phone = ? OR email = ? OR username = ?)
                  AND username = ?
                ORDER BY id ASC
                LIMIT 1
                """;
        List<AuthUser> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, account, account, account, username);
        return users.stream().findFirst();
    }

    public Optional<AuthUser> findById(Long userId) {
        String sql = """
                SELECT id, username, phone, email, password_hash, status
                FROM t_user
                WHERE id = ?
                LIMIT 1
                """;
        List<AuthUser> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, userId);
        return users.stream().findFirst();
    }

    public boolean existsByPhone(String phone) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_user WHERE phone = ?", Integer.class, phone);
        return count != null && count > 0;
    }

    public boolean existsByPhoneExcludeUserId(String phone, Long excludeUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user WHERE phone = ? AND id <> ?",
                Integer.class,
                phone,
                excludeUserId
        );
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_user WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_user WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsByEmailExcludeUserId(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user WHERE email = ? AND id <> ?",
                Integer.class,
                email,
                excludeUserId
        );
        return count != null && count > 0;
    }

    public Long insertUser(String username, String phone, String email, String passwordHash) {
        String sql = """
                INSERT INTO t_user (username, phone, email, password_hash, status)
                VALUES (?, ?, ?, ?, 1)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, phone);
            ps.setString(3, StringUtils.hasText(email) ? email.trim() : null);
            ps.setString(4, passwordHash);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建用户失败，未返回主键");
        }
        return key.longValue();
    }

    public void updateUserContact(Long userId, String phone, String email) {
        jdbcTemplate.update(
                "UPDATE t_user SET phone = ?, email = ? WHERE id = ?",
                phone,
                StringUtils.hasText(email) ? email.trim() : null,
                userId
        );
    }

    public void updateUserPassword(Long userId, String passwordHash) {
        jdbcTemplate.update(
                "UPDATE t_user SET password_hash = ? WHERE id = ?",
                passwordHash,
                userId
        );
    }

    public Optional<Long> findRoleIdByCode(String roleCode) {
        List<Long> roleIds = jdbcTemplate.query(
                "SELECT id FROM t_role WHERE role_code = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                roleCode
        );
        return roleIds.stream().findFirst();
    }

    public void insertUserRole(Long userId, Long roleId) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_role (user_id, role_id)
                        VALUES (?, ?)
                        """,
                userId,
                roleId
        );
    }

    public List<String> findRolesByUserId(Long userId) {
        List<String> roles = jdbcTemplate.query(
                """
                        SELECT r.role_code
                        FROM t_role r
                        JOIN t_user_role ur ON ur.role_id = r.id
                        WHERE ur.user_id = ?
                        ORDER BY r.role_code
                        """,
                (rs, rowNum) -> "ROLE_" + rs.getString("role_code"),
                userId
        );
        return roles == null ? Collections.emptyList() : roles;
    }

    public Optional<Long> findStationIdById(Long stationId) {
        List<Long> stationIds = jdbcTemplate.query(
                """
                        SELECT id
                        FROM t_station
                        WHERE id = ? AND status = 1
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                stationId
        );
        return stationIds.stream().findFirst();
    }

    public Optional<Long> findDefaultStationId() {
        List<Long> stationIds = jdbcTemplate.query(
                """
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

    public List<StationOptionResponse> findActiveStations() {
        return jdbcTemplate.query(
                """
                        SELECT id, name, province, city
                        FROM t_station
                        WHERE status = 1
                        ORDER BY province ASC, city ASC, id ASC
                        """,
                (rs, rowNum) -> new StationOptionResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("province"),
                        rs.getString("city")
                )
        );
    }

    public boolean existsCourierWorkNo(String workNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_courier WHERE work_no = ?",
                Integer.class,
                workNo
        );
        return count != null && count > 0;
    }

    public void insertCourier(Long userId, Long stationId, String workNo, String name, String phone) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_courier (user_id, station_id, work_no, name, phone, status)
                        VALUES (?, ?, ?, ?, ?, 1)
                        """,
                userId,
                stationId,
                workNo,
                name,
                phone
        );
    }

    public List<AuthAddress> listAddressByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, user_id, contact_name, contact_phone, province, city, district, detail, is_default
                        FROM t_address
                        WHERE user_id = ?
                        ORDER BY is_default DESC, id DESC
                        """,
                ADDRESS_ROW_MAPPER,
                userId
        );
    }

    public Optional<AuthAddress> findAddressByIdAndUserId(Long addressId, Long userId) {
        List<AuthAddress> list = jdbcTemplate.query(
                """
                        SELECT id, user_id, contact_name, contact_phone, province, city, district, detail, is_default
                        FROM t_address
                        WHERE id = ? AND user_id = ?
                        LIMIT 1
                        """,
                ADDRESS_ROW_MAPPER,
                addressId,
                userId
        );
        return list.stream().findFirst();
    }

    public Long insertAddress(
            Long userId,
            String contactName,
            String contactPhone,
            String province,
            String city,
            String district,
            String detail,
            int isDefault
    ) {
        String sql = """
                INSERT INTO t_address (user_id, contact_name, contact_phone, province, city, district, detail, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, contactName);
            ps.setString(3, contactPhone);
            ps.setString(4, province);
            ps.setString(5, city);
            ps.setString(6, district);
            ps.setString(7, detail);
            ps.setInt(8, isDefault);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建地址失败，未返回主键");
        }
        return key.longValue();
    }

    public void updateAddress(
            Long addressId,
            Long userId,
            String contactName,
            String contactPhone,
            String province,
            String city,
            String district,
            String detail,
            int isDefault
    ) {
        jdbcTemplate.update(
                """
                        UPDATE t_address
                        SET contact_name = ?,
                            contact_phone = ?,
                            province = ?,
                            city = ?,
                            district = ?,
                            detail = ?,
                            is_default = ?
                        WHERE id = ? AND user_id = ?
                        """,
                contactName,
                contactPhone,
                province,
                city,
                district,
                detail,
                isDefault,
                addressId,
                userId
        );
    }

    public int deleteAddressByIdAndUserId(Long addressId, Long userId) {
        return jdbcTemplate.update(
                "DELETE FROM t_address WHERE id = ? AND user_id = ?",
                addressId,
                userId
        );
    }

    public void clearDefaultAddressByUserId(Long userId) {
        jdbcTemplate.update("UPDATE t_address SET is_default = 0 WHERE user_id = ?", userId);
    }

    public int setDefaultAddress(Long userId, Long addressId) {
        return jdbcTemplate.update(
                "UPDATE t_address SET is_default = 1 WHERE user_id = ? AND id = ?",
                userId,
                addressId
        );
    }

    public int countAddressByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_address WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public Optional<AuthAddress> findFirstAddressByUserId(Long userId) {
        List<AuthAddress> list = jdbcTemplate.query(
                """
                        SELECT id, user_id, contact_name, contact_phone, province, city, district, detail, is_default
                        FROM t_address
                        WHERE user_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                ADDRESS_ROW_MAPPER,
                userId
        );
        return list.stream().findFirst();
    }
}
