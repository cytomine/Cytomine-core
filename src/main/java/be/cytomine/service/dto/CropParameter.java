package be.cytomine.service.dto;

import lombok.Data;

@Data
public class CropParameter {

    private String format;

    private String geometry;

    private String location;

    private boolean complete;

    private Integer maxSize;

    private Integer zoom;

    private Double increaseArea;

    private boolean safe;

    private boolean square;

    private String type;

    private boolean draw;

    private boolean mask;

    private boolean alphaMask;

    private boolean drawScaleBar;

    private double resolution;

    private double magnification;

    private String colormap;

    private boolean inverse;

    private double contrast;

    private double gamma;

    private int bits;

    private boolean maxBits;

    private int alpha;

    private int thickness;

    private String color;

    private int jpegQuality;

    private BoundariesCropParameter boundaries;

}