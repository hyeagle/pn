package sensor.kircheis.top.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sensor.kircheis.top.dto.ControlPayload;
import sensor.kircheis.top.dto.request.DeviceRequest;
import sensor.kircheis.top.dto.response.DeviceResponse;
import sensor.kircheis.top.mqtt.MqttPublisher;
import sensor.kircheis.top.po.DeviceInfo;
import sensor.kircheis.top.po.DeviceInfoStatus;
import sensor.kircheis.top.repository.DeviceInfoRepository;
import sensor.kircheis.top.repository.DeviceInfoStatusRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceInfoRepository deviceInfoRepository;
    private final DeviceInfoStatusRepository deviceInfoStatusRepository;
    private final MqttPublisher mqttPublisher;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页查询设备列表（含状态）
     */
    public PageResult<DeviceResponse> getDevicesByPage(int page, int size) {
        int total = deviceInfoRepository.count();
        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0, page, size, 0);
        }

        int totalPages = (int) Math.ceil((double) total / size);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;
        List<DeviceInfo> list = deviceInfoRepository.findPage(size, offset);

        // 批量查询设备状态
        List<String> deviceIds = list.stream().map(DeviceInfo::getDeviceId).collect(Collectors.toList());
        Map<String, Map<String, Boolean>> statusMap = new HashMap<>();
        List<DeviceInfoStatus> statusList = deviceInfoStatusRepository.findByDeviceIds(deviceIds);
        for (DeviceInfoStatus s : statusList) {
            statusMap.computeIfAbsent(s.getDeviceId(), k -> new HashMap<>()).put(s.getStatusKey(), s.getStatus());
        }

        // 合并为 Response
        List<DeviceResponse> voList = list.stream().map(device -> {
            Map<String, Boolean> statuses = statusMap.getOrDefault(device.getDeviceId(), Collections.emptyMap());
            return DeviceResponse.builder()
                    .deviceId(device.getDeviceId())
                    .userId(device.getUserId())
                    .deviceName(device.getDeviceName())
                    .battery(device.getBattery())
                    .updateTime(device.getUpdateTime())
                    .fiveV(statuses.get("5v"))
                    .bump(statuses.get("bump"))
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size, totalPages);
    }

    /**
     * 添加设备
     */
    public DeviceResponse addDevice(DeviceRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        if (deviceInfoRepository.existsById(request.getDeviceId())) {
            throw new IllegalArgumentException("设备ID已存在: " + request.getDeviceId());
        }
        DeviceInfo device = DeviceInfo.builder()
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .deviceName(request.getDeviceName())
                .battery(request.getBattery())
                .updateTime(System.currentTimeMillis())
                .build();
        deviceInfoRepository.insert(device);
        // 初始化设备状态表
        deviceInfoStatusRepository.init(device.getDeviceId());
        log.info("添加设备: {}", device.getDeviceId());
        return toResponse(device);
    }

    /**
     * 更新设备信息
     */
    public DeviceResponse updateDevice(DeviceRequest request) {
        String deviceId = request.getDeviceId();
        if (!deviceInfoRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }
        DeviceInfo device = DeviceInfo.builder()
                .deviceId(deviceId)
                .userId(request.getUserId())
                .deviceName(request.getDeviceName())
                .battery(request.getBattery())
                .updateTime(System.currentTimeMillis())
                .build();
        deviceInfoRepository.update(device);
        log.info("更新设备: {}", deviceId);
        return toResponse(device);
    }

    private DeviceResponse toResponse(DeviceInfo device) {
        return DeviceResponse.builder()
                .deviceId(device.getDeviceId())
                .userId(device.getUserId())
                .deviceName(device.getDeviceName())
                .battery(device.getBattery())
                .updateTime(device.getUpdateTime())
                .build();
    }

    /**
     * 控制设备 - 发送 5v/bump/sys_restart 指令（sys_restart 不入库）
     */
    public boolean controlDevice(String deviceId, Boolean fiveV, Boolean bump, Boolean sysRestart) {
        if (!deviceInfoRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }

        ControlPayload payload = ControlPayload.builder()
                .deviceId(deviceId)
                .secret(deviceId)
                .fiveV(fiveV)
                .bump(bump)
                .sysRestart(sysRestart)
                .build();
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("序列化控制指令失败: deviceId={}", deviceId, e);
            return false;
        }

        boolean success = mqttPublisher.publish(json);
        if (success) {
            log.info("设备控制指令已发送: deviceId={}, payload={}", deviceId, json);
            // 同步更新 device_info_status 中的 5v/bump 状态（sys_restart 不入库）
            if (fiveV != null) {
                deviceInfoStatusRepository.upsert(deviceId, "5v", fiveV);
            }
            if (bump != null) {
                deviceInfoStatusRepository.upsert(deviceId, "bump", bump);
            }
        }
        return success;
    }
}
