package be.cytomine.service.dto;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public interface AnnotationIndexLightDTO {

    Long getUser();

    Long getSlice();

    Long getCountAnnotation();

    Long getCountReviewedAnnotation();
}
