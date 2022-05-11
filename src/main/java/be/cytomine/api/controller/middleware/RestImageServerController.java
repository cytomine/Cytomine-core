package be.cytomine.api.controller.middleware;

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
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageServerController extends RestCytomineController {

    private final ImageServerService imageServerService;

    @GetMapping("/imageserver.json")
    public ResponseEntity<String> list(
    ) {
        log.debug("REST request to list terms");
        return responseSuccess(imageServerService.list());
    }

    @GetMapping("/imageserver/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get imageserver : {}", id);
        return imageServerService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("imageserver", id));
    }

    @GetMapping("/imageserver/{id}/format.json")
    public ResponseEntity<String> formats(
            @PathVariable Long id
    ) throws IOException {
        log.debug("REST request to list format");
        ImageServer imageserver = imageServerService.find(id).orElseThrow(() -> new ObjectNotFoundException("imageserver", id));
        return responseSuccess(imageServerService.formats(imageserver));
    }

    @GetMapping("/imageserver/format.json")
    public ResponseEntity<String> allFormats(
    ) throws IOException {
        log.debug("REST request to list allFormats");
        List<ImageServer> imageServers = imageServerService.list();
        Set<String> alreadyAdd = new HashSet<>();
        List<Map<String, Object>> formats = new ArrayList<>();
        for (ImageServer imageServer : imageServers) {
            List<Map<String, Object>> formatsServer = imageServerService.formats(imageServer);
            for (Map<String, Object> format : formatsServer) {
                if(!alreadyAdd.contains(format.getOrDefault("id", ""))) {
                    formats.add(format);
                    alreadyAdd.add((String) format.getOrDefault("id", ""));
                }
            }
        }
        return responseSuccess(formats.stream().distinct().toList());
    }
}
