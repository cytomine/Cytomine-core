package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
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
public class RestSliceInstanceController extends RestCytomineController {

    private final SliceInstanceService sliceInstanceService;

    private final ImageInstanceService imageInstanceService;

    @GetMapping("/imageinstance/{id}/sliceinstance.json")
    public ResponseEntity<JsonObject> listByImageInstance(
            @PathVariable Long id
    ) {
        log.debug("REST request to list sliceinstance for imageinstance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(sliceInstanceService.list(imageInstance));
    }
}
