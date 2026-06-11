package sensor.kircheis.top.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sensor.kircheis.top.dto.SensorData;
import sensor.kircheis.top.dto.SensorMessage;
import sensor.kircheis.top.po.SensorDataRecord;
import sensor.kircheis.top.repository.DeviceInfoRepository;
import sensor.kircheis.top.repository.SensorDataRepository;
import sensor.kircheis.top.table.DailyTableScheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataWriterService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DailyTableScheduler dailyTableScheduler;
    private final SensorDataRepository sensorDataRepository;
    private final DeviceInfoRepository deviceInfoRepository;
    private final Set<String> createdTables = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public void save(String payload) throws Exception {
        SensorMessage sensorMessage = objectMapper.readValue(payload, SensorMessage.class);
        String reason = validate(sensorMessage);
        if (reason != null) {
            log.error("Validation failed: {}, payload: {}", reason, payload);
            return;
        }
        save(sensorMessage);
    }

    public void save(SensorMessage sensorMessage) throws Exception {
        String deviceId = sensorMessage.getDevice_id();
        long ts = sensorMessage.getTimestamp();
        List<SensorData> dataList = sensorMessage.getData();

        LocalDate date = Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate();
        String tableName = "sensor_data_" + date.format(DATE_FORMATTER);

        if (createdTables.add(tableName)) {
            dailyTableScheduler.ensureTableForDate(date);
        }

        List<SensorDataRecord> records = dataList.stream()
                .map(item -> SensorDataRecord.builder()
                        .deviceId(deviceId)
                        .sensorId(item.getSensor_id())
                        .metric(item.getMetric())
                        .value(BigDecimal.valueOf(item.getValue()))
                        .ts(ts)
                        .build())
                .collect(Collectors.toList());

        sensorDataRepository.batchInsert(records, date);
        log.debug("Stored {} sensor records into {}", dataList.size(), tableName);

        Integer battery = sensorMessage.getBattery();
        if (battery != null) {
            deviceInfoRepository.updateBattery(deviceId, battery);
            log.debug("Updated battery ({}%) for device {}", battery, deviceId);
        }
    }

    public String validate(SensorMessage message) {
        if (message.getDevice_id() == null || message.getDevice_id().isEmpty()) {
            return "device_id is empty";
        }
        if (message.getTimestamp() <= 0) {
            return "timestamp is invalid (" + message.getTimestamp() + ")";
        }
        if (message.getData() == null || message.getData().isEmpty()) {
            return "data list is empty";
        }
        if (message.getBattery() < 0 || message.getBattery() > 100) {
            return "battery is invalid (" + message.getBattery() + ")";
        }
        for (SensorData d : message.getData()) {
            if (d.getSensor_id() == null || d.getSensor_id().isEmpty()) {
                return "sensor_id is empty, device_id=" + message.getDevice_id();
            }
            if (d.getMetric() == null || d.getMetric().isEmpty()) {
                return "metric is empty, device_id=" + message.getDevice_id();
            }
        }
        return null;
    }
}
