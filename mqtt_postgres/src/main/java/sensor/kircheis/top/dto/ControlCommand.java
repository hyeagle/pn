package sensor.kircheis.top.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlCommand {
    private Boolean relay;
    private Boolean power;
}
