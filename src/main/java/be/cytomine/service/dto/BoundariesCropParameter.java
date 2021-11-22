package be.cytomine.service.dto;

import lombok.Data;

@Data
public class BoundariesCropParameter {

    private Integer topLeftX;

    private Integer topLeftY;

    private Integer width;

    private Integer height;
}
