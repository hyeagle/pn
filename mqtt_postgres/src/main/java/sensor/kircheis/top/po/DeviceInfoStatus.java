package sensor.kircheis.top.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoStatus {
    private String deviceId;
    private String statusKey;
    private Boolean status;
    private Long updateTime;
}
