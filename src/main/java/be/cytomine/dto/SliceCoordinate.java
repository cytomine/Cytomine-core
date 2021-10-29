package be.cytomine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SliceCoordinate {

    Integer channel;

    Integer zStack;

    Integer time;

}
