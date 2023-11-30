package be.cytomine.api.controller.search;

/*
 * Copyright (c) 2009-2023. Authors: see NOTICE file.
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
import be.cytomine.service.search.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestRetrievalController extends RestCytomineController {

    private final RetrievalService retrievalService;

    @GetMapping("/retrieval/index.json")
    public ResponseEntity<String> indexAnnotation(@RequestParam(value = "annotation") Long id) throws IOException {
        log.debug("REST request to index an annotation");
        return responseSuccess(retrievalService.indexAnnotation(id));
    }

    @GetMapping("/retrieval/retrieve.json")
    public ResponseEntity<String> retrieveSimilarAnnotations(@RequestParam(value = "annotation") Long id) throws IOException {
        log.debug("REST request to retrieve similar annotations given a query annotation {}", id);
        return responseSuccess(retrievalService.retrieveSimilarImages(id));
    }
}
