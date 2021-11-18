package be.cytomine.service.dto;

import lombok.Data;

@Data
public class ThumbParameter {

    private String format;

    private Integer maxSize;

    private String colormap;

    private Boolean inverse;

    private Double contrast;

    private Double gamma;

    private Integer bits;

    private Boolean maxBits;

    private Boolean refresh;

}
