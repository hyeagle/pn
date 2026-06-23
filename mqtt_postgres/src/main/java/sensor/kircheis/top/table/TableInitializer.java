package sensor.kircheis.top.table;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        List<String> ddls = Arrays.asList(
                "CREATE TABLE IF NOT EXISTS sys_user (" +
                "user_id VARCHAR(50) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "create_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000" +
                ")",
                "CREATE TABLE IF NOT EXISTS device_info (" +
                "device_id VARCHAR(50) PRIMARY KEY, " +
                "user_id VARCHAR(50) NOT NULL, " +
                "device_name VARCHAR(100) NOT NULL, " +
                "battery INT, " +
                "update_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000" +
                ")",
                "CREATE TABLE IF NOT EXISTS device_info_status (" +
                "device_id VARCHAR(50) NOT NULL REFERENCES device_info(device_id) ON DELETE CASCADE, " +
                "status_key VARCHAR(50) NOT NULL, " +
                "status BOOLEAN DEFAULT false, " +
                "update_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000, " +
                "PRIMARY KEY (device_id, status_key)" +
                ")",
                "CREATE TABLE IF NOT EXISTS sensor_config (" +
                "sensor_id VARCHAR(50) PRIMARY KEY, " +
                "device_id VARCHAR(50) NOT NULL, " +
                "metric VARCHAR(50) NOT NULL, " +
                "metric_name VARCHAR(50) NOT NULL, " +
                "unit VARCHAR(20) NOT NULL, " +
                "create_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000" +
                ")",
                "CREATE TABLE IF NOT EXISTS gps_track ( " +
                "device_id VARCHAR(50) NOT NULL, " +
                "record_ts BIGINT NOT NULL, " +
                "latitude NUMERIC(10, 6) NOT NULL, " +
                "longitude NUMERIC(11, 6) NOT NULL, " +
                "PRIMARY KEY (device_id, record_ts) " +
                ")"
        );

        for (String ddl : ddls) {
            try {
                jdbcTemplate.execute(ddl);
            } catch (Exception e) {
                log.error("Failed to execute DDL: {}", e.getMessage(), e);
            }
        }
        log.info("Base tables created or already exist");
    }
}
