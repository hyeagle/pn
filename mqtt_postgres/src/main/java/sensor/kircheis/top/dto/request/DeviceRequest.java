package sensor.kircheis.top.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {
    private String deviceId;
    private String userId;
    private String deviceName;
    private Integer battery;
}
