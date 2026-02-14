-- =========================================================
-- 快递物流管理系统 - 初始化基础数据
-- 说明：脚本按“可重复执行”方式编写
-- =========================================================

-- 角色初始化（删除商家角色，仅保留用户/快递员/管理员）
INSERT INTO t_role (role_code, role_name)
VALUES ('USER', '普通用户')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO t_role (role_code, role_name)
VALUES ('COURIER', '快递员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO t_role (role_code, role_name)
VALUES ('ADMIN', '系统管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 全国网点初始化（每省份/自治区/直辖市/特区一个中心网点）
INSERT INTO t_station (station_code, name, type, province, city, district, detail, status)
VALUES
('BJ001', '北京中心网点', 1, '北京市', '北京市', '朝阳区', '北京市朝阳区物流大道1号', 1),
('TJ001', '天津中心网点', 1, '天津市', '天津市', '河西区', '天津市河西区物流大道1号', 1),
('SH001', '上海中心网点', 1, '上海市', '上海市', '浦东新区', '上海市浦东新区物流大道1号', 1),
('CQ001', '重庆中心网点', 1, '重庆市', '重庆市', '渝北区', '重庆市渝北区物流大道1号', 1),
('HE001', '河北中心网点', 1, '河北省', '石家庄市', '长安区', '河北省石家庄市物流大道1号', 1),
('SX001', '山西中心网点', 1, '山西省', '太原市', '小店区', '山西省太原市物流大道1号', 1),
('LN001', '辽宁中心网点', 1, '辽宁省', '沈阳市', '和平区', '辽宁省沈阳市物流大道1号', 1),
('JL001', '吉林中心网点', 1, '吉林省', '长春市', '南关区', '吉林省长春市物流大道1号', 1),
('HL001', '黑龙江中心网点', 1, '黑龙江省', '哈尔滨市', '南岗区', '黑龙江省哈尔滨市物流大道1号', 1),
('JS001', '江苏中心网点', 1, '江苏省', '南京市', '玄武区', '江苏省南京市物流大道1号', 1),
('ZJ001', '浙江中心网点', 1, '浙江省', '杭州市', '西湖区', '浙江省杭州市物流大道1号', 1),
('AH001', '安徽中心网点', 1, '安徽省', '合肥市', '蜀山区', '安徽省合肥市物流大道1号', 1),
('FJ001', '福建中心网点', 1, '福建省', '福州市', '鼓楼区', '福建省福州市物流大道1号', 1),
('JX001', '江西中心网点', 1, '江西省', '南昌市', '红谷滩区', '江西省南昌市物流大道1号', 1),
('SD001', '山东中心网点', 1, '山东省', '济南市', '历下区', '山东省济南市物流大道1号', 1),
('HA001', '河南中心网点', 1, '河南省', '郑州市', '金水区', '河南省郑州市物流大道1号', 1),
('HB001', '湖北中心网点', 1, '湖北省', '武汉市', '武昌区', '湖北省武汉市物流大道1号', 1),
('HN001', '湖南中心网点', 1, '湖南省', '长沙市', '岳麓区', '湖南省长沙市物流大道1号', 1),
('GD001', '广东中心网点', 1, '广东省', '广州市', '天河区', '广东省广州市物流大道1号', 1),
('HI001', '海南中心网点', 1, '海南省', '海口市', '龙华区', '海南省海口市物流大道1号', 1),
('SC001', '四川中心网点', 1, '四川省', '成都市', '锦江区', '四川省成都市物流大道1号', 1),
('GZ001', '贵州中心网点', 1, '贵州省', '贵阳市', '南明区', '贵州省贵阳市物流大道1号', 1),
('YN001', '云南中心网点', 1, '云南省', '昆明市', '五华区', '云南省昆明市物流大道1号', 1),
('SN001', '陕西中心网点', 1, '陕西省', '西安市', '雁塔区', '陕西省西安市物流大道1号', 1),
('GS001', '甘肃中心网点', 1, '甘肃省', '兰州市', '城关区', '甘肃省兰州市物流大道1号', 1),
('QH001', '青海中心网点', 1, '青海省', '西宁市', '城西区', '青海省西宁市物流大道1号', 1),
('TW001', '台湾中心网点', 1, '台湾省', '台北市', '中正区', '台湾省台北市物流大道1号', 1),
('NM001', '内蒙古中心网点', 1, '内蒙古自治区', '呼和浩特市', '新城区', '内蒙古呼和浩特市物流大道1号', 1),
('GX001', '广西中心网点', 1, '广西壮族自治区', '南宁市', '青秀区', '广西南宁市物流大道1号', 1),
('XZ001', '西藏中心网点', 1, '西藏自治区', '拉萨市', '城关区', '西藏拉萨市物流大道1号', 1),
('NX001', '宁夏中心网点', 1, '宁夏回族自治区', '银川市', '金凤区', '宁夏银川市物流大道1号', 1),
('XJ001', '新疆中心网点', 1, '新疆维吾尔自治区', '乌鲁木齐市', '天山区', '新疆乌鲁木齐市物流大道1号', 1),
('HK001', '香港中心网点', 1, '香港特别行政区', '香港', '中西区', '香港中西区物流大道1号', 1),
('MO001', '澳门中心网点', 1, '澳门特别行政区', '澳门', '花地玛堂区', '澳门花地玛堂区物流大道1号', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    province = VALUES(province),
    city = VALUES(city),
    district = VALUES(district),
    detail = VALUES(detail),
    status = VALUES(status);

-- 管理员账号：admin / admin
INSERT INTO t_user (username, phone, email, password_hash, status)
SELECT 'admin',
       '13800000000',
       'admin@logistics.local',
       '$2a$10$VvaB2hvpFBKilgSQkZeQ9O0.K773ooTmCaBT4fMWgUeirToTnJ/4.',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM t_user WHERE phone = '13800000000'
);

UPDATE t_user
SET username = 'admin',
    email = 'admin@logistics.local',
    password_hash = '$2a$10$VvaB2hvpFBKilgSQkZeQ9O0.K773ooTmCaBT4fMWgUeirToTnJ/4.',
    status = 1
WHERE phone = '13800000000';

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u
JOIN t_role r ON r.role_code = 'ADMIN'
WHERE u.phone = '13800000000'
  AND NOT EXISTS (
      SELECT 1
      FROM t_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

-- 快递员账号：courier / admin
INSERT INTO t_user (username, phone, email, password_hash, status)
SELECT 'courier',
       '13600000000',
       'courier@logistics.local',
       '$2a$10$VvaB2hvpFBKilgSQkZeQ9O0.K773ooTmCaBT4fMWgUeirToTnJ/4.',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM t_user WHERE phone = '13600000000'
);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u
JOIN t_role r ON r.role_code = 'COURIER'
WHERE u.phone = '13600000000'
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO t_courier (user_id, station_id, work_no, name, phone, status)
SELECT u.id, s.id, 'CY0001', '测试快递员', '13600000000', 1
FROM t_user u
JOIN t_station s ON s.station_code = 'CQ001'
WHERE u.phone = '13600000000'
  AND NOT EXISTS (
      SELECT 1 FROM t_courier c WHERE c.user_id = u.id
  );

-- 普通用户账号：demo / admin
INSERT INTO t_user (username, phone, email, password_hash, status)
SELECT 'demo',
       '13900000000',
       'demo@logistics.local',
       '$2a$10$VvaB2hvpFBKilgSQkZeQ9O0.K773ooTmCaBT4fMWgUeirToTnJ/4.',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM t_user WHERE phone = '13900000000'
);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u
JOIN t_role r ON r.role_code = 'USER'
WHERE u.phone = '13900000000'
  AND NOT EXISTS (
      SELECT 1
      FROM t_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

-- 演示订单
INSERT INTO t_order (
    order_no, user_id, sender_name, sender_phone, sender_addr,
    receiver_name, receiver_phone, receiver_addr, service_type, status, fee_total, remark
)
SELECT 'OD202602120001',
       u.id,
       '张三',
       '13911112222',
       '重庆市渝北区寄件地址示例',
       '李四',
       '13933334444',
       '广东省广州市收件地址示例',
       1,
       'CREATED',
       18.50,
       '初始化演示订单'
FROM t_user u
WHERE u.phone = '13900000000'
  AND NOT EXISTS (
      SELECT 1 FROM t_order o WHERE o.order_no = 'OD202602120001'
  );

INSERT INTO t_waybill (
    waybill_no, order_id, weight, volume, charge_weight, fee_total,
    pay_type, insured_amount, current_status, current_station_id
)
SELECT 'WB202602120001',
       o.id,
       1.20,
       0.80,
       1.20,
       18.50,
       1,
       0.00,
       'CREATED',
       s.id
FROM t_order o
JOIN t_station s ON s.station_code = 'CQ001'
WHERE o.order_no = 'OD202602120001'
  AND NOT EXISTS (
      SELECT 1 FROM t_waybill w WHERE w.waybill_no = 'WB202602120001'
  );

INSERT INTO t_tracking_event (waybill_no, event_time, event_type, station_id, description)
SELECT 'WB202602120001',
       '2026-02-12 10:00:00',
       'CREATED',
       s.id,
       '订单已创建，等待快递员接单'
FROM t_station s
WHERE s.station_code = 'CQ001'
ON DUPLICATE KEY UPDATE
    description = VALUES(description);

INSERT INTO t_task (waybill_no, task_type, courier_id, station_id, status, planned_time)
SELECT 'WB202602120001',
       2,
       NULL,
       s.id,
       'PENDING',
       DATE_ADD(NOW(), INTERVAL 20 MINUTE)
FROM t_station s
WHERE s.station_code = 'CQ001'
  AND NOT EXISTS (
      SELECT 1
      FROM t_task t
      WHERE t.waybill_no = 'WB202602120001'
        AND t.task_type = 2
        AND t.status IN ('PENDING', 'ACCEPTED')
  );
