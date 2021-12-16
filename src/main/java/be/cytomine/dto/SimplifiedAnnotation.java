package be.cytomine.dto;

import com.vividsolutions.jts.geom.Geometry;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimplifiedAnnotation {

    Geometry newAnnotation;

    Double rate;
}
