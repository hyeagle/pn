package sensor.kircheis.top.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sensor.kircheis.top.mqtt.MqttPublisher;
import sensor.kircheis.top.po.DeviceInfo;
import sensor.kircheis.top.repository.DeviceInfoRepository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceInfoRepository deviceInfoRepository;
    private final MqttPublisher mqttPublisher;

    /**
     * 分页查询设备列表
     */
    public PageResult<DeviceInfo> getDevicesByPage(int page, int size) {
        int total = deviceInfoRepository.count();
        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0, page, size, 0);
        }

        int totalPages = (int) Math.ceil((double) total / size);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;
        List<DeviceInfo> list = deviceInfoRepository.findPage(size, offset);

        return new PageResult<>(list, total, page, size, totalPages);
    }

    /**
     * 添加设备
     */
    public DeviceInfo addDevice(DeviceInfo device) {
        if (device.getDeviceId() == null || device.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        if (deviceInfoRepository.existsById(device.getDeviceId())) {
            throw new IllegalArgumentException("设备ID已存在: " + device.getDeviceId());
        }
        if (device.getUpdateTime() == null) {
            device.setUpdateTime(System.currentTimeMillis());
        }
        // 默认 relay 和 power 都为 false
        device.setRelay(false);
        device.setPower(false);
        deviceInfoRepository.insert(device);
        log.info("添加设备: {}", device.getDeviceId());
        return device;
    }

    /**
     * 更新设备信息
     */
    public DeviceInfo updateDevice(String deviceId, DeviceInfo device) {
        if (!deviceInfoRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }
        device.setDeviceId(deviceId);
        device.setUpdateTime(System.currentTimeMillis());
        deviceInfoRepository.update(device);
        log.info("更新设备: {}", deviceId);
        return device;
    }

    /**
     * 控制设备 - 发送 relay/power 指令
     */
    public boolean controlDevice(String deviceId, Boolean relay, Boolean power) {
        if (!deviceInfoRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }

        StringBuilder json = new StringBuilder("{");
        if (relay != null) {
            json.append("\"relay\":").append(relay);
        }
        if (power != null) {
            if (relay != null) json.append(",");
            json.append("\"power\":").append(power);
        }
        json.append("}");

        String payload = json.toString();
        boolean success = mqttPublisher.publish(payload);
        if (success) {
            log.info("设备控制指令已发送: deviceId={}, payload={}", deviceId, payload);
            // 同步更新数据库中的 relay/power 状态
            if (relay != null) {
                deviceInfoRepository.updateRelay(deviceId, relay);
            }
            if (power != null) {
                deviceInfoRepository.updatePower(deviceId, power);
            }
        }
        return success;
    }
}
