package be.cytomine.api.controller.image.group;

/*
 * Copyright (c) 2009-2022. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.group.ImageGroupImageInstanceService;
import be.cytomine.service.image.group.ImageGroupService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageGroupController extends RestCytomineController {

    private final ImageGroupImageInstanceService imageGroupImageInstanceService;

    private final ImageGroupService imageGroupService;

    private final ProjectService projectService;

    @GetMapping("/project/{id}/imagegroup.json")
    public ResponseEntity<String> listByProject(@PathVariable Long id) {
        log.debug("REST request to list an imagegroup for project: {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));

        return responseSuccess(imageGroupService.list(project));
    }

    @PostMapping("/imagegroup.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save imagegroup: " + json);
        return add(imageGroupService, json);
    }

    @GetMapping("/imagegroup/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        log.debug("REST request to get imagegroup: " + id);

        ImageGroup group = imageGroupService.find(id).orElseThrow(() -> new ObjectNotFoundException("ImageGroup", id));
        group.setImages(imageGroupImageInstanceService.buildImageInstances(group));

        return responseSuccess(group);
    }

    @PutMapping("/imagegroup/{id}.json")
    public ResponseEntity<String> edit(@PathVariable Long id, @RequestBody JsonObject json) {
        log.debug("REST request to edit imagegroup: " + id);
        return update(imageGroupService, json);
    }

    @DeleteMapping("/imagegroup/{id}.json")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        log.debug("REST request to delete imagegroup: " + id);
        return delete(imageGroupService, JsonObject.of("id", id), null);
    }
}
