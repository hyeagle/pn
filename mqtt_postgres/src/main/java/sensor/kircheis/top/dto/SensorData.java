package sensor.kircheis.top.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    @JsonProperty("sid")
    private String sensor_id;

    @JsonProperty("mtc")
    private String metric;

    @JsonProperty("v")
    private double value;

    @JsonProperty("u")
    private String unit;
}
