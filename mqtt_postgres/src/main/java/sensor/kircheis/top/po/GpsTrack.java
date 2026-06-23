package sensor.kircheis.top.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsTrack {
    private Long id;
    private String deviceId;
    private Long recordTs;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
