package be.cytomine.controller.ontology;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.locationtech.jts.io.ParseException;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadminjob")
public class AlgoAnnotationResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAlgoAnnotationControllerMockMvc;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {}
    }

    @Test
    @Transactional
    public void get_a_algo_annotation() throws Exception {

        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        em.refresh(algoAnnotationTerm);
        AlgoAnnotation algoAnnotation = em.find(AlgoAnnotation.class, algoAnnotationTerm.getAnnotationIdent());
        restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{id}.json", algoAnnotationTerm.getAnnotationIdent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(algoAnnotation.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.ontology.AlgoAnnotation"))
                .andExpect(jsonPath("$.created").value(algoAnnotation.getCreated().getTime()))
                .andExpect(jsonPath("$.location").value(algoAnnotation.getWktLocation()))
                .andExpect(jsonPath("$.image").value(algoAnnotation.getImage().getId().intValue()))
                .andExpect(jsonPath("$.project").value(algoAnnotation.getProject().getId().intValue()))
                .andExpect(jsonPath("$.user").value(algoAnnotation.getUser().getId()))
                .andExpect(jsonPath("$.centroid.x").exists())
                .andExpect(jsonPath("$.centroid.y").exists())
                .andExpect(jsonPath("$.term[0]").value(algoAnnotationTerm.getTerm().getId()))
        ;
    }

    @Test
    @Transactional
    public void get_a_algo_annotation_not_exists() throws Exception {
        restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{id}.json", 0))
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    @WithMockUser("superadmin")
    public void list_annotations_light() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        builder.addUserToProject(algoAnnotation.getProject(), builder.given_superadmin().getUsername());
        restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+algoAnnotation.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void count_annotations_by_project_with_dates() throws Exception {
        AlgoAnnotation oldAlgoAnnotation = builder.given_a_algo_annotation();
        oldAlgoAnnotation.setCreated(DateUtils.addDays(new Date(), -1));

        AlgoAnnotation newAlgoAnnotation =
                builder.persistAndReturn(builder.given_a_not_persisted_algo_annotation(oldAlgoAnnotation.getProject()));


        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(oldAlgoAnnotation.getCreated().getTime()))
                        .param("endDate", String.valueOf(newAlgoAnnotation.getCreated().getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(oldAlgoAnnotation.getCreated(),-1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(newAlgoAnnotation.getCreated(),1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(newAlgoAnnotation.getCreated(),-1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(oldAlgoAnnotation.getCreated(),1).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restAlgoAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/algoannotation/count.json", oldAlgoAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addDays(oldAlgoAnnotation.getCreated(), -2).getTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @Transactional
    public void add_valid_algo_annotation() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        restAlgoAnnotationControllerMockMvc.perform(post("/api/algoannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(algoAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.algoannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAlgoAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotation.id").exists());

    }


    @Test
    @Transactional
    public void add_algo_annotation_with_not_valid_location() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("location",
                "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, 226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, 225.73582220103702 306.89723126347087))"
                ); // too small
        restAlgoAnnotationControllerMockMvc.perform(post("/api/algoannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    public void add_valid_algo_annotation_without_project() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.remove("project");
        restAlgoAnnotationControllerMockMvc.perform(post("/api/algoannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annotation.project").value(algoAnnotation.getProject().getId()));
        // => project is retrieve from slice/image
    }

    @Test
    @Transactional
    public void add_valid_algo_annotation_with_terms() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        Term term1 = builder.given_a_term(algoAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(algoAnnotation.getProject().getOntology());
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("term", Arrays.asList(term1.getId(), term2.getId()));
        restAlgoAnnotationControllerMockMvc.perform(post("/api/algoannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annotation.term", hasSize(2)));
    }


    @Test
    @Transactional
    public void edit_valid_algo_annotation() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        restAlgoAnnotationControllerMockMvc.perform(put("/api/algoannotation/{id}.json", algoAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(algoAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.algoannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditAlgoAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotation.id").exists());

    }


    @Test
    @Transactional
    public void delete_algo_annotation() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        restAlgoAnnotationControllerMockMvc.perform(delete("/api/algoannotation/{id}.json", algoAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(algoAnnotation.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.algoannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAlgoAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotation.id").exists());

    }


    @Test
    @Transactional
    public void delete_algo_annotation_not_exist_fails() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        restAlgoAnnotationControllerMockMvc.perform(delete("/api/algoannotation/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(algoAnnotation.toJSON()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_algo_annotation_crop() throws Exception {
        AlgoAnnotation annotation = given_a_algo_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"length\":512,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );


        MvcResult mvcResult = restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{id}/crop.png?maxSize=512", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_algo_annotation_crop_mask() throws Exception {
        AlgoAnnotation annotation = given_a_algo_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content
        String url = "/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/mask";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\",\"fill_color\":\"#fff\"}],\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{id}/mask.png", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Disabled("Randomly fail with ProxyExchange, need to find a solution")
    @Test
    @jakarta.transaction.Transactional
    public void get_algo_annotation_alpha_mask() throws Exception {
        AlgoAnnotation annotation = given_a_algo_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/"+ URLEncoder.encode(annotation.getImage().getBaseImage().getPath() , StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"level\":0,\"z_slices\":0,\"annotations\":[{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"}],\"timepoints\":0,\"background_transparency\":100}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(IMS_API_BASE_PATH + url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{id}/alphamask.png", annotation.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    public static AlgoAnnotation given_a_algo_annotation_with_valid_image_server(BasicInstanceBuilder builder) throws ParseException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        ImageInstance imageInstance = builder.given_an_image_instance(image, builder.given_a_project());
        imageInstance.setInstanceFilename("CMU-2");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        SliceInstance sliceInstance = builder.given_a_slice_instance(imageInstance, slice);
        AlgoAnnotation algoAnnotation
                = builder.given_a_algo_annotation(
                        sliceInstance,
                "POLYGON((1 1,50 10,50 50,10 50,1 1))", builder.given_superadmin_job(), null);
        return algoAnnotation;
    }


    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
    public void create_comments_for_annotation() throws Exception {
        SharedAnnotation annotation = builder.given_a_shared_annotation();
        annotation.setAnnotation(builder.given_a_algo_annotation());
        JsonObject jsonObject = annotation.toJsonObject();
        jsonObject.put("subject", "subject for test mail");
        jsonObject.put("message", "message for test mail");
        jsonObject.put("users", List.of(builder.given_superadmin().getId()));

        restAlgoAnnotationControllerMockMvc.perform(post("/api/algoannotation/{id}/comment.json", annotation.getAnnotationIdent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddSharedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.sharedannotation.id").exists());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
    public void get_comment_for_annotation() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        SharedAnnotation comment = builder.given_a_shared_annotation(algoAnnotation);

        restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{annotation}/comment/{id}.json", algoAnnotation.getId(), comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment.getId()));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
    public void list_comment_for_annotation() throws Exception {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        SharedAnnotation comment = builder.given_a_shared_annotation(algoAnnotation);

        restAlgoAnnotationControllerMockMvc.perform(get("/api/algoannotation/{annotation}/comment.json", algoAnnotation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(equalTo(1)));
    }



}
