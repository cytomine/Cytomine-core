package be.cytomine.service.dto;

public interface AnnotationIndexLightDTO {

    Long getUser();

    Long getSlice();

    Long getCountAnnotation();

    Long getCountReviewedAnnotation();
}
