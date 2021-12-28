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
