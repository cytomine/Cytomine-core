package be.cytomine.service.appengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.appengine.TaskRunRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.GeometryService;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskRunService {

    private final AppEngineService appEngineService;

    private final CurrentUserService currentUserService;

    private final GeometryService geometryService;

    private final ProjectService projectService;

    private final SecurityACLService securityACLService;

    private final UserAnnotationService userAnnotationService;

    private final TaskRunRepository taskRunRepository;

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

        UUID taskId = null;
        try {
            JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
            taskId = UUID.fromString(jsonResponse.path("id").asText());
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error parsing JSON response");
        }

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

    private List<JsonNode> processProvisions(List<JsonNode> json) {
        List<JsonNode> requestBody = new ArrayList<>();

        for (JsonNode provision : json) {
            ObjectNode processedProvision = provision.deepCopy();
            processedProvision.remove("type");

            // Process the input if it is an annotation type
            if (provision.get("type").asText().equals("geometry")) {
                Long annotationId = provision.get("value").asLong();
                UserAnnotation annotation = userAnnotationService.get(annotationId);
                processedProvision.put("value", geometryService.WKTToGeoJSON(annotation.getWktLocation()));
            }

            requestBody.add(processedProvision);
        }

        return requestBody;
    }

    public ResponseEntity<String> batchProvisionTaskRun(List<JsonNode> requestBody, Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        List<JsonNode> body = processProvisions(requestBody);
        return appEngineService.put("task-runs/" + taskRunId + "/input-provisions", body, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<String> provisionTaskRun(JsonNode json, Long projectId, UUID taskRunId, String paramName) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.put("task-runs/" + taskRunId.toString() + "/input-provisions/" + paramName, json, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<String> getTaskRun(Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId);
    }

    public ResponseEntity<String> postStateAction(JsonNode body, Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.post("task-runs/" + taskRunId.toString() + "/state-actions", body, MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<String> getOutputs(Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString() + "/outputs");
    }

    public ResponseEntity<String> getInputs(Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString() + "/inputs");
    }
}
