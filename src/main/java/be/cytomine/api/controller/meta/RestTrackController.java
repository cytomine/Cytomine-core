package be.cytomine.api.controller.meta;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.service.ontology.TermService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestTrackController extends RestCytomineController {

    private final TermService termService;


    @GetMapping("/imageinstance/{id}/track.json")
    public ResponseEntity<String> listByImageInstance(
            @PathVariable Long id
    ) {
        log.debug("REST request to list tags");
        // TODO: implement...
        return responseSuccess(List.of());
    }
}
