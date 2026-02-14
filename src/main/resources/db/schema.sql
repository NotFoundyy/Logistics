-- =========================================================
-- 快递物流管理系统 - MySQL 初始化脚本
-- 说明：Spring Boot 启动时自动执行
-- 兼容：MySQL 5.7+
-- =========================================================

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NULL COMMENT '用户名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '邮箱',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_user_phone UNIQUE (phone),
    CONSTRAINT uk_user_email UNIQUE (email),
    INDEX idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_role_code UNIQUE (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    station_code VARCHAR(50) NOT NULL COMMENT '网点编码',
    name VARCHAR(100) NOT NULL COMMENT '网点名称',
    type TINYINT NOT NULL COMMENT '类型：1网点，2仓库',
    province VARCHAR(50) NOT NULL COMMENT '省',
    city VARCHAR(50) NOT NULL COMMENT '市',
    district VARCHAR(50) NOT NULL COMMENT '区',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_station_code UNIQUE (station_code),
    INDEX idx_station_city_status (city, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网点/仓库表';

CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS t_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    province VARCHAR(50) NOT NULL COMMENT '省',
    city VARCHAR(50) NOT NULL COMMENT '市',
    district VARCHAR(50) NOT NULL COMMENT '区',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：1是，0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_address_user (user_id),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='地址簿表';

CREATE TABLE IF NOT EXISTS t_courier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    station_id BIGINT NOT NULL COMMENT '所属网点ID',
    work_no VARCHAR(50) NOT NULL COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1在职，0离职',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_courier_work_no UNIQUE (work_no),
    CONSTRAINT uk_courier_user UNIQUE (user_id),
    INDEX idx_courier_station_status (station_id, status),
    CONSTRAINT fk_courier_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_courier_station FOREIGN KEY (station_id) REFERENCES t_station (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='快递员表';

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    sender_name VARCHAR(50) NOT NULL COMMENT '寄件人姓名',
    sender_phone VARCHAR(20) NOT NULL COMMENT '寄件人电话',
    sender_addr VARCHAR(255) NOT NULL COMMENT '寄件地址',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收件人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件人电话',
    receiver_addr VARCHAR(255) NOT NULL COMMENT '收件地址',
    service_type TINYINT NOT NULL DEFAULT 1 COMMENT '服务类型',
    status VARCHAR(30) NOT NULL COMMENT '订单状态',
    fee_total DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '费用总计',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_order_no UNIQUE (order_no),
    INDEX idx_order_user_status_created (user_id, status, created_at),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS t_waybill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    waybill_no VARCHAR(50) NOT NULL COMMENT '运单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    weight DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实重(kg)',
    volume DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '体积重(kg)',
    charge_weight DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '计费重(kg)',
    fee_total DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    pay_type TINYINT NOT NULL DEFAULT 1 COMMENT '支付方式：1在线，2到付',
    insured_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '保价金额',
    current_status VARCHAR(30) NOT NULL COMMENT '当前状态',
    current_station_id BIGINT NULL COMMENT '当前网点ID',
    current_courier_id BIGINT NULL COMMENT '当前快递员ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_waybill_no UNIQUE (waybill_no),
    CONSTRAINT uk_waybill_order_id UNIQUE (order_id),
    INDEX idx_waybill_status (current_status),
    INDEX idx_waybill_station (current_station_id),
    CONSTRAINT fk_waybill_order FOREIGN KEY (order_id) REFERENCES t_order (id),
    CONSTRAINT fk_waybill_station FOREIGN KEY (current_station_id) REFERENCES t_station (id),
    CONSTRAINT fk_waybill_courier FOREIGN KEY (current_courier_id) REFERENCES t_courier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='运单表';

CREATE TABLE IF NOT EXISTS t_tracking_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    waybill_no VARCHAR(50) NOT NULL COMMENT '运单号',
    event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '节点时间',
    event_type VARCHAR(30) NOT NULL COMMENT '节点类型',
    station_id BIGINT NULL COMMENT '网点ID',
    courier_id BIGINT NULL COMMENT '快递员ID',
    description VARCHAR(255) NOT NULL COMMENT '节点描述',
    ext_json JSON NULL COMMENT '扩展字段',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_tracking_waybill_time (waybill_no, event_time),
    INDEX idx_tracking_event_type (event_type),
    CONSTRAINT uk_tracking_unique UNIQUE (waybill_no, event_type, event_time),
    CONSTRAINT fk_tracking_waybill FOREIGN KEY (waybill_no) REFERENCES t_waybill (waybill_no),
    CONSTRAINT fk_tracking_station FOREIGN KEY (station_id) REFERENCES t_station (id),
    CONSTRAINT fk_tracking_courier FOREIGN KEY (courier_id) REFERENCES t_courier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物流轨迹表';

CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    waybill_no VARCHAR(50) NOT NULL COMMENT '运单号',
    task_type TINYINT NOT NULL COMMENT '任务类型：1揽收，2派送',
    courier_id BIGINT NULL COMMENT '快递员ID',
    station_id BIGINT NOT NULL COMMENT '网点ID',
    status VARCHAR(30) NOT NULL COMMENT '任务状态',
    planned_time DATETIME NULL COMMENT '计划时间',
    accepted_at DATETIME NULL COMMENT '接单时间',
    finished_at DATETIME NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_courier_status (courier_id, status),
    INDEX idx_task_station_status (station_id, status),
    CONSTRAINT uk_task_stage UNIQUE (waybill_no, task_type, status),
    CONSTRAINT fk_task_waybill FOREIGN KEY (waybill_no) REFERENCES t_waybill (waybill_no),
    CONSTRAINT fk_task_courier FOREIGN KEY (courier_id) REFERENCES t_courier (id),
    CONSTRAINT fk_task_station FOREIGN KEY (station_id) REFERENCES t_station (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务表';
