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

import java.io.UnsupportedEncodingException;
import javax.persistence.EntityManager;

import com.vividsolutions.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.search.SearchResponse;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.search.RetrievalService;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RestRetrievalController extends RestCytomineController {

    private final EntityManager entityManager;

    private final RetrievalService retrievalService;

    private CropParameter getParameters(String location) {
        CropParameter parameters = new CropParameter();
        parameters.setComplete(true);
        parameters.setDraw(true);
        parameters.setFormat("png");
        parameters.setIncreaseArea(1.25);
        parameters.setLocation(location);
        parameters.setMaxSize(256);

        return parameters;
    }

    @GetMapping("/retrieval/index")
    public ResponseEntity<String> indexAnnotation(
        @RequestParam(value = "annotation") Long id
    ) throws ParseException, UnsupportedEncodingException {
        log.debug("REST request to index an annotation");

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, id);
        CropParameter parameters = getParameters(annotation.getWktLocation());

        return retrievalService.indexAnnotation(annotation, parameters, getRequestETag());
    }

    @GetMapping("/retrieval/search")
    public ResponseEntity<SearchResponse> retrieveSimilarAnnotations(
        @RequestParam(value = "annotation") Long id,
        @RequestParam(value = "nrt_neigh") Long nrt_neigh
    ) throws ParseException, UnsupportedEncodingException {
        log.debug("REST request to retrieve similar annotations given a query annotation {}", id);

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, id);
        CropParameter parameters = getParameters(annotation.getWktLocation());

        return retrievalService.retrieveSimilarImages(
            annotation,
            parameters,
            getRequestETag(),
            nrt_neigh
        );
    }
}
