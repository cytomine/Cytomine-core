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
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.AnnotationLink;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationLinkRepository;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.ontology.AnnotationGroupService;
import be.cytomine.service.ontology.AnnotationLinkService;
import be.cytomine.utils.JsonObject;
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
public class RestAnnotationLinkController extends RestCytomineController {

    final private AnnotationGroupService annotationGroupService;

    final private AnnotationLinkService annotationLinkService;

    final private TransactionService transactionService;

    final private AnnotationLinkRepository annotationLinkRepository;

    @PostMapping("/annotationlink.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        log.debug("REST request to save annotationlink: " + json);
        return add(annotationLinkService, json);
    }

    @GetMapping("/annotationgroup/{id}/annotationlink.json")
    public ResponseEntity<String> listByAnnotationGroup(@PathVariable Long id) {
        log.debug("REST request to list all annotationlinks for annotationgroup " + id);
        AnnotationGroup annotationGroup = annotationGroupService.find(id).orElseThrow(() -> new ObjectNotFoundException("AnnotationGroup", id));
        return responseSuccess(annotationLinkService.list(annotationGroup));
    }

    @GetMapping("/annotation/{id}/annotationlink.json")
    public ResponseEntity<String> listByAnnotation(@PathVariable Long id) {
        log.debug("REST request to list all annotationlinks for annotation " + id);

        // TODO: Check if annotation exists

        AnnotationLink link = annotationLinkRepository.findByAnnotationIdent(id).orElse(null);
        if (link == null) {
            return responseSuccess(new ArrayList<>());
        }

        return responseSuccess(annotationLinkService.list(link.getGroup()));
    }

    /* getAnnotationDomain() not implemented see L197 AnnotationDomain.java
    @GetMapping("/annotationgroup/{annotationGroup}/annotation/{annotation}.json")
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

    @DeleteMapping("/annotationgroup/{annotationGroup}/annotation/{annotation}.json")
    public ResponseEntity<String> delete(@PathVariable Long annotation, @PathVariable Long annotationGroup) {
        log.debug("REST request to delete annotation {} in annotationgroup {}", annotation, annotationGroup);

        AnnotationLink link = annotationLinkRepository.findByAnnotationIdent(annotation).orElse(null);
        if (link == null) {
            return responseNotFound("Annotationlink", Map.of("Annotation", annotation, "AnnotationGroup", annotationGroup));
        }

        return responseSuccess(annotationLinkService.delete(link, transactionService.start(), null, true));
    }
}
