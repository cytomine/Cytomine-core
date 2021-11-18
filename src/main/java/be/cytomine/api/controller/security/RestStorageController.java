package be.cytomine.api.controller.security;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.StorageService;
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
public class RestStorageController extends RestCytomineController {

    private final SecUserService secUserService;

    private final StorageService storageService;

    @GetMapping("/storage")
    public ResponseEntity<JsonObject> list(
            @RequestParam(required = false, defaultValue = "") String searchString,
            @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list storages");

        SecUser user = secUserService.getCurrentUser();
        List<Storage> storages = storageService.list(user, searchString);
        // TODO: sort
        return ResponseEntity.ok(buildJsonList(convertCytomineDomainListToJSON(storages), allParams));
    }

}
