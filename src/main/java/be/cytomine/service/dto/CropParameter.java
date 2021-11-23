package be.cytomine.service.dto;

import lombok.Data;

@Data
public class CropParameter {

    private String format;

    private String geometry;

    private String location;

    private Boolean complete;

    private Integer maxSize;

    private Integer zoom;

    private Double increaseArea;

    private Boolean safe;

    private Boolean square;

    private String type;

    private Boolean draw;

    private Boolean mask;

    private Boolean alphaMask;

    private Boolean drawScaleBar;

    private double resolution;

    private double magnification;

    private String colormap;

    private Boolean inverse;

    private double contrast;

    private double gamma;

    private Integer bits;

    private Boolean maxBits;

    private Integer alpha;

    private Integer thickness;

    private String color;

    private Integer jpegQuality;

    private BoundariesCropParameter boundaries;

}