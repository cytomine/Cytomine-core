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

import be.cytomine.domain.meta.Property;
import be.cytomine.utils.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
@Slf4j
@Transactional
public class MetadataSearchService {

    private ElasticsearchOperations operations;

    public MetadataSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    private StringQuery buildStringQuery(JsonObject parameters) {
        String prefixQuery = "{\"dis_max\": { \"queries\": [";
        String suffixQuery = "]}}";
        String termsString = String.format("{ \"terms\": { \"domain_ident\": %s } },", parameters.get("imageIds"));
        List<String> queries = new LinkedList<>();

        HashMap<String, Object> filters = (HashMap) parameters.get("filters");
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String queryString = String.format(
                "{ \"query_string\": { \"query\": \"*%s*\", \"default_field\": \"value\" } },",
                entry.getValue()
            );
            String matchString = String.format("{\"match\": {\"key\": \"%s\"}}", entry.getKey());
            queries.add(String.format("{\"bool\": { \"must\": [ %s ] } }", queryString + termsString + matchString));
        }

        String query = prefixQuery + String.join(",", queries) + suffixQuery;

        log.debug("Elasticsearch query: " + query);

        return new StringQuery(query);
    }

    public List<Long> search(JsonObject body) {
        SearchHits<Property> searchHits = operations.search(
            this.buildStringQuery(body),
            Property.class,
            IndexCoordinates.of("properties")
        );
        log.debug(String.format("Total hits: %d", searchHits.getTotalHits()));

        Set<Long> ids = new HashSet<>();

        for (SearchHit<Property> hit : searchHits) {
            ids.add(hit.getContent().getDomainIdent());
        }

        return ids.stream().toList();
    }
}
