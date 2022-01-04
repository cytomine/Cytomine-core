package be.cytomine.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Kmeans {

    Long id;

    String location;

    List term = new ArrayList();

    Long count;

    Double ratio;
}
