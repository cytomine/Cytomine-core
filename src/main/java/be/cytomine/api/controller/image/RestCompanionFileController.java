package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.CompanionFileService;
import be.cytomine.service.image.UploadedFileService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestCompanionFileController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final UploadedFileService uploadedFileService;

    private final CompanionFileService companionFileService;

    private final ImageServerService imageServerService;

    @GetMapping("/abstractimage/{id}/companionfile.json")
    public ResponseEntity<String> listByAbstractImage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list companion file for abstract image {}", id);
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        return responseSuccess(companionFileService.list(abstractImage));
    }

    @GetMapping("/uploadedfile/{id}/companionfile.json")
    public ResponseEntity<String> listByUploadedFile(
            @PathVariable Long id
    ) {
        log.debug("REST request to list companion file for uploaded file {}", id);
        UploadedFile uploadedFile = uploadedFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("UploadedFile", id));
        return responseSuccess(companionFileService.list(uploadedFile));
    }

    @GetMapping("/companionfile/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get companionfile {}", id);
        return companionFileService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
    }

    @PostMapping("/companionfile.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save companionfile : " + json);
        return add(companionFileService, json);
    }

    @PutMapping("/companionfile/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit companionfile : " + id);
        return update(companionFileService, json);
    }

    @DeleteMapping("/companionfile/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete companionfile : " + id);
        return delete(companionFileService, JsonObject.of("id", id), null);
    }


    @GetMapping("/companionfile/{id}/download")
    public RedirectView download(@PathVariable Long id) throws IOException {
        log.debug("REST request to download companionfile");
        CompanionFile companionFile = companionFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("CompanionFile", id));
        // TODO: in abstract image, there is no check fos download auth!?
        String url = imageServerService.downloadUri(companionFile);
        return new RedirectView(url);
    }


    @GetMapping("/companionfile/{id}/user.json")
    public ResponseEntity<String> showUploaderOfImage(@PathVariable Long id) {
        log.debug("REST request to show companionfile uploader");
        CompanionFile companionFile = companionFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("CompanionFile", id));
        if (companionFile.getUploadedFile() !=null && companionFile.getUploadedFile().getUser()!=null) {
            return responseSuccess(companionFile.getUploadedFile().getUser());
        } else {
            return responseNotFound("CompanionFile", "User", id);
        }

    }


    /// TODO:

//
//    @RestApiMethod(description="Ask to compute HDF5 profile for the given image")
//    @RestApiParams(params=[
//            @RestApiParam(name="image", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id")
//            ])
//    def computeProfile() {
//        def id = params.long("image") ?: request.JSON?.image
//
//        AbstractImage abstractImage = abstractImageService.read(id)
//        if (abstractImage) {
//            if (abstractImage.dimensions.length() == 3 && !abstractImage.hasProfile()) {
//                //TODO: check image is greyscale
//                responseSuccess(imageServerService.profile(abstractImage).companionFile)
//            }
//            else {
//                responseError(new InvalidRequestException("Abstract image ${abstractImage.id} already has a profile or cannot have one."))
//            }
//        }
//        else {
//            responseNotFound("Image", id)
//        }
//    }
}
