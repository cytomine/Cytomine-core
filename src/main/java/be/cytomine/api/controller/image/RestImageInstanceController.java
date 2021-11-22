package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.dto.ImageParameter;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.project.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageInstanceController extends RestCytomineController {

    private final ProjectService projectService;

    private final ImageInstanceService imageInstanceService;

    private final ImageServerService imageServerService;

    @GetMapping("/project/{id}/imageinstance.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id,
            @RequestParam(value = "light", defaultValue = "false", required = false) Boolean light,
            @RequestParam(value = "tree", defaultValue = "false", required = false) Boolean tree,
            @RequestParam(value = "withLastActivity", defaultValue = "false", required = false) Boolean withLastActivity,
            @RequestParam(value = "sort", defaultValue = "created", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "desc", required = false) String order,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Integer offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Integer max

    ) {
        log.debug("REST request to list images for project : {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));

        if (light) {
            return responseSuccess(imageInstanceService.listLight(project), offset, max);
        } else if (tree) {
            // TODO!
            throw new CytomineMethodNotYetImplementedException("");
        } else if (withLastActivity) {
            // TODO: support withLastActivity
            throw new CytomineMethodNotYetImplementedException("");
        } else {
            // TODO: retrieve searchParameters
            return responseSuccess(imageInstanceService.list(project, new ArrayList<>(), sort, order, max, offset, false), offset, max);
        }
    }


    @GetMapping("/imageinstance/{id}/sliceinstance/reference.json")
    public ResponseEntity<String> getReferenceSlice(
            @PathVariable Long id
    ) {
        log.debug("REST request get reference sliceinstance for imageinstance {}", id);
        SliceInstance sliceInstance = imageInstanceService.getReferenceSlice(id);
        if (sliceInstance != null) {
            return responseSuccess(sliceInstance);
        } else {
            return responseNotFound("SliceInstance", "ImageInstance", id);
        }
    }

    @GetMapping("/imageinstance/{id}/thumb.{format}")
    public void thumb(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "false", required = false) Boolean refresh,
            @RequestParam(defaultValue = "512", required = false) Integer maxSize,
            @RequestParam(defaultValue = "", required = false) String colormap,
            @RequestParam(defaultValue = "false", required = false) Boolean inverse,
            @RequestParam(defaultValue = "0", required = false) Double contrast,
            @RequestParam(defaultValue = "0", required = false) Double gamma,
            @RequestParam(defaultValue = "0", required = false) String bits

    ) {
        log.debug("REST request get image {} thumb {}", id, format);
        ImageParameter thumbParameter = new ImageParameter();
        thumbParameter.setFormat(format);
        thumbParameter.setMaxSize(maxSize);
        thumbParameter.setColormap(colormap);
        thumbParameter.setInverse(inverse);
        thumbParameter.setContrast(contrast);
        thumbParameter.setGamma(gamma);
        thumbParameter.setMaxBits(bits.equals("max"));
        thumbParameter.setBits(!bits.equals("max") ? Integer.parseInt(bits): 0);
        thumbParameter.setRefresh(refresh);

        responseByteArray(imageServerService.thumb(
                imageInstanceService.find(id).orElseThrow(() -> new ObjectNotFoundException("ImageInstance")),
                thumbParameter), format
        );
    }


    @GetMapping("/project/{id}/bounds/imageinstance.json")
    public ResponseEntity<String> bounds(
            @PathVariable Long id
    ) {
        log.debug("REST request to list projects bounds");
        // TODO: implement...

        return ResponseEntity.status(200).body(
               "{\"channel\":{\"min\":null,\"max\":null},\"countImageAnnotations\":{\"min\":0,\"max\":99999},\"countImageJobAnnotations\":{\"min\":0,\"max\":99999},\"countImageReviewedAnnotations\":{\"min\":0,\"max\":99999},\"created\":{\"min\":\"1691582770212\",\"max\":\"1605232995654\"},\"deleted\":{\"min\":null,\"max\":null},\"instanceFilename\":{\"min\":\"15H26535 CD8_07.12.2020_11.06.32.mrxs\",\"max\":\"VE0CD5700003EF_2020-11-04_11_36_38.scn\"},\"magnification\":{\"list\":[20,40],\"min\":20,\"max\":40},\"resolution\":{\"list\":[0.12499998807907104,0.25,0.49900001287460327],\"min\":0.25,\"max\":0.49900001287460327},\"reviewStart\":{\"min\":null,\"max\":null},\"reviewStop\":{\"min\":null,\"max\":null},\"updated\":{\"min\":null,\"max\":null},\"zIndex\":{\"min\":null,\"max\":null},\"width\":{\"min\":46000,\"max\":106259},\"height\":{\"min\":32914,\"max\":306939},\"format\":{\"list\":[\"mrxs\",\"scn\",\"svs\"]},\"mimeType\":{\"list\":[\"openslide/mrxs\",\"openslide/scn\",\"openslide/svs\"]}}"
        );


    }


    @GetMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Ontology : {}", id);
        return imageInstanceService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("imageInstance", id));
    }

}
