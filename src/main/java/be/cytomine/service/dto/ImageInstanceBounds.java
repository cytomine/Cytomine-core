package be.cytomine.service.dto;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.utils.MinMax;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class ImageInstanceBounds extends AbstractBounds {

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

