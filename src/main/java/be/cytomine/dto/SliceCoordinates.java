package be.cytomine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SliceCoordinates {

    List<Integer> channels;

    List<Integer> zStacks;

    List<Integer> times;

}
