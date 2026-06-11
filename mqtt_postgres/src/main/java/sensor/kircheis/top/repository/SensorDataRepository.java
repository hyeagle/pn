package sensor.kircheis.top.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sensor.kircheis.top.po.SensorDataRecord;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PARENT_TABLE = "sensor_data";

    private static final RowMapper<SensorDataRecord> ROW_MAPPER = (rs, rowNum) -> {
        SensorDataRecord r = SensorDataRecord.builder()
                .id(rs.getLong("id"))
                .deviceId(rs.getString("device_id"))
                .sensorId(rs.getString("sensor_id"))
                .metric(rs.getString("metric"))
                .value(rs.getBigDecimal("value"))
                .ts(rs.getLong("ts"))
                .build();
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) {
            r.setCreateTime(ct.toLocalDateTime());
        }
        return r;
    };

    // ---------- 动态表名 ----------

    private String tableName(LocalDate date) {
        return PARENT_TABLE + "_" + date.format(DATE_FORMATTER);
    }

    // ---------- 增 ----------

    /**
     * 插入一条数据到指定日期的分区表
     */
    public int insert(SensorDataRecord record, LocalDate date) {
        String sql = "INSERT INTO " + tableName(date) +
                " (device_id, sensor_id, metric, value, ts) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                record.getDeviceId(),
                record.getSensorId(),
                record.getMetric(),
                record.getValue(),
                record.getTs());
    }

    /**
     * 批量插入到指定日期的分区表
     */
    public int[][] batchInsert(List<SensorDataRecord> records, LocalDate date) {
        String sql = "INSERT INTO " + tableName(date) +
                " (device_id, sensor_id, metric, value, ts) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.batchUpdate(sql, records, records.size(),
                (ps, r) -> {
                    ps.setString(1, r.getDeviceId());
                    ps.setString(2, r.getSensorId());
                    ps.setString(3, r.getMetric());
                    ps.setBigDecimal(4, r.getValue());
                    ps.setLong(5, r.getTs());
                });
    }

    /**
     * 单条插入父表（由分区路由自动分配）
     */
    public int insertToParent(SensorDataRecord record) {
        String sql = "INSERT INTO " + PARENT_TABLE +
                " (device_id, sensor_id, metric, value, ts) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                record.getDeviceId(),
                record.getSensorId(),
                record.getMetric(),
                record.getValue(),
                record.getTs());
    }

    // ---------- 删 ----------

    /**
     * 删除指定日期分区中的记录
     */
    public int deleteByDeviceId(String deviceId, LocalDate date) {
        String sql = "DELETE FROM " + tableName(date) + " WHERE device_id = ?";
        return jdbcTemplate.update(sql, deviceId);
    }

    /**
     * 删除指定时间之前的记录（跨父表，由分区路由处理）
     */
    public int deleteBeforeTs(long ts) {
        String sql = "DELETE FROM " + PARENT_TABLE + " WHERE ts < ?";
        return jdbcTemplate.update(sql, ts);
    }

    // ---------- 查 ----------

    /**
     * 查询指定日期分区的传感器数据
     */
    public List<SensorDataRecord> findByDeviceId(String deviceId, LocalDate date) {
        String sql = "SELECT * FROM " + tableName(date) + " WHERE device_id = ? ORDER BY ts DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId);
    }

    /**
     * 按设备和指标查询指定日期的数据
     */
    public List<SensorDataRecord> findByDeviceAndMetric(String deviceId, String metric, LocalDate date, int limit) {
        String sql = "SELECT * FROM " + tableName(date) +
                " WHERE device_id = ? AND metric = ? ORDER BY ts DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId, metric, limit);
    }

    /**
     * 按传感器ID查询指定日期的数据
     */
    public List<SensorDataRecord> findBySensorId(String sensorId, LocalDate date, int limit) {
        String sql = "SELECT * FROM " + tableName(date) +
                " WHERE sensor_id = ? ORDER BY ts DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, sensorId, limit);
    }

    /**
     * 时间范围查询（在指定日期的分区内）
     */
    public List<SensorDataRecord> findByTimeRange(String deviceId, LocalDate date, long startTs, long endTs) {
        String sql = "SELECT * FROM " + tableName(date) +
                " WHERE device_id = ? AND ts >= ? AND ts <= ? ORDER BY ts DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId, startTs, endTs);
    }

    /**
     * 跨天时间范围查询（走父表）
     */
    public List<SensorDataRecord> findByTimeRangeCrossDay(String deviceId, long startTs, long endTs, int limit) {
        String sql = "SELECT * FROM " + PARENT_TABLE +
                " WHERE device_id = ? AND ts >= ? AND ts <= ? ORDER BY ts DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, deviceId, startTs, endTs, limit);
    }

    /**
     * 获取指定日期的数据量
     */
    public long countByDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM " + tableName(date);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }
}
