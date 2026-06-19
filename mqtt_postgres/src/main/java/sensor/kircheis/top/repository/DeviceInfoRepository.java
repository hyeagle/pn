package sensor.kircheis.top.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sensor.kircheis.top.po.DeviceInfo;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DeviceInfo> ROW_MAPPER = (rs, rowNum) -> DeviceInfo.builder()
            .deviceId(rs.getString("device_id"))
            .userId(rs.getString("user_id"))
            .deviceName(rs.getString("device_name"))
            .battery(rs.getObject("battery", Integer.class))
            .updateTime(rs.getObject("update_time", Long.class))
            .build();

    // ---------- 增 ----------

    public int insert(DeviceInfo device) {
        String sql = "INSERT INTO device_info (device_id, user_id, device_name, battery, update_time) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                device.getDeviceId(),
                device.getUserId(),
                device.getDeviceName(),
                device.getBattery(),
                device.getUpdateTime());
    }

    // ---------- 删 ----------

    public int deleteById(String deviceId) {
        String sql = "DELETE FROM device_info WHERE device_id = ?";
        return jdbcTemplate.update(sql, deviceId);
    }

    public int deleteByUserId(String userId) {
        String sql = "DELETE FROM device_info WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }

    // ---------- 改 ----------

    public int update(DeviceInfo device) {
        String sql = "UPDATE device_info SET user_id = ?, device_name = ?, battery = ?, update_time = ? WHERE device_id = ?";
        return jdbcTemplate.update(sql,
                device.getUserId(),
                device.getDeviceName(),
                device.getBattery(),
                device.getUpdateTime(),
                device.getDeviceId());
    }

    /**
     * 更新设备电量
     */
    public int updateBattery(String deviceId, int battery) {
        String sql = "UPDATE device_info SET battery = ? WHERE device_id = ?";
        return jdbcTemplate.update(sql, battery, deviceId);
    }

    // ---------- 查 ----------

    public Optional<DeviceInfo> findById(String deviceId) {
        String sql = "SELECT * FROM device_info WHERE device_id = ?";
        List<DeviceInfo> list = jdbcTemplate.query(sql, ROW_MAPPER, deviceId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<DeviceInfo> findByUserId(String userId) {
        String sql = "SELECT * FROM device_info WHERE user_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    public List<DeviceInfo> findAll() {
        String sql = "SELECT * FROM device_info ORDER BY update_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public boolean existsById(String deviceId) {
        String sql = "SELECT COUNT(*) FROM device_info WHERE device_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, deviceId);
        return count != null && count > 0;
    }

    // ---------- 分页 ----------

    /**
     * 分页查询所有设备，按更新时间降序
     */
    public List<DeviceInfo> findPage(int limit, int offset) {
        String sql = "SELECT * FROM device_info ORDER BY update_time DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, limit, offset);
    }

    /**
     * 统计设备总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM device_info";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
