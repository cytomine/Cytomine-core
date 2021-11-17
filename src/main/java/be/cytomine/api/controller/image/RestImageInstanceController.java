package be.cytomine.api.controller.image;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageInstanceController extends RestCytomineController {

    private final ProjectService projectService;

    private final ImageInstanceService imageInstanceService;

    @GetMapping("/project/{id}/imageinstance.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id,
            @RequestParam(value = "light", defaultValue = "false", required = false) Boolean light,
            @RequestParam(value = "tree", defaultValue = "false", required = false) Boolean tree,
            @RequestParam(value = "withLastActivity", defaultValue = "false", required = false) Boolean withLastActivity,
            @RequestParam(value = "sortColumn", defaultValue = "created", required = false) String sortColumn,
            @RequestParam(value = "sortDirection", defaultValue = "desc", required = false) String sortDirection,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Integer offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Integer max

    ) {
        log.debug("REST request to list images for project : {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));

        if (light) {
            return ResponseEntity.ok(responseList(imageInstanceService.listLight(project), offset, max).toJsonString());
        } else if (tree) {
            // TODO!
            throw new CytomineMethodNotYetImplementedException("");
        } else if (withLastActivity) {
            // TODO: support withLastActivity
            throw new CytomineMethodNotYetImplementedException("");
        } else {
            // TODO: retrieve searchParameters
            return ResponseEntity.ok(responseList(imageInstanceService.list(project, new ArrayList<>(), sortColumn, sortDirection, max, offset, false), offset, max).toJsonString());
        }
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
                .map( imageInstance -> ResponseEntity.ok(convertCytomineDomainToJSON(imageInstance)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseNotFound("imageInstance", id).toJsonString()));
    }

}
