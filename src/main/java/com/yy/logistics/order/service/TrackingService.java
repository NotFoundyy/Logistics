package com.yy.logistics.order.service;

import com.yy.logistics.common.enums.ErrorCode;
import com.yy.logistics.common.exception.BizException;
import com.yy.logistics.order.dto.TrackingEventResponse;
import com.yy.logistics.order.dto.TrackingGeoPointResponse;
import com.yy.logistics.order.dto.TrackingProgressResponse;
import com.yy.logistics.order.dto.TrackingQueryResponse;
import com.yy.logistics.order.model.AcceptedTaskSnapshot;
import com.yy.logistics.order.model.TransitRouteSnapshot;
import com.yy.logistics.order.model.WaybillSnapshot;
import com.yy.logistics.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrackingService {

    private static final int SPEED_KM_PER_HOUR = 100;
    private static final double SPEED_KM_PER_SECOND = SPEED_KM_PER_HOUR / 3600D;

    private static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    private static final String STATUS_DELIVERING = "DELIVERING";
    private static final String STATUS_SIGNED = "SIGNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String EVENT_PICKED_UP = "PICKED_UP";
    private static final String EVENT_IN_TRANSIT = "IN_TRANSIT";
    private static final String EVENT_ARRIVED_PROVINCE_HUB = "ARRIVED_PROVINCE_HUB";
    private static final String EVENT_ARRIVED_CITY_HUB = "ARRIVED_CITY_HUB";
    private static final String EVENT_DELIVERING = "DELIVERING";
    private static final String EVENT_SIGNED = "SIGNED";
    private static final String EVENT_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String EVENT_WAITING_PAYMENT = "WAITING_PAYMENT";
    private static final String MUNICIPAL_PLACEHOLDER_DISTRICT = "\u5e02\u8f96\u533a";
    private static final String MUNICIPAL_PLACEHOLDER_COUNTY = "\u5e02\u8f96\u53bf";

    private static final Map<String, ProvinceMeta> PROVINCE_META = buildProvinceMeta();
    private static final Map<String, String> PROVINCE_ALIASES = buildProvinceAliases();

    private final OrderRepository orderRepository;

    public TrackingService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public TrackingQueryResponse queryByWaybillNo(String waybillNo) {
        refreshAutoProgress(waybillNo);
        WaybillSnapshot waybill = orderRepository.findWaybillByNo(waybillNo)
                .orElseThrow(() -> new BizException(ErrorCode.WAYBILL_NOT_FOUND));
        List<TrackingEventResponse> events = orderRepository.findTrackingEvents(waybillNo);
        return new TrackingQueryResponse(waybill.waybillNo(), waybill.currentStatus(), events);
    }

    public TrackingProgressResponse queryProgressByWaybillNo(String waybillNo) {
        refreshAutoProgress(waybillNo);

        TransitRouteSnapshot routeSnapshot = orderRepository.findTransitRouteByWaybillNo(waybillNo)
                .orElseThrow(() -> new BizException(ErrorCode.WAYBILL_NOT_FOUND));

        AcceptedTaskSnapshot task = orderRepository.findAcceptedTaskByWaybillNo(waybillNo).orElse(null);
        RoutePlan plan = buildRoutePlan(routeSnapshot, task == null ? null : task.acceptedAt());
        String phase = resolvePhaseByStatus(routeSnapshot.currentStatus(), plan.phase());

        return new TrackingProgressResponse(
                waybillNo,
                phase,
                plan.progress(),
                round(plan.totalDistanceKm()),
                round(plan.travelledKm()),
                SPEED_KM_PER_HOUR,
                SPEED_KM_PER_SECOND,
                plan.elapsedSeconds(),
                plan.totalSeconds(),
                plan.acceptedAt(),
                plan.arriveProvinceHubAt(),
                plan.arriveCityHubAt(),
                plan.signedAt(),
                plan.senderProvince(),
                plan.senderCity(),
                plan.senderAddr(),
                plan.receiverProvince(),
                plan.receiverCity(),
                plan.receiverDistrict(),
                plan.receiverAddr(),
                plan.receiverCapitalCity(),
                plan.routeNodeNames(),
                toResponse(plan.routePoints().get(0)),
                toResponse(plan.routePoints().get(plan.routePoints().size() - 1)),
                toResponse(plan.currentPoint()),
                plan.routePoints().stream().map(this::toResponse).toList()
        );
    }

    public void refreshAcceptedTasksByCourierId(Long courierId) {
        List<String> waybillNos = orderRepository.findAcceptedWaybillNosByCourierId(courierId);
        for (String waybillNo : waybillNos) {
            refreshAutoProgress(waybillNo);
        }
    }

    public void refreshAutoProgress(String waybillNo) {
        TransitRouteSnapshot routeSnapshot = orderRepository.findTransitRouteByWaybillNo(waybillNo).orElse(null);
        if (routeSnapshot == null) {
            return;
        }
        if (STATUS_CANCELLED.equals(routeSnapshot.currentStatus()) || STATUS_SIGNED.equals(routeSnapshot.currentStatus())) {
            return;
        }

        AcceptedTaskSnapshot task = orderRepository.findAcceptedTaskByWaybillNo(waybillNo).orElse(null);
        if (task == null || task.acceptedAt() == null) {
            return;
        }

        RoutePlan plan = buildRoutePlan(routeSnapshot, task.acceptedAt());
        LocalDateTime now = LocalDateTime.now();

        ensureEvent(waybillNo, EVENT_PICKED_UP, task.acceptedAt(), task.courierId(), "快递员已接单，准备发运");
        ensureEvent(waybillNo, EVENT_IN_TRANSIT, task.acceptedAt().plusSeconds(1), task.courierId(), "干线运输已开始，车辆在途");

        if (now.isBefore(plan.arriveProvinceHubAt())) {
            orderRepository.updateOrderStatusByWaybillNo(waybillNo, STATUS_IN_TRANSIT);
            orderRepository.updateWaybillStatusByWaybillNo(waybillNo, STATUS_IN_TRANSIT);
            return;
        }

        ensureEvent(
                waybillNo,
                EVENT_ARRIVED_PROVINCE_HUB,
                plan.arriveProvinceHubAt(),
                task.courierId(),
                "已到达收货省会站：" + plan.routeNodeNames().get(1)
        );

        if (now.isBefore(plan.arriveCityHubAt())) {
            orderRepository.updateOrderStatusByWaybillNo(waybillNo, STATUS_IN_TRANSIT);
            orderRepository.updateWaybillStatusByWaybillNo(waybillNo, STATUS_IN_TRANSIT);
            return;
        }

        ensureEvent(
                waybillNo,
                EVENT_ARRIVED_CITY_HUB,
                plan.arriveCityHubAt(),
                task.courierId(),
                "已到达地级市站：" + plan.routeNodeNames().get(2)
        );
        ensureEvent(
                waybillNo,
                EVENT_DELIVERING,
                plan.arriveCityHubAt().plusSeconds(1),
                task.courierId(),
                "快递员出站派送中"
        );

        orderRepository.updateOrderStatusByWaybillNo(waybillNo, STATUS_DELIVERING);
        orderRepository.updateWaybillStatusByWaybillNo(waybillNo, STATUS_DELIVERING);
        if (now.isBefore(plan.signedAt())) {
            return;
        }

        if (routeSnapshot.payType() != null && routeSnapshot.payType() == 2) {
            return;
        }

        if (!orderRepository.existsTrackingEventByType(waybillNo, EVENT_PAYMENT_CONFIRMED)) {
            ensureEvent(
                    waybillNo,
                    EVENT_WAITING_PAYMENT,
                    plan.signedAt(),
                    task.courierId(),
                    "在线支付未完成，暂不可签收"
            );
            return;
        }

        ensureEvent(waybillNo, EVENT_SIGNED, LocalDateTime.now(), task.courierId(), "收件人已签收");
        orderRepository.updateOrderStatusByWaybillNo(waybillNo, STATUS_SIGNED);
        orderRepository.updateWaybillStatusByWaybillNo(waybillNo, STATUS_SIGNED);
        orderRepository.finishAcceptedTaskByWaybillNo(waybillNo);
    }

    private void ensureEvent(String waybillNo, String eventType, LocalDateTime plannedTime, Long courierId, String description) {
        if (orderRepository.existsTrackingEventByType(waybillNo, eventType)) {
            return;
        }

        LocalDateTime eventTime = plannedTime;
        while (orderRepository.existsTrackingEvent(waybillNo, eventType, eventTime)) {
            eventTime = eventTime.plusSeconds(1);
        }

        orderRepository.insertTrackingEvent(waybillNo, eventTime, eventType, null, courierId, description);
    }

    private RoutePlan buildRoutePlan(TransitRouteSnapshot routeSnapshot, LocalDateTime acceptedAt) {
        AddressParts sender = parseAddress(routeSnapshot.senderAddr());
        AddressParts receiver = parseAddress(routeSnapshot.receiverAddr());

        ProvinceMeta senderMeta = PROVINCE_META.getOrDefault(sender.province(), PROVINCE_META.get("重庆市"));
        ProvinceMeta receiverMeta = PROVINCE_META.getOrDefault(receiver.province(), PROVINCE_META.get("重庆市"));

        GeoPoint senderPoint = pointForDistrict(senderMeta, sender.city(), sender.district());
        GeoPoint provinceHubPoint = receiverMeta.capitalPoint();
        GeoPoint cityHubPoint = pointForCity(receiverMeta, receiver.city());
        GeoPoint receiverPoint = pointForDistrict(receiverMeta, receiver.city(), receiver.district());

        List<GeoPoint> routePoints = List.of(senderPoint, provinceHubPoint, cityHubPoint, receiverPoint);
        List<String> routeNodeNames = List.of(
                sender.city() + "发货站",
                receiverMeta.capitalCity() + "省会站",
                receiver.city() + "地级市站",
                receiver.district() + "收货点"
        );

        double[] cumulative = buildCumulativeDistance(routePoints);
        double totalDistance = cumulative[cumulative.length - 1];
        long totalSeconds = Math.max(1L, (long) Math.ceil(totalDistance / SPEED_KM_PER_SECOND));
        long provinceHubSeconds = Math.max(1L, (long) Math.ceil(cumulative[1] / SPEED_KM_PER_SECOND));
        long cityHubSeconds = Math.max(1L, (long) Math.ceil(cumulative[2] / SPEED_KM_PER_SECOND));

        String phase = "WAITING_ACCEPT";
        double progress = 0D;
        long elapsedSeconds = 0L;
        double travelledKm = 0D;
        GeoPoint currentPoint = senderPoint;

        LocalDateTime safeAcceptedAt = acceptedAt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arriveProvinceHubAt = safeAcceptedAt == null ? now : safeAcceptedAt.plusSeconds(provinceHubSeconds);
        LocalDateTime arriveCityHubAt = safeAcceptedAt == null ? now : safeAcceptedAt.plusSeconds(cityHubSeconds);
        LocalDateTime signedAt = safeAcceptedAt == null ? now : safeAcceptedAt.plusSeconds(totalSeconds);

        if (safeAcceptedAt != null) {
            elapsedSeconds = Math.max(0L, Duration.between(safeAcceptedAt, now).getSeconds());
            travelledKm = Math.min(totalDistance, elapsedSeconds * SPEED_KM_PER_SECOND);
            currentPoint = pointOnRoute(routePoints, cumulative, travelledKm);
            progress = totalDistance <= 0D ? 1D : clamp(travelledKm / totalDistance, 0D, 1D);

            if (travelledKm >= totalDistance) {
                phase = STATUS_SIGNED;
            } else if (travelledKm >= cumulative[2]) {
                phase = STATUS_DELIVERING;
            } else {
                phase = STATUS_IN_TRANSIT;
            }
        }

        return new RoutePlan(
                sender.province(),
                sender.city(),
                routeSnapshot.senderAddr(),
                receiver.province(),
                receiver.city(),
                receiver.district(),
                routeSnapshot.receiverAddr(),
                receiverMeta.capitalCity(),
                routePoints,
                routeNodeNames,
                currentPoint,
                totalDistance,
                travelledKm,
                progress,
                phase,
                elapsedSeconds,
                totalSeconds,
                safeAcceptedAt,
                arriveProvinceHubAt,
                arriveCityHubAt,
                signedAt
        );
    }

    private AddressParts parseAddress(String rawAddress) {
        if (!StringUtils.hasText(rawAddress)) {
            return new AddressParts("重庆市", "重庆市", "渝北区");
        }

        String normalized = rawAddress
                .replace('，', ' ')
                .replace(',', ' ')
                .replace('/', ' ')
                .trim();
        String[] tokens = normalized.split("\\s+");

        String province = tokens.length > 0 ? normalizeProvince(tokens[0]) : "重庆市";
        ProvinceMeta provinceMeta = PROVINCE_META.getOrDefault(province, PROVINCE_META.get("重庆市"));

        String city = tokens.length > 1 && StringUtils.hasText(tokens[1])
                ? tokens[1].trim()
                : provinceMeta.capitalCity();
        if (isMunicipalPlaceholder(city)) {
            city = provinceMeta.capitalCity();
        }
        String district = tokens.length > 2 && StringUtils.hasText(tokens[2])
                ? tokens[2].trim()
                : city + "主城区";
        if (isMunicipalPlaceholder(district)) {
            district = city + "主城区";
        }

        return new AddressParts(province, city, district);
    }

    private boolean isMunicipalPlaceholder(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String trimmed = text.trim().replace(" ", "");
        return MUNICIPAL_PLACEHOLDER_DISTRICT.equals(trimmed)
                || MUNICIPAL_PLACEHOLDER_COUNTY.equals(trimmed)
                || trimmed.startsWith(MUNICIPAL_PLACEHOLDER_DISTRICT)
                || trimmed.startsWith(MUNICIPAL_PLACEHOLDER_COUNTY);
    }

    private String normalizeProvince(String text) {
        if (!StringUtils.hasText(text)) {
            return "重庆市";
        }
        String trimmed = text.trim();
        for (Map.Entry<String, String> entry : PROVINCE_ALIASES.entrySet()) {
            if (trimmed.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "重庆市";
    }

    private GeoPoint pointForCity(ProvinceMeta provinceMeta, String city) {
        if (provinceMeta.capitalCity().equals(city)) {
            return provinceMeta.capitalPoint();
        }

        int hash = Math.abs((provinceMeta.province() + "#" + city).hashCode());
        double latOffset = ((hash % 2001) - 1000) / 10000D;
        double lngOffset = (((hash / 2001) % 2001) - 1000) / 10000D;
        return new GeoPoint(
                provinceMeta.capitalPoint().lat() + latOffset,
                provinceMeta.capitalPoint().lng() + lngOffset
        );
    }

    private GeoPoint pointForDistrict(ProvinceMeta provinceMeta, String city, String district) {
        GeoPoint cityPoint = pointForCity(provinceMeta, city);
        int hash = Math.abs((provinceMeta.province() + "#" + city + "#" + district).hashCode());
        double latOffset = ((hash % 801) - 400) / 20000D;
        double lngOffset = (((hash / 801) % 801) - 400) / 20000D;
        return new GeoPoint(cityPoint.lat() + latOffset, cityPoint.lng() + lngOffset);
    }

    private double[] buildCumulativeDistance(List<GeoPoint> points) {
        double[] cumulative = new double[points.size()];
        cumulative[0] = 0D;
        for (int i = 1; i < points.size(); i++) {
            GeoPoint prev = points.get(i - 1);
            GeoPoint curr = points.get(i);
            cumulative[i] = cumulative[i - 1] + haversineKm(prev.lat(), prev.lng(), curr.lat(), curr.lng());
        }
        return cumulative;
    }

    private GeoPoint pointOnRoute(List<GeoPoint> points, double[] cumulative, double travelledKm) {
        if (travelledKm <= 0D) {
            return points.get(0);
        }
        if (travelledKm >= cumulative[cumulative.length - 1]) {
            return points.get(points.size() - 1);
        }

        for (int i = 1; i < points.size(); i++) {
            if (travelledKm <= cumulative[i]) {
                double segmentStart = cumulative[i - 1];
                double segmentEnd = cumulative[i];
                double rate = segmentEnd <= segmentStart ? 1D : (travelledKm - segmentStart) / (segmentEnd - segmentStart);
                return interpolate(points.get(i - 1), points.get(i), clamp(rate, 0D, 1D));
            }
        }
        return points.get(points.size() - 1);
    }

    private GeoPoint interpolate(GeoPoint start, GeoPoint end, double rate) {
        return new GeoPoint(
                start.lat() + (end.lat() - start.lat()) * rate,
                start.lng() + (end.lng() - start.lng()) * rate
        );
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 1000D) / 1000D;
    }

    private String resolvePhaseByStatus(String currentStatus, String fallbackPhase) {
        if (!StringUtils.hasText(currentStatus)) {
            return fallbackPhase;
        }
        return switch (currentStatus) {
            case STATUS_CANCELLED -> STATUS_CANCELLED;
            case STATUS_SIGNED -> STATUS_SIGNED;
            case STATUS_DELIVERING -> STATUS_DELIVERING;
            case STATUS_IN_TRANSIT -> STATUS_IN_TRANSIT;
            default -> fallbackPhase;
        };
    }

    private TrackingGeoPointResponse toResponse(GeoPoint point) {
        return new TrackingGeoPointResponse(point.lat(), point.lng());
    }

    private static Map<String, String> buildProvinceAliases() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ProvinceMeta meta : PROVINCE_META.values()) {
            map.put(meta.province(), meta.province());
            map.put(meta.shortName(), meta.province());
        }
        return map;
    }

    private static Map<String, ProvinceMeta> buildProvinceMeta() {
        Map<String, ProvinceMeta> map = new LinkedHashMap<>();

        addProvince(map, "北京市", "北京", "北京市", 39.9042, 116.4074);
        addProvince(map, "天津市", "天津", "天津市", 39.3434, 117.3616);
        addProvince(map, "上海市", "上海", "上海市", 31.2304, 121.4737);
        addProvince(map, "重庆市", "重庆", "重庆市", 29.5630, 106.5516);

        addProvince(map, "河北省", "河北", "石家庄市", 38.0428, 114.5149);
        addProvince(map, "山西省", "山西", "太原市", 37.8706, 112.5489);
        addProvince(map, "辽宁省", "辽宁", "沈阳市", 41.8057, 123.4315);
        addProvince(map, "吉林省", "吉林", "长春市", 43.8160, 125.3235);
        addProvince(map, "黑龙江省", "黑龙江", "哈尔滨市", 45.8038, 126.5349);
        addProvince(map, "江苏省", "江苏", "南京市", 32.0603, 118.7969);
        addProvince(map, "浙江省", "浙江", "杭州市", 30.2741, 120.1551);
        addProvince(map, "安徽省", "安徽", "合肥市", 31.8206, 117.2272);
        addProvince(map, "福建省", "福建", "福州市", 26.0745, 119.2965);
        addProvince(map, "江西省", "江西", "南昌市", 28.6820, 115.8579);
        addProvince(map, "山东省", "山东", "济南市", 36.6512, 117.1201);
        addProvince(map, "河南省", "河南", "郑州市", 34.7473, 113.6249);
        addProvince(map, "湖北省", "湖北", "武汉市", 30.5928, 114.3055);
        addProvince(map, "湖南省", "湖南", "长沙市", 28.2282, 112.9388);
        addProvince(map, "广东省", "广东", "广州市", 23.1291, 113.2644);
        addProvince(map, "海南省", "海南", "海口市", 20.0440, 110.1999);
        addProvince(map, "四川省", "四川", "成都市", 30.5728, 104.0668);
        addProvince(map, "贵州省", "贵州", "贵阳市", 26.6470, 106.6302);
        addProvince(map, "云南省", "云南", "昆明市", 25.0389, 102.7183);
        addProvince(map, "陕西省", "陕西", "西安市", 34.3416, 108.9398);
        addProvince(map, "甘肃省", "甘肃", "兰州市", 36.0611, 103.8343);
        addProvince(map, "青海省", "青海", "西宁市", 36.6171, 101.7782);
        addProvince(map, "台湾省", "台湾", "台北市", 25.0330, 121.5654);

        addProvince(map, "内蒙古自治区", "内蒙古", "呼和浩特市", 40.8426, 111.7492);
        addProvince(map, "广西壮族自治区", "广西", "南宁市", 22.8170, 108.3669);
        addProvince(map, "西藏自治区", "西藏", "拉萨市", 29.6525, 91.1721);
        addProvince(map, "宁夏回族自治区", "宁夏", "银川市", 38.4872, 106.2309);
        addProvince(map, "新疆维吾尔自治区", "新疆", "乌鲁木齐市", 43.8256, 87.6168);

        addProvince(map, "香港特别行政区", "香港", "香港", 22.3193, 114.1694);
        addProvince(map, "澳门特别行政区", "澳门", "澳门", 22.1987, 113.5439);

        return map;
    }

    private static void addProvince(
            Map<String, ProvinceMeta> map,
            String province,
            String shortName,
            String capitalCity,
            double lat,
            double lng
    ) {
        map.put(province, new ProvinceMeta(province, shortName, capitalCity, new GeoPoint(lat, lng)));
    }

    private record GeoPoint(double lat, double lng) {
    }

    private record ProvinceMeta(String province, String shortName, String capitalCity, GeoPoint capitalPoint) {
    }

    private record AddressParts(String province, String city, String district) {
    }

    private record RoutePlan(
            String senderProvince,
            String senderCity,
            String senderAddr,
            String receiverProvince,
            String receiverCity,
            String receiverDistrict,
            String receiverAddr,
            String receiverCapitalCity,
            List<GeoPoint> routePoints,
            List<String> routeNodeNames,
            GeoPoint currentPoint,
            double totalDistanceKm,
            double travelledKm,
            double progress,
            String phase,
            long elapsedSeconds,
            long totalSeconds,
            LocalDateTime acceptedAt,
            LocalDateTime arriveProvinceHubAt,
            LocalDateTime arriveCityHubAt,
            LocalDateTime signedAt
    ) {
    }
}
