package sensor.kircheis.top.table;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTableScheduler {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PARENT_TABLE = "sensor_data";
    private static final String SEQ_NAME = "sensor_data_id_seq";
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createSequence();
        createParentTable();
        try {
            createDailyTable(LocalDate.now());
        } catch (SQLException e) {
            log.error("Failed to create today's partition table", e);
        }
        log.info("Daily table scheduler initialized");
    }

    public void ensureTableForDate(LocalDate date) throws SQLException {
        createDailyTable(date);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void createTomorrowTables() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            createDailyTable(tomorrow);
        } catch (SQLException e) {
            log.error("Failed to create daily table: {}", e.getMessage(), e);
        }
    }

    private void createSequence() {
        String sql = "CREATE SEQUENCE IF NOT EXISTS " + SEQ_NAME;
        try {
            jdbcTemplate.execute(sql);
            log.info("Sequence created or already exists: {}", SEQ_NAME);
        } catch (Exception e) {
            log.error("Failed to create sequence: {}", e.getMessage(), e);
        }
    }

    private void createParentTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + PARENT_TABLE + " (" +
                "id BIGINT DEFAULT nextval('" + SEQ_NAME + "'), " +
                "device_id VARCHAR(50) NOT NULL, " +
                "sensor_id VARCHAR(50) NOT NULL, " +
                "metric VARCHAR(50) NOT NULL, " +
                "value NUMERIC(10,2) NOT NULL, " +
                "ts BIGINT NOT NULL, " +
                "create_time TIMESTAMP DEFAULT now()" +
                ") PARTITION BY RANGE (ts)";
        try {
            jdbcTemplate.execute(sql);
            log.info("Parent partition table created: {} PARTITION BY RANGE (ts)", PARENT_TABLE);
        } catch (Exception e) {
            log.error("Failed to create parent table: {}", e.getMessage(), e);
        }
    }

    private long toEpochSecond(LocalDate date) {
        return date.atStartOfDay(ZONE).toEpochSecond();
    }

    private void createDailyTable(LocalDate date) throws SQLException {
        String dateStr = date.format(DATE_FORMATTER);
        String tableName = "sensor_data_" + dateStr;
        long startTs = toEpochSecond(date);
        long endTs = toEpochSecond(date.plusDays(1));

        // 检查表是否已存在
        List<Integer> result = jdbcTemplate.query(
                "SELECT 1 FROM information_schema.tables WHERE table_name = ?",
                (rs, rowNum) -> 1, tableName);
        if (!result.isEmpty()) {
            log.info("Daily partition table already exists, skipped: {}", tableName);
            return;
        }

        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "device_id VARCHAR(50) NOT NULL, " +
                "sensor_id VARCHAR(50) NOT NULL, " +
                "metric VARCHAR(50) NOT NULL, " +
                "value NUMERIC(10,2) NOT NULL, " +
                "ts BIGINT NOT NULL, " +
                "create_time TIMESTAMP NOT NULL DEFAULT now()" +
                ")";

        String attachSql = "ALTER TABLE " + PARENT_TABLE + " ATTACH PARTITION " + tableName +
                " FOR VALUES FROM (" + startTs + ") TO (" + endTs + ")";

        try {
            jdbcTemplate.execute(createTableSql);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_device_metric_ts_" + dateStr + " ON " + tableName + " (device_id, metric, ts)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sensor_ts_" + dateStr + " ON " + tableName + " (sensor_id, ts)");
            jdbcTemplate.execute(attachSql);
            log.info("Daily partition table created and attached: {} FOR VALUES FROM ({}) TO ({})", tableName, startTs, endTs);
        } catch (Exception e) {
            log.error("Failed to create daily table: {}", e.getMessage(), e);
        }
    }
}
