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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class MetadataSearchService {

    private ElasticsearchOperations operations;

    public MetadataSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }
    
    public List<Long> search(JsonObject body) {
        List<FieldValue> imageIDs = (List<FieldValue>) ((List<Integer>) body.get("imageIds"))
            .stream()
            .map(x -> FieldValue.of(x))
            .collect(Collectors.toList());
        TermsQueryField termsQueryField = new TermsQueryField.Builder().value(imageIDs).build();
        Query byDomainId = TermsQuery.of(ts -> ts.field("domain_ident").terms(termsQueryField))._toQuery();

        List<Query> subqueries = new ArrayList<>();
        HashMap<String, Object> filters = (HashMap) body.get("filters");
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            Query byValue = QueryStringQuery.of(qs -> qs
                    .query(String.format("*%s*", entry.getValue()))
                    .defaultField("value"))
                ._toQuery();

            Query byKey = MatchQuery.of(m -> m
                    .field("key")
                    .query(entry.getKey()))
                ._toQuery();

            Query byKeyword = BoolQuery.of(b -> b
                .must(byValue)
                .must(byDomainId)
                .must(byKey)
            )._toQuery();

            subqueries.add(byKeyword);
        }

        NativeQuery query = NativeQuery.builder()
            .withAggregation("domain_id", Aggregation.of(a -> a.terms(ta -> ta.field("domain_ident"))))
            .withQuery(q -> q.bool(b -> b.should(subqueries)))
            .build();
        log.debug(String.format("Elasticsearch %s", query.getQuery()));

        SearchHits<Property> searchHits = operations.search(
            query,
            Property.class,
            IndexCoordinates.of("properties")
        );
        log.debug(String.format("Total hits: %d", searchHits.getTotalHits()));

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

        Map<String, Long> buckets = aggregations
            .aggregations()
            .get(0)
            .aggregation()
            .getAggregate()
            .lterms()
            .buckets()
            .array()
            .stream()
            .collect(Collectors.toMap(LongTermsBucket::key, LongTermsBucket::docCount));

        List<Long> IDs = new ArrayList<>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            if (entry.getValue() == filters.size()) {
                IDs.add(Long.valueOf(entry.getKey()));
            }
        }

        return IDs;
    }
}
