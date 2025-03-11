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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.transaction.Transactional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.WireMockHelper;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class MetadataSearchServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    MetadataSearchService metadataSearchService;

    private WireMockServer wireMockServer;

    private WireMockHelper wireMockHelper;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(9200); // Use port 9200 as it is the default Elasticsearch port
        wireMockServer.start();
        wireMockHelper = new WireMockHelper(wireMockServer);

        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    public void clean() {
        wireMockServer.stop();
    }

    @Test
    void list_all_images_by_string_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();

        HashMap<String, Object> query = new HashMap<>();
        query.put("test-key", "test-value");

        wireMockHelper.stubElasticSearchApi(ai1.getId(), query.size());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId().intValue(), ai2.getId().intValue()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualIDs = metadataSearchService.search(filters);
        List<Long> expectedIDs = List.of(ai1.getId());

        assertThat(actualIDs).isEqualTo(expectedIDs);
    }

    @Test
    void list_all_images_by_number_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();

        HashMap<String, Object> query = new HashMap<>();
        query.put("test-key", Arrays.asList(0, 2000));

        wireMockHelper.stubElasticSearchApi(ai1.getId(), query.size());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId().intValue(), ai2.getId().intValue()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualIDs = metadataSearchService.search(filters);
        List<Long> expectedIDs = List.of(ai1.getId());

        assertThat(actualIDs).isEqualTo(expectedIDs);
    }

    @Test
    void list_all_images_by_mixed_filters() {
        AbstractImage ai1 = builder.given_an_abstract_image();
        AbstractImage ai2 = builder.given_an_abstract_image();

        HashMap<String, Object> query = new HashMap<>();
        query.put("key1", "val");
        query.put("test-key", Arrays.asList(0, 2000));

        wireMockHelper.stubElasticSearchApi(ai1.getId(), query.size());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("imageIds", Arrays.asList(ai1.getId().intValue(), ai2.getId().intValue()));
        parameters.put("filters", query);

        JsonObject filters = new JsonObject(parameters);

        List<Long> actualIDs = metadataSearchService.search(filters);
        List<Long> expectedIDs = List.of(ai1.getId());

        assertThat(actualIDs).isEqualTo(expectedIDs);
    }

    @Test
    void list_no_suggestions_for_wrong_value() {
        wireMockHelper.stubElasticAutoCompleteApiReturnEmpty();

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("key", "wrong");
        List<String> expectedSuggestions = List.of();

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_no_suggestions_for_wrong_key() {
        wireMockHelper.stubElasticAutoCompleteApiReturnEmpty();

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("wrong", "");
        List<String> expectedSuggestions = List.of();

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_suggestions_for_partial_value() {
        wireMockHelper.stubElasticAutoCompleteApiReturnResults(List.of("value1", "value2"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("key", "val");
        List<String> expectedSuggestions = List.of("value1", "value2");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_exact_suggestion_for_value() {
        wireMockHelper.stubElasticAutoCompleteApiReturnResults(List.of("value1"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("key", "value1");
        List<String> expectedSuggestions = List.of("value1");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }

    @Test
    void list_all_suggestions_for_empty_value() {
        wireMockHelper.stubElasticAutoCompleteApiReturnResults(List.of("value1", "value2", "value3"));

        List<String> actualSuggestions = metadataSearchService.searchAutoCompletion("key", "");
        List<String> expectedSuggestions = List.of("value1", "value2", "value3");

        assertThat(actualSuggestions).isEqualTo(expectedSuggestions);
    }
}
