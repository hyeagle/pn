package sensor.kircheis.top.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sensor.kircheis.top.dto.request.ControlRequest;
import sensor.kircheis.top.dto.request.DeviceRequest;
import sensor.kircheis.top.dto.response.DeviceResponse;
import sensor.kircheis.top.service.DeviceService;
import sensor.kircheis.top.service.PageResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 分页查询设备列表
     * GET /api/devices?page=1&size=10
     */
    @GetMapping("/devices")
    public PageResult<DeviceResponse> getDevices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return deviceService.getDevicesByPage(page, size);
    }

    /**
     * 添加设备
     * POST /api/devices
     * Body: { "deviceId": "xxx", "userId": "xxx", "deviceName": "xxx", "battery": 100 }
     */
    @PostMapping("/devices")
    public ResponseEntity<?> addDevice(@RequestBody DeviceRequest request) {
        try {
            DeviceResponse result = deviceService.addDevice(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorMap(e.getMessage()));
        }
    }

    /**
     * 更新设备信息
     * PUT /api/devices
     */
    @PutMapping("/devices")
    public ResponseEntity<?> updateDevice(@RequestBody DeviceRequest request) {
        try {
            DeviceResponse result = deviceService.updateDevice(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorMap(e.getMessage()));
        }
    }

    /**
     * 控制设备 - 发送 5v/bump/sys_restart 指令
     * POST /api/devices/{deviceId}/control
     * Body: { "5v": true, "bump": false, "sys_restart": false }
     */
    @PostMapping("/devices/{deviceId}/control")
    public ResponseEntity<?> controlDevice(
            @PathVariable String deviceId,
            @RequestBody ControlRequest command) {
        try {
            Boolean fiveV = command.getFiveV();
            Boolean bump = command.getBump();
            Boolean sysRestart = command.getSysRestart();

            if (fiveV == null && bump == null && sysRestart == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(errorMap("请至少提供 5v、bump 或 sys_restart 字段"));
            }

            boolean success = deviceService.controlDevice(deviceId, fiveV, bump, sysRestart);
            return ResponseEntity.ok(successMap(success, success ? "指令已发送" : "指令发送失败"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorMap(e.getMessage()));
        }
    }

    // -------- Java 8 兼容工具方法 --------

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }

    private Map<String, Object> successMap(boolean success, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        return map;
    }
}
