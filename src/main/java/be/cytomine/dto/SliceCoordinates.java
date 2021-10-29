package be.cytomine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SliceCoordinates {

    List<Integer> channels;

    List<Integer> zStacks;

    List<Integer> times;

}
