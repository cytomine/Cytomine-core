package be.cytomine.api.controller.image.group;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.group.ImageGroupImageInstanceService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageGroupImageInstanceController extends RestCytomineController {

    private final ImageInstanceService imageInstanceService;

    private final ImageGroupImageInstanceService imageGroupImageInstanceService;

    @GetMapping("/imageinstance/{id}/imagegroupimageinstance.json")
    public ResponseEntity<String> listByImageInstance(@PathVariable Long id) {
        log.debug("REST request to get all relations for an image: {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageGroupImageInstanceService.list(imageInstance));
    }

    @PostMapping("/imagegroup/{group}/imageinstance/{image}.json")
    public ResponseEntity<String> add(
            @PathVariable Long group,
            @PathVariable Long image,
            @RequestBody String json
    ) {
        log.debug("REST request to add image {} to imagegroup {}", image, group);
        return add(imageGroupImageInstanceService, json);
    }

    @DeleteMapping("/imagegroup/{group}/imageinstance/{image}.json")
    public ResponseEntity<String> delete(@PathVariable Long group, @PathVariable Long image) {
        log.debug("REST request to delete image {} from imagegroup {}", image, group);
        return delete(imageGroupImageInstanceService, JsonObject.of("group", group, "image", image), null);
    }
}
