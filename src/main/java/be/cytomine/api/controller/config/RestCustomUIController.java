package be.cytomine.api.controller.config;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestCustomUIController extends RestCytomineController {

    private final OntologyService ontologyService;

    private final OntologyRepository ontologyRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;


    @GetMapping("/custom-ui/project/{id}.json")
    public ResponseEntity<String> showCustomUIForProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Ontology : {}", id);
        return ResponseEntity.status(200).body(
                "{\"dashboard\":true,\"search\":false,\"project\":true,\"ontology\":false,\"storage\":true,\"activity\":true,\"feedback\":true,\"explore\":true,\"admin\":false,\"help\":true,\"project-images-tab\":true,\"project-annotations-tab\":true,\"project-jobs-tab\":false,\"project-activities-tab\":true,\"project-information-tab\":true,\"project-configuration-tab\":true,\"project-explore-image-overview\":true,\"project-explore-image-status\":true,\"project-explore-image-description\":true,\"project-explore-image-tags\":true,\"project-explore-image-properties\":true,\"project-explore-image-attached-files\":true,\"project-explore-image-slide-preview\":true,\"project-explore-image-original-filename\":true,\"project-explore-image-format\":true,\"project-explore-image-vendor\":true,\"project-explore-image-size\":true,\"project-explore-image-resolution\":true,\"project-explore-image-magnification\":true,\"project-explore-hide-tools\":true,\"project-explore-overview\":true,\"project-explore-info\":true,\"project-explore-digital-zoom\":true,\"project-explore-link\":true,\"project-explore-color-manipulation\":true,\"project-explore-image-layers\":true,\"project-explore-ontology\":true,\"project-explore-review\":true,\"project-explore-job\":true,\"project-explore-property\":true,\"project-explore-follow\":true,\"project-explore-guided-tour\":true,\"project-explore-annotation-main\":true,\"project-explore-annotation-geometry-info\":true,\"project-explore-annotation-info\":true,\"project-explore-annotation-comments\":true,\"project-explore-annotation-preview\":true,\"project-explore-annotation-properties\":true,\"project-explore-annotation-description\":true,\"project-explore-annotation-panel\":true,\"project-explore-annotation-terms\":true,\"project-explore-annotation-tags\":true,\"project-explore-annotation-attached-files\":true,\"project-explore-annotation-creation-info\":true,\"project-tools-main\":true,\"project-tools-select\":true,\"project-tools-point\":true,\"project-tools-line\":true,\"project-tools-freehand-line\":true,\"project-tools-arrow\":true,\"project-tools-rectangle\":true,\"project-tools-diamond\":true,\"project-tools-circle\":true,\"project-tools-polygon\":true,\"project-tools-freehand-polygon\":true,\"project-tools-magic\":true,\"project-tools-freehand\":true,\"project-tools-union\":true,\"project-tools-diff\":true,\"project-tools-fill\":true,\"project-tools-rule\":true,\"project-tools-edit\":true,\"project-tools-resize\":true,\"project-tools-rotate\":true,\"project-tools-move\":true,\"project-tools-delete\":true,\"project-tools-screenshot\":true,\"project-tools-undo-redo\":true,\"project-annotations-term-piegraph\":true,\"project-annotations-term-bargraph\":true,\"project-annotations-users-graph\":true,\"project-annotated-slides-term-graph\":true,\"project-annotated-slides-users-graph\":true,\"project-annotation-graph\":true,\"project-users-global-activities-graph\":true,\"project-users-heatmap-graph\":true}"
        );
    }

}
