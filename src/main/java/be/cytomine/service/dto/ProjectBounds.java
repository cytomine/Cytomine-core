package be.cytomine.service.dto;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.MinMax;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class ProjectBounds extends AbstractBounds {




//   "numberOfAnnotations":{
//      "min":0,
//      "max":31
//   },
//   "numberOfJobAnnotations":{
//      "min":0,
//      "max":0
//   },
//   "numberOfReviewedAnnotations":{
//      "min":0,
//      "max":2
//   },
//   "numberOfImages":{
//      "min":0,
//      "max":5
//   },
//   "members":{
//      "min":1,
//      "max":5
//   }
//}

    private MinMax<Date> created = new MinMax<>();

    private MinMax<Date> updated = new MinMax<>();

    private MinMax<String> mode = new MinMax<>();

    private MinMax<String> name = new MinMax<>();

    private MinMax<Long> numberOfAnnotations = new MinMax<>();

    private MinMax<Long> numberOfJobAnnotations = new MinMax<>();

    private MinMax<Long> numberOfReviewedAnnotations = new MinMax<>();

    private MinMax<Long> numberOfImages = new MinMax<>();

    private MinMax<Long> members = new MinMax<>();



    public void submit(JsonObject project) {
        log.debug(project.toJsonString());
        updateMinMax(created, project.getJSONAttrDate("created"));
        updateMinMax(updated, project.getJSONAttrDate("updated"));

        updateMinMax(mode, project.getJSONAttrStr("mode"));
        updateMinMax(name, project.getJSONAttrStr("name"));
        updateMinMax(numberOfAnnotations, project.getJSONAttrLong("numberOfAnnotations"));
        updateMinMax(numberOfJobAnnotations, project.getJSONAttrLong("numberOfJobAnnotations"));

        updateMinMax(numberOfReviewedAnnotations, project.getJSONAttrLong("numberOfReviewedAnnotations"));
        updateMinMax(numberOfImages, project.getJSONAttrLong("numberOfImages"));
        updateMinMax(members, project.getJSONAttrLong("membersCount"));
    }
}



    
    
