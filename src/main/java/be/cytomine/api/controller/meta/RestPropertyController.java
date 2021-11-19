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
public class RestPropertyController extends RestCytomineController {

    private final TermService termService;


    @GetMapping("/annotation/property/key.json")
    public ResponseEntity<String> listKeyForAnnotation() {
        log.debug("REST request to list property key");
        // TODO: implement...
        return responseSuccess(List.of());
    }

    @GetMapping("/domain/{domainClassName}/{domainIdent}/property.json")
    public ResponseEntity<String> listByDomain(
            @PathVariable String domainClassName,
            @PathVariable String domainIdent
    ) {
        log.debug("REST request to list property for domain {} {}", domainClassName, domainIdent);
        // TODO: implement...
        return responseSuccess(List.of());
    }

}
