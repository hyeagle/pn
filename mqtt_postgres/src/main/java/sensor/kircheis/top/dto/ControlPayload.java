package sensor.kircheis.top.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlPayload {
    @JsonProperty("device_id")
    private String deviceId;
    @JsonProperty("secret")
    private String secret;
    @JsonProperty("5v")
    private Boolean fiveV;
    @JsonProperty("bump")
    private Boolean bump;
    @JsonProperty("sys_restart")
    private Boolean sysRestart;
}
