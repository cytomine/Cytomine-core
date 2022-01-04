package be.cytomine.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabelParameter {

    private String format;

    private String label;

    private Integer maxSize;

}
