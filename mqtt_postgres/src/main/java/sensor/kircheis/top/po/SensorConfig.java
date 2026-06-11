package sensor.kircheis.top.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorConfig {
    private String sensorId;
    private String deviceId;
    private String metric;
    private String metricName;
    private String unit;
    private Long createTime;
}
