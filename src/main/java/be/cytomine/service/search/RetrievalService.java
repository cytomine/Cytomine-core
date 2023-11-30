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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.search.RetrievalServer;
import be.cytomine.service.ModelService;
import be.cytomine.utils.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static be.cytomine.utils.HttpUtils.getContentFromUrl;

@Service
@Slf4j
public class RetrievalService extends ModelService {

    private final ApplicationProperties applicationProperties;

    public RetrievalService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Class currentDomain() {
        return RetrievalServer.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new RetrievalServer().buildDomainFromJson(json, getEntityManager());
    }

    public String indexAnnotation(Long id) throws IOException {
        String url = applicationProperties.getRetrievalServerURL() + "/images/index";

        return getContentFromUrl(url);
    }

    public List<Long> retrieveSimilarImages(Long id) throws IOException {
        String url = applicationProperties.getRetrievalServerURL() + "/images/retrieve";
        String response = getContentFromUrl(url);

        return new ArrayList<>();
    }
}
