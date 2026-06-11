package sensor.kircheis.top.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorMessage {
    @JsonProperty("did")
    private String device_id;

    @JsonProperty("ts")
    private long timestamp;

    @JsonProperty("ba")
    private Integer battery;

    @JsonProperty("lat")
    private BigDecimal lat;

    @JsonProperty("lon")
    private BigDecimal lon;

    @JsonProperty("dt")
    private List<SensorData> data;
}
