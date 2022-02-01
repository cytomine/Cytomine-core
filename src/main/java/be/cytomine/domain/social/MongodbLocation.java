package be.cytomine.domain.social;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MongodbLocation {
    List<List<Double>> coordinates;
    String type;
}
