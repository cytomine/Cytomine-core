package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.AnnotationIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationDomainController extends RestCytomineController {

    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @PostMapping("/annotation/search.json")
    public ResponseEntity<String> search() {
        // TODO :'(
        return responseSuccess(List.of());
    }

}
