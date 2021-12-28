package be.cytomine.api.controller.project;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.security.SecUser;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.utils.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestProjectController extends RestCytomineController {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @GetMapping("/project.json")
    public ResponseEntity<String> list(
            @RequestParam(value = "withMembersCount", defaultValue = "false", required = false) Boolean withMembersCount,
            @RequestParam(value = "withLastActivity", defaultValue = "false", required = false) Boolean withLastActivity,
            @RequestParam(value = "withDescription", defaultValue = "false", required = false) Boolean withDescription,
            @RequestParam(value = "withCurrentUserRoles", defaultValue = "false", required = false) Boolean withCurrentUserRoles,
            @RequestParam(value = "sort", defaultValue = "created", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "desc", required = false) String order

    ) {
        log.debug("REST request to list projects");
        SecUser user = currentUserService.getCurrentUser();

        if(currentRoleService.isAdminByNow(user)) {
            //if user is admin, we print all available project
            user = null;
        }

        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithMembersCount(withMembersCount);
        projectSearchExtension.setWithLastActivity(withLastActivity);
        projectSearchExtension.setWithDescription(withDescription);
        projectSearchExtension.setWithCurrentUserRoles(withCurrentUserRoles);
        // TODO: parse searchParameterEntry
        return responseSuccess(projectService.list(user, projectSearchExtension, new ArrayList<>(), sort, order, 0L, 0L));
    }


    @GetMapping("/project/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get project : {}", id);
        return projectService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Project", id));
    }



    @GetMapping("/bounds/project.json")
    public ResponseEntity<String> bounds(
                @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list projects bounds");
        // TODO: implement...


        return ResponseEntity.status(200).body(
                "{\"created\":{\"min\":\"1621022930120\",\"max\":\"1634961982641\"},\"deleted\":{\"min\":null,\"max\":null},\"mode\":{\"min\":null,\"max\":null},\"name\":{\"min\":\"Cytomine Team\",\"max\":\"lrollus-test\"},\"updated\":{\"min\":null,\"max\":null},\"numberOfAnnotations\":{\"min\":3,\"max\":5},\"numberOfJobAnnotations\":{\"min\":0,\"max\":1000000},\"numberOfReviewedAnnotations\":{\"min\":0,\"max\":1000000},\"numberOfImages\":{\"min\":0,\"max\":10000000},\"members\":{\"min\":0,\"max\":10000}}"
        );


    }


    @PostMapping("/project/{id}/userconnection.json")
    public ResponseEntity<String> userconnection(
            @PathVariable Long id
    ) {
        log.debug("REST request to list project {}", id);
        // TODO: implement...


        return ResponseEntity.status(200).body(
            "{\"class\":\"be.cytomine.social.PersistentProjectConnection\",\"id\":7255896,\"created\":\"1637155883674\",\"updated\":null,\"deleted\":null,\"user\":6399285,\"project\":6447795,\"time\":null,\"os\":\"Linux\",\"browser\":\"chrome\",\"browserVersion\":\"95.0.4638\",\"countViewedImages\":null,\"countCreatedAnnotations\":null}"
        );


    }

}
