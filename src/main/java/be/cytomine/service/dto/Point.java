package be.cytomine.service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Point {
    Double x;
    Double y;

    public List<Double> toList() {
        return List.of(x,y);
    }
}
