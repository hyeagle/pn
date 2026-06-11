package sensor.kircheis.top.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> data;
    private int total;
    private int page;
    private int size;
    private int totalPages;
}
