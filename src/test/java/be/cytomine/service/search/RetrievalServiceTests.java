package be.cytomine.service.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.vividsolutions.jts.io.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.api.controller.ontology.UserAnnotationResourceTests;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.dto.search.SearchResponse;
import be.cytomine.service.dto.CropParameter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
public class RetrievalServiceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private RetrievalService retrievalService;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8888);
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    public void index_annotation_with_success() throws ParseException, UnsupportedEncodingException {
        UserAnnotation annotation = UserAnnotationResourceTests.given_a_user_annotation_with_valid_image_server(builder);

        /* Simulate call to PIMS */
        String id = URLEncoder.encode(annotation.getSlice().getBaseSlice().getPath(), StandardCharsets.UTF_8);
        String url = "/image/" + id + "/annotation/crop";
        String body = "{\"annotations\":{},\"level\":0,\"background_transparency\":0,\"z_slices\":0,\"timepoints\":0}";

        wireMockServer.stubFor(WireMock.post(urlEqualTo(url))
            .withRequestBody(
                WireMock.equalToJson(body)
            )
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody(UUID.randomUUID().toString().getBytes())
            )
        );

        /* Simulate call to CBIR */
        String expectedUrlPath = "/api/images";
        String expectedResponseBody = "{ \"ids\": [" + annotation.getId() + "]";
        expectedResponseBody += ", \"storage\": " + annotation.getProject().getId().toString();
        expectedResponseBody += ", \"index\": \"annotation\" }";

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(expectedResponseBody)
            )
        );

        /* Test index annotation method */
        CropParameter parameters = new CropParameter();
        parameters.setFormat("png");
        ResponseEntity<String> response = retrievalService.indexAnnotation(annotation, parameters, "");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponseBody, response.getBody());

        wireMockServer.verify(WireMock.postRequestedFor(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation")));
    }

    @Test
    public void delete_index_with_success() {
        UserAnnotation annotation = builder.given_a_user_annotation();

        /* Simulate call to CBIR */
        String expectedUrlPath = "/api/images/" + annotation.getId();
        String expectedResponseBody = "{ \"id\": " + annotation.getId();
        expectedResponseBody += ", \"storage\": " + annotation.getProject().getId().toString();
        expectedResponseBody += ", \"index\": \"annotation\" }";

        wireMockServer.stubFor(WireMock.delete(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(expectedResponseBody)
            )
        );

        /* Test delete index method */
        ResponseEntity<String> response = retrievalService.deleteIndex(annotation);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponseBody, response.getBody());

        wireMockServer.verify(WireMock.deleteRequestedFor(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation")));
    }

    @Test
    public void search_similar_images_with_success() throws JsonProcessingException, ParseException,
        UnsupportedEncodingException {
        UserAnnotation annotation =
            UserAnnotationResourceTests.given_a_user_annotation_with_valid_image_server(builder);

        /* Simulate call to PIMS */
        String id = URLEncoder.encode(annotation.getSlice().getBaseSlice().getPath(), StandardCharsets.UTF_8);
        String url = "/image/" + id + "/annotation/crop";
        String body = "{\"annotations\":{},\"level\":0,\"background_transparency\":0,\"z_slices\":0,\"timepoints\":0}";

        wireMockServer.stubFor(WireMock.post(urlEqualTo(url))
            .withRequestBody(
                WireMock.equalToJson(body)
            )
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody(UUID.randomUUID().toString().getBytes())
            )
        );

        /* Simulate call to CBIR */
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedUrlPath = "/api/search";
        SearchResponse expectedResponse = new SearchResponse(
            annotation.getId().toString(),
            annotation.getProject().getId().toString(),
            "annotation",
            Arrays.asList(
                Arrays.asList(annotation.getId().toString(), 0.0),
                Arrays.asList("1", 123.0),
                Arrays.asList("2", 456.0)
            )
        );

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(expectedResponse))
            )
        );

        /* Test retrieve similar images method */
        Long neighbours = 2L;
        CropParameter parameters = new CropParameter();
        parameters.setFormat("png");

        ResponseEntity<SearchResponse> response = retrievalService.retrieveSimilarImages(
            annotation,
            parameters,
            "",
            neighbours
        );

        expectedResponse.setSimilarities(
            Arrays.asList(
                Arrays.asList("1", 73.02631578947368),
                Arrays.asList("2", 0.0)
            )
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }
}
