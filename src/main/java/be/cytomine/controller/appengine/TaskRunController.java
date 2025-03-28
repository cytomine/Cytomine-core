package be.cytomine.controller.appengine;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.cytomine.service.appengine.TaskRunService;

@ConditionalOnExpression("${application.appEngine.enabled: false}")
@RequiredArgsConstructor
@RequestMapping("/api/app-engine")
@RestController
public class TaskRunController {

    private final TaskRunService taskRunService;

    @PostMapping("/project/{project}/tasks/{task}/runs")
    public ResponseEntity<String> add(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.addTaskRun(project, "tasks/" + task + "/runs");
    }

    @PostMapping("/project/{project}/tasks/{namespace}/{version}/runs")
    public ResponseEntity<String> add(
        @PathVariable Long project,
        @PathVariable String namespace,
        @PathVariable String version
    ) {
        return taskRunService.addTaskRun(project, "tasks/" + namespace + "/" + version + "/runs");
    }

    @GetMapping("/project/{project}/task-runs/{task}")
    public ResponseEntity<String> get(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getTaskRun(project, task);
    }

    @PutMapping("/project/{project}/task-runs/{task}/input-provisions")
    public ResponseEntity<String> batchProvision(
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody List<JsonNode> body
    ) {
        return taskRunService.batchProvisionTaskRun(body, project, task);
    }

    @PutMapping("/project/{project}/task-runs/{task}/input-provisions/{parameter-name}")
    public ResponseEntity<String> provision(
        @PathVariable("parameter-name") String parameterName,
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody JsonNode json
    ) {
        return taskRunService.provisionTaskRun(json, project, task, parameterName);
    }

    @PostMapping("/project/{project}/task-runs/{task}/state-actions")
    public ResponseEntity<String> stateAction(
        @PathVariable Long project,
        @PathVariable UUID task,
        @RequestBody JsonNode body
    ) {
        return taskRunService.postStateAction(body, project, task);
    }

    @GetMapping("/project/{project}/task-runs/{task}/outputs")
    public ResponseEntity<String> getOutputs(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getOutputs(project, task);
    }

    @GetMapping("/project/{project}/task-runs/{task}/inputs")
    public ResponseEntity<String> getInputs(
        @PathVariable Long project,
        @PathVariable UUID task
    ) {
        return taskRunService.getInputs(project, task);
    }
}
