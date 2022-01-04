package be.cytomine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SliceCoordinate {

    Integer channel;

    Integer zStack;

    Integer time;

}
