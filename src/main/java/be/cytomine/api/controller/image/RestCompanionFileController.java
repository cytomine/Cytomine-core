package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.CompanionFileService;
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
public class RestCompanionFileController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final CompanionFileService companionFileService;

    @GetMapping("/abstractimage/{id}/companionfile.json")
    public ResponseEntity<String> listByAbstractImage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list companion file for abstract image {}", id);
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        return responseSuccess(companionFileService.list(abstractImage));
    }
}
