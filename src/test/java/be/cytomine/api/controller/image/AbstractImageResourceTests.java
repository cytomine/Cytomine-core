package be.cytomine.api.controller.image;

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
import be.cytomine.api.ApiExceptionHandler;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.dto.WindowParameter;
import be.cytomine.service.image.ImagePropertiesService;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AbstractImageResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAbstractImageControllerMockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

//    @Autowired
//    private RestAbstractImageController restAbstractImageController;

    private static WireMockServer wireMockServer = new WireMockServer(8888);


//    @BeforeEach
//    public void initMock() {
//        if(restAbstractImageControllerMockMvc==null) {
//            restAbstractImageControllerMockMvc = MockMvcBuilders.standaloneSetup(restAbstractImageController)
//                    .setControllerAdvice(new ApiExceptionHandler())
//                    .build();
//        }
//    }



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
    public void list_abstract_image_by_width() throws Exception {
        AbstractImage img500Width = builder.given_an_abstract_image();
        img500Width.setWidth(500);
        img500Width = builder.persistAndReturn(img500Width);
        AbstractImage img501Width = builder.given_an_abstract_image();
        img501Width.setWidth(501);
        img501Width = builder.persistAndReturn(img501Width);


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]", "501"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[gte]", "500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]", String.valueOf(Integer.MAX_VALUE)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[gte]","501"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]","500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[equals]","500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[equals]","100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_abstract_image_by_projects() throws Exception {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        AbstractImage imageInProject1 = builder.given_an_abstract_image();
        AbstractImage imageInProject2 = builder.given_an_abstract_image();
        AbstractImage imageInProject1And2 = builder.given_an_abstract_image();

        builder.given_an_image_instance(imageInProject1, project1);
        builder.given_an_image_instance(imageInProject2, project2);
        builder.given_an_image_instance(imageInProject1And2, project1);
        builder.given_an_image_instance(imageInProject1And2, project2);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(project1.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")].inProject").value(true))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")].inProject").value(false))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")].inProject").value(true));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(project2.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")].inProject").value(false))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")].inProject").value(true))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")].inProject").value(true));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(anotherProject.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")].inProject").value(false))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")].inProject").value(false))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")].inProject").value(false));

    }

    @Test
    @Transactional
    public void list_abstract_image_with_ordering() throws Exception {
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        AbstractImage image1 = builder.given_an_abstract_image();
        image1.setWidth(width);
        AbstractImage image2 = builder.given_an_abstract_image();
        image2.setWidth(width);
        AbstractImage image3 = builder.given_an_abstract_image();
        image3.setWidth(width);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")// default sorting must be created desc
                        .param("width[equals]", String.valueOf(width)))

                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "id")
                        .param("order", "asc")
                        .param("width[equals]", String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(image1.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image3.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "id")
                        .param("order", "desc")
                        .param("width[equals]", String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));

        image1.setWidth(100);
        builder.persistAndReturn(image1);
        image2.setWidth(200);
        builder.persistAndReturn(image1);
        image3.setWidth(150);
        builder.persistAndReturn(image1);

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "width")
                        .param("order", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> map = JsonObject.toMap(mvcResult.getResponse().getContentAsString());
        List<Map<String, Object>> objectList = (List<Map<String, Object>>)map.get("collection");
        List<Long> ids = objectList.stream().map(x -> Long.valueOf((int)x.get("id"))).collect(Collectors.toList());

        assertThat(ids.indexOf(image2.getId())).isLessThan(ids.indexOf(image3.getId()));
        assertThat(ids.indexOf(image3.getId())).isLessThan(ids.indexOf(image1.getId()));


    }



    @Test
    @Transactional
    public void list_abstract_image_with_pagination() throws Exception {
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        AbstractImage image1 = builder.given_an_abstract_image();
        image1.setWidth(width);
        AbstractImage image2 = builder.given_an_abstract_image();
        image2.setWidth(width);
        AbstractImage image3 = builder.given_an_abstract_image();
        image3.setWidth(width);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "0")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "1")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "1")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "500")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "500")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
                .andExpect(jsonPath("$.offset").value(500))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }


    @Test
    @Transactional
    public void get_an_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        String serverUrl = applicationProperties.getServerURL();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.AbstractImage"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.contentType").value("MRXS"))
                .andExpect(jsonPath("$.preview").value(serverUrl + "/api/abstractimage/"+image.getId()+"/thumb.png?maxSize=1024"))
                .andExpect(jsonPath("$.thumb").value(serverUrl + "/api/abstractimage/"+image.getId()+"/thumb.png?maxSize=512"))
                .andExpect(jsonPath("$.macroURL").value(serverUrl + "/api/abstractimage/"+image.getId()+"/associated/macro.png?maxWidth=512"))
                .andExpect(jsonPath("$.path").value("1636379100999/CMU-2/CMU-2.mrxs"))
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240))
                .andExpect(jsonPath("$.physicalSizeY").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeX").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeZ").hasJsonPath())
                .andExpect(jsonPath("$.fps").hasJsonPath())
                .andExpect(jsonPath("$.zoom").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath())
                .andExpect(jsonPath("$.dimensions").hasJsonPath());
    }


    @Test
    @Transactional
    public void get_an_abstract_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    @Transactional
    public void get_an_abstract_image_from_uploaded_file() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/uploadedfile/{id}/abstractimage.json", image.getUploadedFile().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()));
    }


    @Test
    @Transactional
    public void add_valid_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(abstractImage.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        JsonObject jsonObject = abstractImage.toJsonObject();
        jsonObject.put("width", 999);
        restAbstractImageControllerMockMvc.perform(put("/api/abstractimage/{id}.json", abstractImage.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditAbstractImageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists())
                .andExpect(jsonPath("$.abstractimage.width").value(999));


    }


    @Test
    @Transactional
    public void delete_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        restAbstractImageControllerMockMvc.perform(delete("/api/abstractimage/{id}.json", abstractImage.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAbstractImageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists());


    }


    @Test
    @Transactional
    public void list_unused_abstract_image() throws Exception {
        AbstractImage imageWithImageInstance = builder.given_an_image_instance().getBaseImage();
        AbstractImage imageWithoutImageInstance = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/unused.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageWithImageInstance.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageWithoutImageInstance.getId() + ")]").exists());

    }

    @Test
    @Transactional
    public void get_abstract_image_uploader() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/user.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(builder.given_superadmin().getId()));
    }

    @Test
    @Transactional
    public void get_abstract_image_uploader_for_abstract_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/user.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void get_abstract_image_thumb() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0,0,0);
        slice.setUploadedFile(image.getUploadedFile());
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/thumb.png?maxSize=512", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_abstract_image_thumb_if_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/thumb.png", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void get_abstract_image_preview() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0,0,0);
        slice.setUploadedFile(image.getUploadedFile());
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/thumb?z_slices=0&timepoints=0&length=1024"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/preview.png?maxSize=1024", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @Transactional
    public void get_abstract_image_associeted_label() throws Exception {
        AbstractImage image = given_test_abstract_image();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/info/associated"))
                .willReturn(
                        aResponse().withBody("{\"items\": [{\"name\":\"macro\"},{\"name\":\"thumbnail\"},{\"name\":\"label\"}], \"size\": 0}")
                )
        );
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/associated.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection", containsInAnyOrder("label","macro","thumbnail")));
    }


    @Test
    @Transactional
    public void get_abstract_image_associeted_label_macro() throws Exception {
        AbstractImage image = given_test_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/associated/macro?length=512";

        System.out.println(url);
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/associated/{label}.png", image.getId(), "macro")
                        .param("maxSize", "512"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @Transactional
    public void get_abstract_image_crop() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);


        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/annotation/crop";
        String body = "{\"annotations\":{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"},\"level\":0,\"background_transparency\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);

        stubFor(post(urlEqualTo(url)).withRequestBody(equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/crop.png", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_abstract_image_window() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        //String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";

        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(url)).withRequestBody(equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/window-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/window_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/image/"+ URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/")+"/window?region=%7B%22left%22%3A10%2C%22top%22%3A20%2C%22width%22%3A30%2C%22height%22%3A40%7D&level=0"))
                .andExpect(status().isOk());

    }


    @Test
    @Transactional
    public void get_abstract_image_camera() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        String url = "/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/window";
        String body = "{\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(url)).withRequestBody(equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/camera-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/camera_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/image/"+URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/window?region=%7B%22left%22%3A10%2C%22top%22%3A20%2C%22width%22%3A30%2C%22height%22%3A40%7D&level=0"))
                .andExpect(status().isOk());

    }

    private AbstractImage given_test_abstract_image() {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        return image;
    }

    @Test
    public void download_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/download", image.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost:8888/image/"+URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/")+"/export");


    }


    @Test
    public void clear_properties_from_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();
        builder.given_a_property(image, "cytomine.width", "value1");
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isPresent();
        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/clear.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isEmpty();

    }


    @Test
    public void extract_populated_properties_to_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");


        image.setWidth(1);
        image.setPhysicalSizeX(2d);
        image.setColorspace("empty");

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/metadata"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"size":14,"items":
                                [{"key":"ImageWidth","value":30720,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ImageLength","value":25600,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"BitsPerSample","value":"(8, 8, 8)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"Compression","value":"JPEG","type":"STRING","namespace":"TIFF"},
                                {"key":"PhotometricInterpretation","value":"YCBCR","type":"STRING","namespace":"TIFF"},
                                {"key":"Orientation","value":"TOPLEFT","type":"STRING","namespace":"TIFF"},
                                {"key":"SamplesPerPixel","value":3,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"XResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"YResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"PlanarConfiguration","value":"CONTIG","type":"STRING","namespace":"TIFF"},
                                {"key":"ResolutionUnit","value":"CENTIMETER","type":"STRING","namespace":"TIFF"},
                                {"key":"TileWidth","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"TileLength","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ReferenceBlackWhite","value":"(0, 1, 255, 1, 128, 1, 255, 1, 128, 1, 255, 1)","type":"UNKNOWN","namespace":"TIFF"}]
                                }
                                """
                        )
                )
        );

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/populate.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "TIFF.ImageWidth")).isPresent();
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "TIFF.ImageWidth").get().getValue()).isEqualTo("30720");


        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/info"))
                .willReturn(
                        aResponse().withBody(
                                """
                                  {"image":
                                      {
                                          "original_format":"PYRTIFF",
                                          "width":30720,
                                          "height":25600,
                                          "depth":1,
                                          "duration":1,
                                          "physical_size_x":100000.00617,
                                          "physical_size_y":100000.00617,
                                          "physical_size_z":null,
                                          "frame_rate":null,
                                          "n_channels":3,
                                          "n_concrete_channels":1,
                                          "n_samples":3,
                                          "n_planes":1,
                                          "are_rgb_planes":true,
                                          "n_distinct_channels":1,
                                          "acquired_at":null,
                                          "description":"",
                                          "pixel_type":"uint8",
                                          "significant_bits":8,"bits":8},
                                          "instrument":
                                              {"microscope":{"model":null},
                                               "objective":{"nominal_magnification":null,"calibrated_magnification":null}},
                                               "associated":[],
                                               "channels":[
                                                  {"index":0,"suggested_name":"R","emission_wavelength":null,"excitation_wavelength":null,"color":"#f00"},
                                                  {"index":1,"suggested_name":"G","emission_wavelength":null,"excitation_wavelength":null,"color":"#0f0"},
                                                  {"index":2,"suggested_name":"B","emission_wavelength":null,"excitation_wavelength":null,"color":"#00f"}
                                               ],
                                               "representations":[
                                                  {"role":"UPLOAD",
                                                   "file":
                                                      {"file_type":"SINGLE",
                                                      "filepath":"/data/images/upload1644425985928451/LUNG1_pyr.tif",
                                                      "stem":"LUNG1_pyr",
                                                      "extension":".tif",
                                                      "created_at":"2022-05-05T22:16:23.318839",
                                                      "size":126616954,"is_symbolic":false,"role":"UPLOAD"}
                                                      },
                                                      {"role":"ORIGINAL",
                                                      "file":
                                                          {"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/original.PYRTIFF","stem":"original","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"ORIGINAL"}
                                                          },
                                                          {"role":"SPATIAL","file":{"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/visualisation.PYRTIFF","stem":"visualisation","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"SPATIAL"},"pyramid":{"n_tiers":8,"tiers":[{"zoom":7,"level":0,"width":30720,"height":25600,"tile_width":256,"tile_height":256,"downsampling_factor":1.0,"n_tiles":12000,"n_tx":120,"n_ty":100},{"zoom":6,"level":1,"width":15360,"height":12800,"tile_width":256,"tile_height":256,"downsampling_factor":2.0,"n_tiles":3000,"n_tx":60,"n_ty":50},{"zoom":5,"level":2,"width":7680,"height":6400,"tile_width":256,"tile_height":256,"downsampling_factor":4.0,"n_tiles":750,"n_tx":30,"n_ty":25},{"zoom":4,"level":3,"width":3840,"height":3200,"tile_width":256,"tile_height":256,"downsampling_factor":8.0,"n_tiles":195,"n_tx":15,"n_ty":13},{"zoom":3,"level":4,"width":1920,"height":1600,"tile_width":256,"tile_height":256,"downsampling_factor":16.0,"n_tiles":56,"n_tx":8,"n_ty":7},{"zoom":2,"level":5,"width":960,"height":800,"tile_width":256,"tile_height":256,"downsampling_factor":32.0,"n_tiles":16,"n_tx":4,"n_ty":4},{"zoom":1,"level":6,"width":480,"height":400,"tile_width":256,"tile_height":256,"downsampling_factor":64.0,"n_tiles":4,"n_tx":2,"n_ty":2},{"zoom":0,"level":7,"width":240,"height":200,"tile_width":256,"tile_height":256,"downsampling_factor":128.0,"n_tiles":1,"n_tx":1,"n_ty":1}]}}]}
      
                                  """
                        )
                )
        );

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/extract.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(image.getWidth()).isEqualTo(30720);
        assertThat(image.getHeight()).isEqualTo(25600);
        assertThat(image.getColorspace()).isEqualTo("empty");
    }


    @Test
    public void regenarate_properties_to_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");


        image.setWidth(1);
        image.setPhysicalSizeX(2d);
        image.setColorspace("empty");

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/metadata"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"size":14,"items":
                                [{"key":"ImageWidth","value":30720,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ImageLength","value":25600,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"BitsPerSample","value":"(8, 8, 8)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"Compression","value":"JPEG","type":"STRING","namespace":"TIFF"},
                                {"key":"PhotometricInterpretation","value":"YCBCR","type":"STRING","namespace":"TIFF"},
                                {"key":"Orientation","value":"TOPLEFT","type":"STRING","namespace":"TIFF"},
                                {"key":"SamplesPerPixel","value":3,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"XResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"YResolution","value":"(429496703, 4294967295)","type":"UNKNOWN","namespace":"TIFF"},
                                {"key":"PlanarConfiguration","value":"CONTIG","type":"STRING","namespace":"TIFF"},
                                {"key":"ResolutionUnit","value":"CENTIMETER","type":"STRING","namespace":"TIFF"},
                                {"key":"TileWidth","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"TileLength","value":256,"type":"INTEGER","namespace":"TIFF"},
                                {"key":"ReferenceBlackWhite","value":"(0, 1, 255, 1, 128, 1, 255, 1, 128, 1, 255, 1)","type":"UNKNOWN","namespace":"TIFF"}]
                                }
                                """
                        )
                )
        );

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/populate.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "TIFF.ImageWidth")).isPresent();
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "TIFF.ImageWidth").get().getValue()).isEqualTo("30720");


        stubFor(get(urlEqualTo("/image/" + URLEncoder.encode(image.getPath(), StandardCharsets.UTF_8).replace("%2F", "/") + "/info"))
                .willReturn(
                        aResponse().withBody(
                                """
                                  {"image":
                                      {
                                          "original_format":"PYRTIFF",
                                          "width":30720,
                                          "height":25600,
                                          "depth":1,
                                          "duration":1,
                                          "physical_size_x":100000.00617,
                                          "physical_size_y":100000.00617,
                                          "physical_size_z":null,
                                          "frame_rate":null,
                                          "n_channels":3,
                                          "n_concrete_channels":1,
                                          "n_samples":3,
                                          "n_planes":1,
                                          "are_rgb_planes":true,
                                          "n_distinct_channels":1,
                                          "acquired_at":null,
                                          "description":"",
                                          "pixel_type":"uint8",
                                          "significant_bits":8,"bits":8},
                                          "instrument":
                                              {"microscope":{"model":null},
                                               "objective":{"nominal_magnification":null,"calibrated_magnification":null}},
                                               "associated":[],
                                               "channels":[
                                                  {"index":0,"suggested_name":"R","emission_wavelength":null,"excitation_wavelength":null,"color":"#f00"},
                                                  {"index":1,"suggested_name":"G","emission_wavelength":null,"excitation_wavelength":null,"color":"#0f0"},
                                                  {"index":2,"suggested_name":"B","emission_wavelength":null,"excitation_wavelength":null,"color":"#00f"}
                                               ],
                                               "representations":[
                                                  {"role":"UPLOAD",
                                                   "file":
                                                      {"file_type":"SINGLE",
                                                      "filepath":"/data/images/upload1644425985928451/LUNG1_pyr.tif",
                                                      "stem":"LUNG1_pyr",
                                                      "extension":".tif",
                                                      "created_at":"2022-05-05T22:16:23.318839",
                                                      "size":126616954,"is_symbolic":false,"role":"UPLOAD"}
                                                      },
                                                      {"role":"ORIGINAL",
                                                      "file":
                                                          {"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/original.PYRTIFF","stem":"original","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"ORIGINAL"}
                                                          },
                                                          {"role":"SPATIAL","file":{"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/visualisation.PYRTIFF","stem":"visualisation","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"SPATIAL"},"pyramid":{"n_tiers":8,"tiers":[{"zoom":7,"level":0,"width":30720,"height":25600,"tile_width":256,"tile_height":256,"downsampling_factor":1.0,"n_tiles":12000,"n_tx":120,"n_ty":100},{"zoom":6,"level":1,"width":15360,"height":12800,"tile_width":256,"tile_height":256,"downsampling_factor":2.0,"n_tiles":3000,"n_tx":60,"n_ty":50},{"zoom":5,"level":2,"width":7680,"height":6400,"tile_width":256,"tile_height":256,"downsampling_factor":4.0,"n_tiles":750,"n_tx":30,"n_ty":25},{"zoom":4,"level":3,"width":3840,"height":3200,"tile_width":256,"tile_height":256,"downsampling_factor":8.0,"n_tiles":195,"n_tx":15,"n_ty":13},{"zoom":3,"level":4,"width":1920,"height":1600,"tile_width":256,"tile_height":256,"downsampling_factor":16.0,"n_tiles":56,"n_tx":8,"n_ty":7},{"zoom":2,"level":5,"width":960,"height":800,"tile_width":256,"tile_height":256,"downsampling_factor":32.0,"n_tiles":16,"n_tx":4,"n_ty":4},{"zoom":1,"level":6,"width":480,"height":400,"tile_width":256,"tile_height":256,"downsampling_factor":64.0,"n_tiles":4,"n_tx":2,"n_ty":2},{"zoom":0,"level":7,"width":240,"height":200,"tile_width":256,"tile_height":256,"downsampling_factor":128.0,"n_tiles":1,"n_tx":1,"n_ty":1}]}}]}
      
                                  """
                        )
                )
        );

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/regenerate.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(image.getWidth()).isEqualTo(30720);
        assertThat(image.getHeight()).isEqualTo(25600);
        assertThat(image.getColorspace()).isEqualTo("empty");
    }

}
