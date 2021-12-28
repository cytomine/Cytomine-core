package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.AnnotationIndexService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationTrackController extends RestCytomineController {

    @GetMapping("/annotation/{id}/annotationtrack.json")
    public ResponseEntity<String> listByAnnotation(
            @PathVariable Long id
    ) {
        return responseSuccess(new ArrayList<JsonObject>());
    }

}
