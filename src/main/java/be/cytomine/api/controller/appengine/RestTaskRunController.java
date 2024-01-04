package be.cytomine.api.controller.appengine;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.service.appengine.TaskRunService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController
@RequestMapping("/api/app-engine")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("${application.appEngine.enabled: false}")
public class RestTaskRunController extends RestCytomineController {

    @Autowired
    private TaskRunService taskRunService;

    @PostMapping("/project/{project}/tasks/{task}/runs")
    public ResponseEntity<String> add(
            @PathVariable String project,
            @PathVariable String task
    ) {
        return taskRunService.addTaskRun(Long.parseLong(project), "tasks/" + task + "/runs");
    }

    @PostMapping("/project/{project}/tasks/{namespace}/{version}/runs")
    public ResponseEntity<String> add(
            @PathVariable String project,
            @PathVariable String namespace,
            @PathVariable String version
    ) {
        return taskRunService.addTaskRun(Long.parseLong(project), "tasks/" + namespace + "/" + version + "/runs");
    }

    @PutMapping("/project/{project}/task-runs/{task}/input-provisions")
    public ResponseEntity<String> batchProvision(
            @RequestBody ArrayList<JsonObject> body,
            @PathVariable String project,
            @PathVariable String task
    ) {
        return taskRunService.batchProvisionTaskRun(body, Long.parseLong(project), UUID.fromString(task));
    }

    @PutMapping("/project/{project}/task-runs/{task}/input-provisions/{param_name}")
    public ResponseEntity<String> provision(
            @RequestBody JsonObject json,
            @PathVariable String project,
            @PathVariable String task,
            @PathVariable("param_name") String paramName
    ) {
        return taskRunService.provisionTaskRun(json, Long.parseLong(project), UUID.fromString(task), paramName);
    }
}
