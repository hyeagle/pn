package sensor.kircheis.top.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sensor.kircheis.top.po.SensorConfig;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<SensorConfig> ROW_MAPPER = (rs, rowNum) -> SensorConfig.builder()
            .sensorId(rs.getString("sensor_id"))
            .deviceId(rs.getString("device_id"))
            .metric(rs.getString("metric"))
            .metricName(rs.getString("metric_name"))
            .unit(rs.getString("unit"))
            .createTime(rs.getObject("create_time", Long.class))
            .build();

    // ---------- 增 ----------

    public int insert(SensorConfig config) {
        String sql = "INSERT INTO sensor_config (sensor_id, device_id, metric, metric_name, unit, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                config.getSensorId(),
                config.getDeviceId(),
                config.getMetric(),
                config.getMetricName(),
                config.getUnit(),
                config.getCreateTime());
    }

    public int[][] batchInsert(List<SensorConfig> configs) {
        String sql = "INSERT INTO sensor_config (sensor_id, device_id, metric, metric_name, unit, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.batchUpdate(sql, configs, configs.size(),
                (ps, config) -> {
                    ps.setString(1, config.getSensorId());
                    ps.setString(2, config.getDeviceId());
                    ps.setString(3, config.getMetric());
                    ps.setString(4, config.getMetricName());
                    ps.setString(5, config.getUnit());
                    ps.setLong(6, config.getCreateTime());
                });
    }

    // ---------- 删 ----------

    public int deleteById(String sensorId) {
        String sql = "DELETE FROM sensor_config WHERE sensor_id = ?";
        return jdbcTemplate.update(sql, sensorId);
    }

    public int deleteByDeviceId(String deviceId) {
        String sql = "DELETE FROM sensor_config WHERE device_id = ?";
        return jdbcTemplate.update(sql, deviceId);
    }

    // ---------- 改 ----------

    public int update(SensorConfig config) {
        String sql = "UPDATE sensor_config SET device_id = ?, metric = ?, metric_name = ?, unit = ?, create_time = ? WHERE sensor_id = ?";
        return jdbcTemplate.update(sql,
                config.getDeviceId(),
                config.getMetric(),
                config.getMetricName(),
                config.getUnit(),
                config.getCreateTime(),
                config.getSensorId());
    }

    // ---------- 查 ----------

    public Optional<SensorConfig> findById(String sensorId) {
        String sql = "SELECT * FROM sensor_config WHERE sensor_id = ?";
        List<SensorConfig> list = jdbcTemplate.query(sql, ROW_MAPPER, sensorId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<SensorConfig> findByDeviceId(String deviceId) {
        String sql = "SELECT * FROM sensor_config WHERE device_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId);
    }

    public Optional<SensorConfig> findByDeviceAndMetric(String deviceId, String metric) {
        String sql = "SELECT * FROM sensor_config WHERE device_id = ? AND metric = ?";
        List<SensorConfig> list = jdbcTemplate.query(sql, ROW_MAPPER, deviceId, metric);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<SensorConfig> findAll() {
        String sql = "SELECT * FROM sensor_config ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public boolean existsById(String sensorId) {
        String sql = "SELECT COUNT(*) FROM sensor_config WHERE sensor_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sensorId);
        return count != null && count > 0;
    }
}
