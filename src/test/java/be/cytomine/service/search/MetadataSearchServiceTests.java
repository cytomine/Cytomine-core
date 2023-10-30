package be.cytomine.service.search;

/* Copyright (c) 2009-2023. Authors: see NOTICE file.
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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.meta.Property;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import javax.transaction.Transactional;
import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class MetadataSearchServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ElasticsearchOperations operations;

    @Autowired
    MetadataSearchService metadataSearchService;

    @BeforeEach
    public void setup() {
        IndexOperations indexOperations = operations.indexOps(IndexCoordinates.of("properties"));
        indexOperations.create();
    }

    @AfterEach
    public void clean() {
        IndexOperations indexOperations = operations.indexOps(IndexCoordinates.of("properties"));
        indexOperations.delete();
    }

    @Test
    void list_all_images_by_string_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();
        Property p1 = builder.given_a_property(ai1, "key1", "value");
        Property p2 = builder.given_a_property(ai1, "key2", "2000");
        Property p3 = builder.given_a_property(ai2, "key3", "7000");

        for (Property p : Arrays.asList(p1, p2, p3)) {
            operations.save(p, IndexCoordinates.of("properties"));
        }

        HashMap<String, Object> query = new HashMap<>();
        query.put("key1", "val");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId(), ai2.getId()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualSuggestions = metadataSearchService.search(filters);
        List<Long> expectedSuggestions = Collections.singletonList(ai1.getId());

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_images_by_number_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();
        Property p1 = builder.given_a_property(ai1, "key1", "value");
        Property p2 = builder.given_a_property(ai1, "key2", "2000");

        for (Property p : Arrays.asList(p1, p2)) {
            operations.save(p, IndexCoordinates.of("properties"));
        }

        HashMap<String, Object> query = new HashMap<>();
        query.put("key2", Arrays.asList(1000, 2000));

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId(), ai2.getId()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualSuggestions = metadataSearchService.search(filters);
        List<Long> expectedSuggestions = Collections.singletonList(ai2.getId());

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_images_by_mixed_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();
        Property p1 = builder.given_a_property(ai1, "key1", "value");
        Property p2 = builder.given_a_property(ai1, "key2", "2000");

        for (Property p : Arrays.asList(p1, p2)) {
            operations.save(p, IndexCoordinates.of("properties"));
        }

        HashMap<String, Object> query = new HashMap<>();
        query.put("key1", "val");
        query.put("key2", Arrays.asList(1000, 2000));

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId(), ai2.getId()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualSuggestions = metadataSearchService.search(filters);
        List<Long> expectedSuggestions = Arrays.asList(ai1.getId(), ai2.getId());

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_no_suggestions_for_wrong_value() {
        AbstractImage ai = builder.given_an_abstract_image();
        String key = "key";
        Property p1 = builder.given_a_property(ai, key, "value1");
        Property p2 = builder.given_a_property(ai, key, "value2");

        operations.save(p1, IndexCoordinates.of("properties"));
        operations.save(p2, IndexCoordinates.of("properties"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion(key, "wrong");
        List<String> expectedSuggestions = new ArrayList<>();

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_no_suggestions_for_wrong_key() {
        AbstractImage ai = builder.given_an_abstract_image();
        String key = "key";
        Property p1 = builder.given_a_property(ai, key, "value1");
        Property p2 = builder.given_a_property(ai, key, "value2");

        operations.save(p1, IndexCoordinates.of("properties"));
        operations.save(p2, IndexCoordinates.of("properties"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("wrong", "");
        List<String> expectedSuggestions = new ArrayList<>();

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_suggestions_for_partial_value() {
        AbstractImage ai = builder.given_an_abstract_image();
        String key = "key";
        Property p1 = builder.given_a_property(ai, key, "value1");
        Property p2 = builder.given_a_property(ai, key, "value2");

        operations.save(p1, IndexCoordinates.of("properties"));
        operations.save(p2, IndexCoordinates.of("properties"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion(key, "val");
        List<String> expectedSuggestions = Arrays.asList("value1", "value2");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_exact_suggestion_for_value() {
        AbstractImage ai = builder.given_an_abstract_image();
        String key = "key";
        Property p1 = builder.given_a_property(ai, key, "value1");
        Property p2 = builder.given_a_property(ai, key, "value2");

        operations.save(p1, IndexCoordinates.of("properties"));
        operations.save(p2, IndexCoordinates.of("properties"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion(key, "value1");
        List<String> expectedSuggestions = Arrays.asList("value1");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_suggestions_for_empty_value() {
        AbstractImage ai = builder.given_an_abstract_image();
        String key = "key";
        Property p1 = builder.given_a_property(ai, key, "value1");
        Property p2 = builder.given_a_property(ai, key, "value2");
        Property p3 = builder.given_a_property(ai, key, "value3");

        operations.save(p1, IndexCoordinates.of("properties"));
        operations.save(p2, IndexCoordinates.of("properties"));
        operations.save(p3, IndexCoordinates.of("properties"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion(key, "");
        List<String> expectedSuggestions = Arrays.asList("value1", "value2", "value3");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }
}
