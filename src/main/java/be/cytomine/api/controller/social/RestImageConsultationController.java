package be.cytomine.api.controller.social;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageConsultationController extends RestCytomineController {

    private final TermService termService;


    @PostMapping("/imageinstance/{id}/consultation.json")
    public ResponseEntity<String> add(
            Long id
    ) {
        // TODO: implement...
        return responseSuccess(new JsonObject());
    }
}
