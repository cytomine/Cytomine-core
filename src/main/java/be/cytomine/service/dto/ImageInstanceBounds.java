package be.cytomine.service.dto;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.utils.MinMax;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class ImageInstanceBounds {

    private MinMax<Date> created = new MinMax<>(); // should be long in final request!!

    private MinMax<Date> updated = new MinMax<>();

    private MinMax<Date> reviewStart = new MinMax<>();

    private MinMax<Date> reviewStop = new MinMax<>();

    private MinMax<Integer> magnification = new MinMax<>(); // + list

    private MinMax<Double> physicalSizeX = new MinMax<>();

    private MinMax<Double> physicalSizeY = new MinMax<>();

    private MinMax<Double> physicalSizeZ = new MinMax<>();

    private MinMax<Double> fps = new MinMax<>();

    private MinMax<String> format = new MinMax<>();

    private MinMax<String> mimeType = new MinMax<>();

    private MinMax<Long> countImageAnnotations = new MinMax<>();

    private MinMax<Long> countImageJobAnnotations = new MinMax<>();

    private MinMax<Long> countImageReviewedAnnotations = new MinMax<>();

    private MinMax<Integer> width = new MinMax<>();

    private MinMax<Integer> height = new MinMax<>();

    private MinMax<String> instanceFilename = new MinMax<>();


    public void submit(ImageInstance imageInstance) {
        AbstractImage abstractImage = imageInstance.getBaseImage();
        updateMinMax(created, imageInstance.getCreated());
        updateMinMax(updated, imageInstance.getUpdated());
        updateMinMax(reviewStart, imageInstance.getReviewStart());
        updateMinMax(reviewStop, imageInstance.getReviewStop());

        updateMinMax(magnification, imageInstance.getMagnification());
        updateChoices(magnification, imageInstance.getMagnification());

        updateMinMax(physicalSizeX, imageInstance.getPhysicalSizeX());
        updateChoices(physicalSizeX, imageInstance.getPhysicalSizeX());

        updateMinMax(physicalSizeY, imageInstance.getPhysicalSizeY());
        updateMinMax(physicalSizeZ, imageInstance.getPhysicalSizeZ());
        updateMinMax(fps, imageInstance.getFps());

        updateChoices(mimeType, abstractImage.getContentType());
        updateChoices(format, abstractImage.getContentType());

        updateMinMax(countImageAnnotations, imageInstance.getCountImageAnnotations());
        updateMinMax(countImageJobAnnotations, imageInstance.getCountImageJobAnnotations());
        updateMinMax(countImageReviewedAnnotations, imageInstance.getCountImageReviewedAnnotations());

        updateMinMax(width, abstractImage.getWidth());
        updateMinMax(height, abstractImage.getHeight());

        updateMinMax(instanceFilename, imageInstance.getInstanceFilename());
    }

    protected <T extends Comparable> void updateMinMax(MinMax<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue.getMin()==null) {
            currentValue.setMin(newValue);
        } else if(newValue.compareTo(currentValue.getMin())<0) {
            currentValue.setMin(newValue);
        }
        if (currentValue.getMax()==null) {
            currentValue.setMax(newValue);
        } else if(newValue.compareTo(currentValue.getMax())>0) {
            currentValue.setMax(newValue);
        }
    }

    protected <T extends Comparable> void updateChoices(MinMax<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue.getList()==null) {
            currentValue.setList(new ArrayList<>());
        }
        if (!currentValue.getList().contains(newValue)) {
            currentValue.getList().add(newValue);
        }
    }

    protected <T extends Comparable> void updateChoices(List<T> currentValue, T newValue) {
        if (newValue==null) {
            return;
        }
        if (currentValue==null) {
            currentValue = new ArrayList<>();
        }
        if (!currentValue.contains(newValue)) {
            currentValue.add(newValue);
        }
    }
}



    
    
    
    
//{
//   "channel":{
//      "min":null,
//      "max":null
//   },
//   "countImageAnnotations":{
//      "min":0,
//      "max":2
//   },
//   "countImageJobAnnotations":{
//      "min":0,
//      "max":0
//   },
//   "countImageReviewedAnnotations":{
//      "min":0,
//      "max":0
//   },
//   "created":{
//      "min":"1621582770212",
//      "max":"1635232995654"
//   },
//   "deleted":{
//      "min":null,
//      "max":null
//   },
//   "instanceFilename":{
//      "min":"15H26535 CD8_07.12.2020_11.06.32.mrxs",
//      "max":"VE0CD5700003EF_2020-11-04_11_36_38.scn"
//   },
//   "magnification":{
//      "list":[
//         20,
//         40
//      ],
//      "min":20,
//      "max":40
//   },
//   "resolution":{
//      "list":[
//         0.32499998807907104,
//         0.25,
//         0.49900001287460327
//      ],
//      "min":0.25,
//      "max":0.49900001287460327
//   },
//   "reviewStart":{
//      "min":null,
//      "max":null
//   },
//   "reviewStop":{
//      "min":null,
//      "max":null
//   },
//   "updated":{
//      "min":null,
//      "max":null
//   },
//   "zIndex":{
//      "min":null,
//      "max":null
//   },
//   "width":{
//      "min":46000,
//      "max":106259
//   },
//   "height":{
//      "min":32914,
//      "max":306939
//   },
//   "format":{
//      "list":[
//         "mrxs",
//         "scn",
//         "svs"
//      ]
//   },
//   "mimeType":{
//      "list":[
//         "openslide/mrxs",
//         "openslide/scn",
//         "openslide/svs"
//      ]
//   }
//}

