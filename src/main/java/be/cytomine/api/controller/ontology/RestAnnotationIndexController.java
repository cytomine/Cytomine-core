package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.AnnotationIndexService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationIndexController extends RestCytomineController {

    private final AnnotationIndexService annotationIndexService;

    private final SliceInstanceService sliceInstanceService;
    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @GetMapping("/sliceinstance/{id}/annotationindex.json")
    public ResponseEntity<String> listBySlice(
            @PathVariable Long id
    ) {
        log.debug("REST request to get annotationindex for slice {}", id);
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));
        return responseSuccess(annotationIndexService.list(sliceInstance));
    }

}
