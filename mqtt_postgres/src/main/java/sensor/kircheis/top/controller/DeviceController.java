package sensor.kircheis.top.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sensor.kircheis.top.dto.ControlCommand;
import sensor.kircheis.top.po.DeviceInfo;
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
    public PageResult<DeviceInfo> getDevices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return deviceService.getDevicesByPage(page, size);
    }

    /**
     * 添加设备
     * POST /api/devices
     * Body: { "deviceId": "xxx", "userId": "xxx", "deviceName": "xxx", "relay": false, "power": false, "battery": 100 }
     */
    @PostMapping("/devices")
    public ResponseEntity<?> addDevice(@RequestBody DeviceInfo device) {
        try {
            DeviceInfo result = deviceService.addDevice(device);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorMap(e.getMessage()));
        }
    }

    /**
     * 更新设备信息
     * PUT /api/devices/{deviceId}
     */
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<?> updateDevice(
            @PathVariable String deviceId,
            @RequestBody DeviceInfo device) {
        try {
            DeviceInfo result = deviceService.updateDevice(deviceId, device);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorMap(e.getMessage()));
        }
    }

    /**
     * 控制设备 - 发送 relay/power 指令
     * POST /api/devices/{deviceId}/control
     * Body: { "relay": true, "power": false }
     */
    @PostMapping("/devices/{deviceId}/control")
    public ResponseEntity<?> controlDevice(
            @PathVariable String deviceId,
            @RequestBody ControlCommand command) {
        try {
            Boolean relay = command.getRelay();
            Boolean power = command.getPower();

            if (relay == null && power == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(errorMap("请至少提供 relay 或 power 字段"));
            }

            boolean success = deviceService.controlDevice(deviceId, relay, power);
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
