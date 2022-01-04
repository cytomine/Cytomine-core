package be.cytomine.dto;

import com.vividsolutions.jts.geom.Geometry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SimplifiedAnnotation {

    Geometry newAnnotation;

    Double rate;
}
