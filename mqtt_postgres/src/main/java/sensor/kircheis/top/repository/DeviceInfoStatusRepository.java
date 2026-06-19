package sensor.kircheis.top.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sensor.kircheis.top.po.DeviceInfoStatus;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceInfoStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DeviceInfoStatus> ROW_MAPPER = (rs, rowNum) -> DeviceInfoStatus.builder()
            .deviceId(rs.getString("device_id"))
            .statusKey(rs.getString("status_key"))
            .status(rs.getObject("status", Boolean.class))
            .updateTime(rs.getObject("update_time", Long.class))
            .build();

    /**
     * 插入或更新单条设备状态（upsert）
     */
    public int upsert(String deviceId, String statusKey, boolean status) {
        long now = System.currentTimeMillis();
        String sql = "INSERT INTO device_info_status (device_id, status_key, status, update_time) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (device_id, status_key) DO UPDATE SET status = EXCLUDED.status, update_time = EXCLUDED.update_time";
        return jdbcTemplate.update(sql, deviceId, statusKey, status, now);
    }

    /**
     * 初始化设备状态（默认写入 5v=false, bump=false）
     */
    public void init(String deviceId) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO device_info_status (device_id, status_key, status, update_time) VALUES (?, '5v', false, ?) ON CONFLICT DO NOTHING",
                deviceId, now);
        jdbcTemplate.update(
                "INSERT INTO device_info_status (device_id, status_key, status, update_time) VALUES (?, 'bump', false, ?) ON CONFLICT DO NOTHING",
                deviceId, now);
    }

    /**
     * 查询设备所有状态
     */
    public List<DeviceInfoStatus> findByDeviceId(String deviceId) {
        String sql = "SELECT * FROM device_info_status WHERE device_id = ? ORDER BY status_key";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId);
    }

    /**
     * 批量查询多个设备的状态
     */
    public List<DeviceInfoStatus> findByDeviceIds(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String placeholders = String.join(",", deviceIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = "SELECT * FROM device_info_status WHERE device_id IN (" + placeholders + ") ORDER BY device_id, status_key";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceIds.toArray());
    }

    /**
     * 删除设备所有状态
     */
    public int deleteByDeviceId(String deviceId) {
        String sql = "DELETE FROM device_info_status WHERE device_id = ?";
        return jdbcTemplate.update(sql, deviceId);
    }
}
