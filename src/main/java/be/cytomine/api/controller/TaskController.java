package be.cytomine.api.controller;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentConnection;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentConnectionRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TaskController extends RestCytomineController {

    private final CurrentUserService currentUserService;

    private final ProjectService projectService;

    private final TaskService taskService;

    @GetMapping("/task/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        Task task = taskService.get(id);
        if (task == null) {
            throw new ObjectNotFoundException("Task", id);
        }
        JsonObject jsonObject = task.toJsonObject();
        jsonObject.put("comments", taskService.getLastComments(task,5));
        return responseSuccess(jsonObject);
    }

    @PostMapping("/task.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        Project project = null;
        try {
            project = projectService.get(json.getJSONAttrLong("project", 0L));
        } catch(Exception ignored) {

        }
        SecUser user = currentUserService.getCurrentUser();
        boolean printInActivity = json.getJSONAttrBoolean("printInActivity", false);
        Task task = taskService.createNewTask(project,user,printInActivity);
        JsonObject jsonObject = task.toJsonObject();
        jsonObject.put("comments", taskService.getLastComments(task,5));
        return responseSuccess(JsonObject.of("task", jsonObject));
    }

    @GetMapping("/project/{project}/task/comment.json")
    public ResponseEntity<String> listCommentByProject(@PathVariable(value = "project") Long projectId) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(taskService.listLastComments(project));
    }
}
