package sensor.kircheis.top.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GpsTrackRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 插入或更新 GPS 轨迹点，以 (device_id, record_ts) 为主键
     */
    public int upsert(String deviceId, long recordTs,
                      BigDecimal latitude, BigDecimal longitude) {
        String sql = "INSERT INTO gps_track (device_id, record_ts, latitude, longitude) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (device_id, record_ts) DO UPDATE SET " +
                     "latitude = EXCLUDED.latitude, " +
                     "longitude = EXCLUDED.longitude";
        return jdbcTemplate.update(sql,
                deviceId,
                recordTs,
                latitude,
                longitude);
    }
}
