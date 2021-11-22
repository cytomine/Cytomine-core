package be.cytomine.service.dto;

import lombok.Data;

@Data
public class WindowParameter {

    private String format;

    private int x;

    private int y;

    private int w;

    private int h;

    private boolean withExterior;

    private BoundariesCropParameter boundaries;
}
