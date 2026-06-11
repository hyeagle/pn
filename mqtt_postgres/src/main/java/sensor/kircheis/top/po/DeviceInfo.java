package sensor.kircheis.top.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    private String deviceId;
    private String userId;
    private String deviceName;
    private Boolean relay;
    private Boolean power;
    private Integer battery;
    private Long updateTime;
}
