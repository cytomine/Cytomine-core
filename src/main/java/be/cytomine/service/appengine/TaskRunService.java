package be.cytomine.service.appengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.domain.appengine.TaskRunLayer;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.dto.appengine.task.TaskRunValue;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.appengine.TaskRunLayerRepository;
import be.cytomine.repository.appengine.TaskRunRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.annotation.AnnotationLayerService;
import be.cytomine.service.annotation.AnnotationService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.GeometryService;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskRunService {

    private final AnnotationService annotationService;

    private final AnnotationLayerService annotationLayerService;

    private final AppEngineService appEngineService;

    private final CurrentUserService currentUserService;

    private final GeometryService geometryService;

    private final ImageInstanceService imageInstanceService;

    private final ProjectService projectService;

    private final SecurityACLService securityACLService;

    private final UserAnnotationService userAnnotationService;

    private final TaskRunRepository taskRunRepository;

    private final TaskRunLayerRepository taskRunLayerRepository;

    public ResponseEntity<String> addTaskRun(Long projectId, String uri, JsonNode body) {
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
        ImageInstance image = imageInstanceService.get(body.get("image").asLong());

        TaskRun taskRun = new TaskRun();
        taskRun.setUser(currentUser);
        taskRun.setProject(project);
        taskRun.setTaskRunId(taskId);
        taskRun.setImage(image);
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

        ResponseEntity<String> response = appEngineService.get("task-runs/" + taskRunId.toString() + "/outputs");
        Optional<TaskRun> taskRun = taskRunRepository.findTaskRunByProjectIdAndTaskRunId(projectId, taskRunId);
        if (taskRun.isEmpty()) {
            throw new ObjectNotFoundException("TaskRun", taskRunId);
        }

        List<TaskRunValue> outputs = new ArrayList<>();
        try {
            outputs = new ObjectMapper().readValue(response.getBody(), new TypeReference<List<TaskRunValue>>() {});
        } catch (JsonProcessingException e) {
            throw new ObjectNotFoundException("TaskRun", taskRunId);
        }

        List<String> geometries = outputs
            .stream()
            .map(output -> output.getValue())
            .filter(value -> value instanceof String geometry && geometryService.isGeometry(geometry))
            .map(value -> (String) value)
            .toList();

        if (!geometries.isEmpty()) {
            String layerName = "task-run-" + taskRunId.toString();
            AnnotationLayer annotationLayer = annotationLayerService.createAnnotationLayer(layerName);

            for (String geometry : geometries) {
                annotationService.createAnnotation(annotationLayer, geometryService.GeoJSONToWKT(geometry));
            }

            TaskRunLayer taskRunLayer = new TaskRunLayer();
            taskRunLayer.setAnnotationLayer(annotationLayer);
            taskRunLayer.setTaskRun(taskRun.get());
            taskRunLayer.setImage(taskRun.get().getImage());
            taskRunLayerRepository.saveAndFlush(taskRunLayer);
        }

        return response;
    }

    public ResponseEntity<String> getInputs(Long projectId, UUID taskRunId) {
        checkTaskRun(projectId, taskRunId);
        return appEngineService.get("task-runs/" + taskRunId.toString() + "/inputs");
    }
}
