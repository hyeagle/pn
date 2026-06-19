package sensor.kircheis.top.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlRequest {
    @JsonProperty("5v")
    private Boolean fiveV;
    @JsonProperty("bump")
    private Boolean bump;
    @JsonProperty("sys_restart")
    private Boolean sysRestart;
}
