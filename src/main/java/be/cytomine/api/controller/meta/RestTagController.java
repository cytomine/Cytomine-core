package be.cytomine.api.controller.meta;

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
import be.cytomine.repository.meta.TagRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.meta.TagService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestTagController extends RestCytomineController {

    private final TagService tagService;

    private final TagRepository tagRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    @GetMapping("/tag.json")
    public ResponseEntity<String> list() {
        log.debug("REST request to list tags");
        return responseSuccess(tagService.list());
    }

    @GetMapping("/tag/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Tag : {}", id);
        return tagService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Tag", id));
    }


    @PostMapping("/tag.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save Tag : " + json);
        return add(tagService, json);
    }

    @PutMapping("/tag/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit Tag : " + id);
        return update(tagService, json);
    }

    @DeleteMapping("/tag/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestParam(required = false) Long task) {
        log.debug("REST request to delete Tag : " + id);
        Task existingTask = taskService.get(task);
        return delete(tagService, JsonObject.of("id", id), existingTask);
    }
}
