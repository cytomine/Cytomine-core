package be.cytomine.api.controller.middleware;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageServerController extends RestCytomineController {

    private final ImageServerService imageServerService;

    @GetMapping("/imageserver.json")
    public ResponseEntity<String> list(
    ) {
        log.debug("REST request to list terms");
        return responseSuccess(imageServerService.list());
    }

    @GetMapping("/imageserver/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get imageserver : {}", id);
        return imageServerService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("imageserver", id));
    }

}
