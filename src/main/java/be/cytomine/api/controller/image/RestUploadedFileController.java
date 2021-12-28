package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.api.controller.utils.RequestParams;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.UploadedFileService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestUploadedFileController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final UploadedFileService uploadedFileService;

    private final ImageServerService imageServerService;

    private final CurrentUserService currentUserService;


    @GetMapping("/uploadedfile.json")
    public ResponseEntity<String> list(
            @RequestParam(defaultValue = "false") Boolean onlyRootsWithDetails,
            @RequestParam(defaultValue = "false") Boolean onlyRoots,
            @RequestParam(required = false) Long parent,
            @RequestParam(required = false)  Long root,
            @RequestParam(defaultValue = "false") Boolean all
    ) {
        log.debug("REST request to list uploaded files");

        RequestParams requestParams = retrievePageableParameters();
        if (root!=null) {
            return responseSuccess(uploadedFileService.listHierarchicalTree((User) currentUserService.getCurrentUser(), root));
        } else if (onlyRootsWithDetails) {
            return responseSuccess(uploadedFileService.list(retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder()));
        } else if (all) {
            return responseSuccess(uploadedFileService.list(retrievePageable()));
        } else {
            return responseSuccess(uploadedFileService.list(currentUserService.getCurrentUser(), parent, onlyRoots, retrievePageable()));
        }
    }

    @GetMapping("/uploadedfile/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get uploadedFile {}", id);
        return uploadedFileService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
    }

    @PostMapping("/uploadedfile.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save uploadedFile : " + json);
        return add(uploadedFileService, json);
    }

    @PutMapping("/uploadedfile/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit uploadedFile : " + id);
        return update(uploadedFileService, json);
    }

    @DeleteMapping("/uploadedfile/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete uploadedFile : " + id);
        return delete(uploadedFileService, JsonObject.of("id", id), null);
    }


    @GetMapping("/uploadedfile/{id}/download")
    public RedirectView download(@PathVariable Long id) throws IOException {
        log.debug("REST request to download uploadedFile");
        UploadedFile uploadedFile = uploadedFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("UploadedFile", id));
        // TODO: in abstract image, there is no check fos download auth!?
        String url = imageServerService.downloadUri(uploadedFile);
        return new RedirectView(url);
    }
}
