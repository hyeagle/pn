package sensor.kircheis.top.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private String deviceId;
    private String userId;
    private String deviceName;
    private Integer battery;
    private Long updateTime;
    private Boolean fiveV;
    private Boolean bump;
}
