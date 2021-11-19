package be.cytomine.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public interface AnnotationIndexLightDTO {

    Long getUser();

    Long getSlice();

    Long getCountAnnotation();

    Long getCountReviewedAnnotation();
}
