package be.cytomine.service.appengine;

import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.appengine.TaskRunRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
public class TaskRunService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private AppEngineService appEngineService;

    @Autowired
    private TaskRunRepository taskRunRepository;

    @Autowired
    private ProjectService projectService;

    public ResponseEntity<String> addTaskRun(Long projectId, String uri) {
        Project project = projectService.get(projectId);
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(project, READ);
        securityACLService.checkIsNotReadOnly(project);

        ResponseEntity<String> response = appEngineService.post(uri, null, MediaType.APPLICATION_JSON);
        if (response.getStatusCode() != HttpStatus.OK) {
            return response;
        }

        JsonObject jsonResponse = Objects.requireNonNull(JsonObject.toJsonObject(response.getBody()));
        UUID taskId = UUID.fromString(jsonResponse.getJSONAttrStr("id", true));

        TaskRun taskRun = new TaskRun();
        taskRun.setUser(currentUser);
        taskRun.setProject(project);
        taskRun.setTaskRunId(taskId);
        taskRunRepository.save(taskRun);

        // We return the App engine response. Should we include information from Cytomine (project ID, user ID, created, ... ?)
        return response;
    }

    private void checkTaskRun(Long projectId, UUID taskRunId) {
        Optional<TaskRun> taskRun = taskRunRepository.findTaskRunByProjectIdAndTaskRunId(projectId, taskRunId);
        if (taskRun.isEmpty()) {
            throw new ObjectNotFoundException("TaskRun", taskRunId);
        }

        SecUser currentUser = currentUserService.getCurrentUser();
        Project project = projectService.get(projectId);

        securityACLService.checkUser(currentUser);
        securityACLService.check(project, READ);
        securityACLService.checkIsNotReadOnly(project);
    }

    public ResponseEntity<String> batchProvisionTaskRun(ArrayList<JsonObject> json, Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.put("task-runs/" + taskRunId.toString() + "/input-provisions", json, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<String> provisionTaskRun(JsonObject json, long projectId, UUID taskRunId, String paramName) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.put("task-runs/" + taskRunId.toString() + "/input-provisions/" + paramName, json, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<?> getTask(long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString());
    }

    public ResponseEntity<?> postStateAction(JsonObject json, long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.post("task-runs/" + taskRunId.toString() + "/state-actions", json, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<?> getOutputs(long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString() + "/outputs");
    }

    public ResponseEntity<?> getInputs(long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString() + "/outputs");
    }
}
