package be.cytomine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SliceCoordinate {

    Integer channel;

    Integer zStack;

    Integer time;

}
