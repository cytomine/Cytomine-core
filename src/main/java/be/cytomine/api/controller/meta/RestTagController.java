package be.cytomine.api.controller.meta;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestTagController extends RestCytomineController {

    private final TermService termService;


    @GetMapping("/tag.json")
    public ResponseEntity<String> list(
            @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list tags");
        // TODO: implement...
        return responseSuccess(List.of());
    }
}
