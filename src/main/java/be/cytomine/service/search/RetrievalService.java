package be.cytomine.service.search;

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
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.io.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.search.RetrievalServer;
import be.cytomine.dto.PimsResponse;
import be.cytomine.dto.search.SearchResponse;
import be.cytomine.service.ModelService;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;

@Slf4j
@Service
public class RetrievalService extends ModelService {

    private final ImageServerService imageServerService;

    private final RestTemplate restTemplate;

    private final String baseUrl;

    private final String indexName = "annotation";

    public RetrievalService(
        ApplicationProperties applicationProperties,
        ImageServerService imageServerService,
        RestTemplate restTemplate
    ) {
        this.imageServerService = imageServerService;
        this.restTemplate = restTemplate;
        this.baseUrl = applicationProperties.getRetrievalServerURL();
    }

    @Override
    public Class currentDomain() {
        return RetrievalServer.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new RetrievalServer().buildDomainFromJson(json, getEntityManager());
    }

    private HttpEntity<MultiValueMap<String, Object>> createMultipartRequestEntity(
        AnnotationDomain annotation,
        CropParameter parameters,
        String etag
    ) throws ParseException, UnsupportedEncodingException {
        // Request annotation crop from PIMS
        PimsResponse crop = imageServerService.crop(annotation.getSlice().getBaseSlice(), parameters, etag);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(crop.getContent()) {
            @Override
            public String getFilename() {
                return annotation.getId().toString();
            }
        });

        return new HttpEntity<>(body, headers);
    }

    public ResponseEntity<String> indexAnnotation(
        AnnotationDomain annotation,
        CropParameter parameters,
        String etag
    ) throws ParseException, UnsupportedEncodingException {
        String storageName = annotation.getProject().getId().toString();
        String url = UriComponentsBuilder
            .fromHttpUrl(this.baseUrl + "/api/images")
            .queryParam("storage", storageName)
            .queryParam("index", this.indexName)
            .toUriString();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createMultipartRequestEntity(
            annotation,
            parameters,
            etag
        );

        return this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    public ResponseEntity<String> deleteIndex(AnnotationDomain annotation) {
        String url = UriComponentsBuilder
            .fromHttpUrl(this.baseUrl + "/api/images/" + annotation.getId())
            .queryParam("storage", annotation.getProject().getId())
            .queryParam("index", this.indexName)
            .toUriString();

        log.debug("Sending DELETE request to {}", url);
        return restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            String.class
        );
    }

    private List<List<Object>> processSimilarities(List<List<Object>> similarities, double maxDistance) {
        List<List<Object>> percentages = new ArrayList<>();

        for (List<Object> entry : similarities) {
            String item = (String) entry.get(0);
            Double distance = (Double) entry.get(1);
            Double percentage = (1 - (distance / maxDistance)) * 100;

            percentages.add(List.of(item, percentage));
        }

        return percentages;
    }

    public ResponseEntity<SearchResponse> retrieveSimilarImages(
        AnnotationDomain annotation,
        CropParameter parameters,
        String etag,
        Long nrt_neigh
    ) throws ParseException, UnsupportedEncodingException {
        String url = UriComponentsBuilder
            .fromHttpUrl(this.baseUrl + "/api/search")
            .queryParam("storage", annotation.getProject().getId())
            .queryParam("index", this.indexName)
            .queryParam("nrt_neigh", nrt_neigh + 1)
            .toUriString();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createMultipartRequestEntity(
            annotation,
            parameters,
            etag
        );

        log.debug("Sending request to {} with entity {}", url, requestEntity);
        ResponseEntity<SearchResponse> response = this.restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            SearchResponse.class
        );
        log.debug("Receiving response {}", response);

        SearchResponse searchResponse = response.getBody();
        if (searchResponse == null) {
            return response;
        }

        searchResponse.getSimilarities().remove(0);

        double maxDistance = searchResponse
            .getSimilarities()
            .stream()
            .mapToDouble(d -> (Double) d.get(1))
            .max()
            .orElse(1.0);
        searchResponse.setSimilarities(processSimilarities(searchResponse.getSimilarities(), maxDistance));

        return new ResponseEntity<>(searchResponse, HttpStatus.OK);
    }
}
