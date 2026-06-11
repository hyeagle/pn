package sensor.kircheis.top.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataRecord {
    private Long id;
    private String deviceId;
    private String sensorId;
    private String metric;
    private BigDecimal value;
    private Long ts;
    private LocalDateTime createTime;
}
