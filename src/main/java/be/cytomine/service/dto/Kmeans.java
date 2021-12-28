package be.cytomine.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Kmeans {

    Long id;

    String location;

    List term = new ArrayList();

    Long count;

    Double ratio;
}
