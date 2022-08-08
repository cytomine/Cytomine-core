package be.cytomine.api.controller.ontology;

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
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.group.ImageGroupService;
import be.cytomine.service.ontology.AnnotationGroupService;
import be.cytomine.service.ontology.AnnotationLinkService;
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
public class RestAnnotationGroupController extends RestCytomineController {

    private final AnnotationGroupService annotationGroupService;

    private final AnnotationLinkService annotationLinkService;

    private final ImageGroupService imageGroupService;

    private final ProjectService projectService;

    @PostMapping("/annotationgroup.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save annotationgroup: " + json);
        return add(annotationGroupService, json);
    }

    @GetMapping("/api/annotationgroup/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        log.debug("REST request to show annotationgroup: " + id);
        return annotationGroupService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AnnotationGroup", id));
    }

    @PutMapping("/api/annotationgroup/{id}.json")
    public ResponseEntity<String> edit(@PathVariable Long id, @RequestBody JsonObject json) {
        log.debug("REST request to edit annotationgroup: " + id);
        return update(annotationGroupService, json);
    }

    @DeleteMapping("/api/annotationgroup/{id}.json")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        log.debug("REST request to delete annotationgroup: " + id);
        return delete(annotationGroupService, JsonObject.of("id", id), null);
    }

    @GetMapping("/api/project/{id}/annotationgroup.json")
    public ResponseEntity<String> listByProject(@PathVariable Long id) {
        log.debug("REST request to list annotationgroup for project " + id);
        Project project = projectService.find(id).orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(annotationGroupService.list(project));
    }

    @GetMapping("/api/imagegroup/{id}/annotationgroup.json")
    public ResponseEntity<String> listByImageGroup(@PathVariable Long id) {
        log.debug("REST request to list annotationgroup for imagegroup " + id);
        ImageGroup group = imageGroupService.find(id).orElseThrow(() -> new ObjectNotFoundException("ImageGroup", id));
        return responseSuccess(annotationGroupService.list(group));
    }

    /*
    @PostMapping("/api/annotationgroup/{id}/annotationgroup/{mergedId}/merge.json")
    public ResponseEntity<String> merge(@PathVariable Long id, @PathVariable Long mergedId) {
        log.debug("REST request to merge annotationgroup {} with annotationgroup {}", id, mergedId);
        return responseSuccess(annotationGroupService.merge(id, mergedId));
    }
     */

    /* getAnnotationDomain() not implemented see L197 AnnotationDomain.java
    @GetMapping("/api/annotationgroup/{annotationGroup}/annotation/{annotation}.json")
    public ResponseEntity<String> show(@PathVariable Long annotation, @PathVariable Long annotationGroup) {
        log.debug("REST request to get annotation {} in annotationgroup {}", annotation, annotationGroup);

        AnnotationDomain annotationDomain = AnnotationDomain.getAnnotationDomain(annotation);
        AnnotationGroup group = annotationGroupService.get(annotationGroup);

        if (annotationDomain == null) {
            return responseNotFound("Annotation", annotation);
        }

        if (group == null) {
            return responseNotFound("AnnotationGroup", annotationGroup);
        }

        AnnotationLink link = annotationLinkService.get(group, annotationDomain);
        if (link == null) {
            return responseNotFound("AnnotationLink", Map.of("Annotation", annotation, "AnnotationGroup", annotationGroup));
        }

        return null;
    }
     */

    @DeleteMapping("/api/annotationgroup/{annotationGroup}/annotation/{annotation}.json")
    public ResponseEntity<String> delete(@PathVariable Long annotation, @PathVariable Long annotationGroup) {
        log.debug("REST request to delete annotation {} in annotationgroup {}", annotation, annotationGroup);
        return delete(imageGroupService, JsonObject.of("annotationId", annotation, "annotationGroup", annotationGroup), null);
    }

    @PostMapping("/api/annotationlink.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save annotationlink: " + json);
        return add(annotationLinkService, json);
    }

    @GetMapping("/api/annotationgroup/{id}/annotationlink.json")
    public ResponseEntity<String> listByAnnotationGroup(@PathVariable Long id) {
        log.debug("REST request to list all annotationlinks for annotationgroup " + id);
        AnnotationGroup annotationGroup = annotationGroupService.find(id).orElseThrow(() -> new ObjectNotFoundException("AnnotationGroup", id));
        return responseSuccess(annotationLinkService.list(annotationGroup));
    }

    /* getAnnotationDomain() not implemented see L197 AnnotationDomain.java
    @GetMapping("/api/annotation/{id}/annotationlink.json")
    public ResponseEntity<String> listByAnnotation(@PathVariable Long id) {
        log.debug("REST request to list all annotationlinks for annotation " + id);

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(id);
        if (annotation == null) {
            return responseNotFound("AnnotationLink", "Annotation", id);
        }
        AnnotationLink link = annotationLinkRepository.findByAnnotationId(id);
        return responseSuccess(annotationLinkService.list(link.getAnnotationGroup()));
    }
     */
}
