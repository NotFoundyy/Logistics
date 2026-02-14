package com.yy.logistics.order.controller;

import com.yy.logistics.common.api.ApiResponse;
import com.yy.logistics.order.dto.TrackingProgressResponse;
import com.yy.logistics.order.dto.TrackingQueryResponse;
import com.yy.logistics.order.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Tracking", description = "物流轨迹接口")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Operation(summary = "按运单号查询轨迹")
    @GetMapping("/{waybillNo}")
    public ApiResponse<TrackingQueryResponse> query(@PathVariable("waybillNo") String waybillNo) {
        return ApiResponse.success(trackingService.queryByWaybillNo(waybillNo));
    }

    @Operation(summary = "按运单号查询地图进度")
    @GetMapping("/{waybillNo}/progress")
    public ApiResponse<TrackingProgressResponse> progress(@PathVariable("waybillNo") String waybillNo) {
        return ApiResponse.success(trackingService.queryProgressByWaybillNo(waybillNo));
    }
}
