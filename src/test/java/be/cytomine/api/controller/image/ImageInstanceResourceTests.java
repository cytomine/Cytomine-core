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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageInstanceResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restImageInstanceControllerMockMvc;

    @Autowired
    private PropertyRepository propertyRepository;
    

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

    private ImageInstance given_test_image_instance() {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");
        ImageInstance imageInstance = builder.given_an_image_instance(image, builder.given_a_project());
        imageInstance.setInstanceFilename("CMU-2");
        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        SliceInstance sliceInstance = builder.given_a_slice_instance(imageInstance, slice);
        return imageInstance;
    }

    @Test
    @Transactional
    public void get_an_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.ImageInstance"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.preview").value("http://localhost:8080/api/imageinstance/"+image.getId()+"/thumb.png?maxSize=1024"))
                .andExpect(jsonPath("$.thumb").value("http://localhost:8080/api/imageinstance/"+image.getId()+"/thumb.png?maxSize=512"))
                .andExpect(jsonPath("$.macroURL").value("http://localhost:8080/api/imageinstance/"+image.getId()+"/associated/macro.png?maxWidth=512"))
                .andExpect(jsonPath("$.reviewStop").hasJsonPath())
                .andExpect(jsonPath("$.baseImage").value(image.getBaseImage().getId()))
                .andExpect(jsonPath("$.project").value(image.getProject().getId()))

                .andExpect(jsonPath("$.path").value("/data/images/"+builder.given_superadmin().getId()+"/1636379100999/CMU-2/CMU-2.mrxs"))
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.instanceFilename").value("CMU-2"))
                .andExpect(jsonPath("$.originalFilename").exists())
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240))
                .andExpect(jsonPath("$.physicalSizeY").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeX").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeZ").hasJsonPath())
                .andExpect(jsonPath("$.fps").hasJsonPath())
                .andExpect(jsonPath("$.zoom").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath());
    }

    @Test
    @Transactional
    @WithMockUser(username = "get_blind_image_instance")
    public void get_blind_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();

        User user = builder.given_a_user("get_blind_image_instance");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        image.getProject().setBlindMode(true);

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.instanceFilename").doesNotExist())
                .andExpect(jsonPath("$.originalFilename").doesNotExist())
                .andExpect(jsonPath("$.blindedName").hasJsonPath())
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240));
    }


    @Test
    @Transactional
    public void get_an_image_instance_not_exist() throws Exception {
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void get_slice_instance_reference() throws Exception {
        ImageInstance image = given_test_image_instance();
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/sliceinstance/reference.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    @Transactional
    public void get_image_instance_bounds() throws Exception {
        ImageInstance image = given_test_image_instance();
        image.setReviewStart(new Date(1636702044534L));
        image.setMagnification(123);
        image.setPhysicalSizeX(null);
        image.setPhysicalSizeY(0.5);
        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/bounds/imageinstance.json", image.getProject().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStart.min").value(1636702044534L))
                .andExpect(jsonPath("$.reviewStart.max").value(1636702044534L))
                .andExpect(jsonPath("$.reviewStop.min").isEmpty())
                .andExpect(jsonPath("$.reviewStop.max").isEmpty())
                .andExpect(jsonPath("$.magnification.min").value(123))
                .andExpect(jsonPath("$.magnification.max").value(123))
                .andExpect(jsonPath("$.magnification.list", contains(123)))
                .andExpect(jsonPath("$.physicalSizeY.min").value(0.5))
                .andExpect(jsonPath("$.physicalSizeX.min").isEmpty())
                .andExpect(jsonPath("$.format.list", contains(image.getBaseImage().getContentType())));

    }

    @Autowired
    SecUserRepository secUserRepository;

    @WithMockUser(username = "list_image_instance_by_user")
    @Test
    @Transactional
    public void list_image_instance_by_user() throws Exception {
        ImageInstance image = builder.given_an_image_instance();
        image.getBaseImage().setWidth(500);
        ImageInstance imageFromOtherProjectNotAccessibleForUser = builder.given_an_image_instance();
        User user = builder.given_a_user("list_image_instance_by_user");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+imageFromOtherProjectNotAccessibleForUser.getId()+")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()).param("width[lte]", "500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance.json", user.getId()).param("width[gte]", "501"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").doesNotExist());
    }


    @WithMockUser(username = "list_image_instance_light_by_user")
    @Test
    @Transactional
    public void list_image_instance_light_by_user() throws Exception {
        ImageInstance image = builder.given_an_image_instance();
        image.getBaseImage().setWidth(500);
        ImageInstance imageFromOtherProjectNotAccessibleForUser = builder.given_an_image_instance();
        User user = builder.given_a_user("list_image_instance_light_by_user");
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor


        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance/light.json", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+imageFromOtherProjectNotAccessibleForUser.getId()+")]").doesNotExist());

    }


    @Test
    @Transactional
    public void list_image_instance_by_projects() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance imageInProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance imageInAnotherProject = builder.given_an_image_instance(builder.given_an_abstract_image(), anotherProject);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("light", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());


    }


    @Test
    @Transactional
    public void list_image_instance_light() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance imageInProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance imageInAnotherProject = builder.given_an_image_instance(builder.given_an_abstract_image(), anotherProject);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("light", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")].instanceFilename").value(imageInProject1.getBlindInstanceFilename()))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());
    }


    @Test
    @Transactional
    @WithMockUser("list_image_instance_by_projects_blind_filenames")
    public void list_image_instance_by_projects_blind_filenames() throws Exception {
        User user = builder.given_a_user("list_image_instance_by_projects_blind_filenames");
        ImageInstance image = given_test_image_instance();

        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor

        image.getProject().setBlindMode(true);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", image.getProject().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[0].filename").isEmpty());
    }


    @Test
    @Transactional
    public void list_image_instance_by_projects_tree() throws Exception {
        Project project1 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        ImageInstance image1InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
        ImageInstance image2InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                        .param("tree", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children", hasSize(2)));

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
                    .param("tree", "true").param("max", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children", hasSize(1)));
    }


    @Test
    @Transactional
    public void list_image_instance_by_project_with_annotation_filter() throws Exception {
        Project project = builder.given_a_project();
        // we add width filter to get only the image set defined in this test
        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image1.setCountImageAnnotations(2L);
        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image2.setCountImageAnnotations(4L);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "5")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "3")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").doesNotExist());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "4")
                        .param("numberOfAnnotations[gte]", "2")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
                        .param("offset", "0")
                        .param("max", "0")
                        .param("sort", "created")
                        .param("order", "desc")
                        .param("numberOfAnnotations[lte]", "4")
                        .param("numberOfAnnotations[gte]", "3")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());
    }


    @Test
    @Transactional
    public void list_image_instance_by_project_with_pagination() throws Exception {
        Project project = builder.given_a_project();
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image1.getBaseImage().setWidth(width);
        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image2.getBaseImage().setWidth(width);
        ImageInstance image3 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
        image3.getBaseImage().setWidth(width);

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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

        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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


        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
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
    public void get_next_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageInstance1.getId().intValue()));

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    public void get_previous_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageInstance2.getId().intValue()));

        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }



    @Test
    @Transactional
    public void add_valid_image_instance() throws Exception {
        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance();
        restImageInstanceControllerMockMvc.perform(post("/api/imageinstance.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imageInstance.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_image_instance() throws Exception {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance();
        JsonObject jsonObject = imageInstance.toJsonObject();
        jsonObject.put("project", project.getId());
        restImageInstanceControllerMockMvc.perform(put("/api/imageinstance/{id}.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists())
                .andExpect(jsonPath("$.imageinstance.project").value(project.getId()));


    }


    @Test
    @Transactional
    public void delete_image_instance() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        restImageInstanceControllerMockMvc.perform(delete("/api/imageinstance/{id}.json", imageInstance.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.imageinstance.id").exists());


    }


    @Test
    @Transactional
    public void get_image_instance_thumb() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo("/image/" + image.getBaseImage().getPath() + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png?maxSize=512", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_image_instance_thumb_if_image_not_exist() throws Exception {
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void get_image_instance_preview() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + image.getBaseImage().getPath() + "/thumb?z_slices=0&timepoints=0&length=1024"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/preview.png?maxSize=1024", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @Transactional
    public void get_image_instance_associeted_label() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/1636379100999/CMU-2/CMU-2.mrxs/info/associated"))
                .willReturn(
                        aResponse().withBody("{\"items\": [{\"name\":\"macro\"},{\"name\":\"thumbnail\"},{\"name\":\"label\"}], \"size\": 0}")
                )
        );
        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection", containsInAnyOrder("label","macro","thumbnail")));
    }


    @Test
    @Transactional
    public void get_image_instance_associeted_label_macro() throws Exception {
        ImageInstance image = given_test_image_instance();
        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + image.getBaseImage().getPath() + "/associated/macro?length=512";

        System.out.println(url);
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated/{label}.png", image.getId(), "macro")
                        .param("maxSize", "512"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @Transactional
    public void get_image_instance_crop() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + image.getBaseImage().getPath() + "/annotation/crop";
        String body = "{\"length\":512,\"annotations\":{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\"},\"background_transparency\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        //{"length":512,"annotations":{"geometry":"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))"},"level":0,"background_transparency":0,"z_slices":0,"timepoints":0}
        //{"length":512,"annotations":{"geometry":"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))"},"background_transparency":0,"z_slices":0,"timepoints":0}

        stubFor(WireMock.post(urlEqualTo(url)).withRequestBody(WireMock.equalTo(
                                body
                        ))
                        .willReturn(
                                aResponse().withBody(mockResponse)
                        )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/crop.png?maxSize=512", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @Transactional
    public void get_image_instance_window() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + image.getBaseImage().getPath() + "/window";
        String body = "{\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(url)).withRequestBody(WireMock.equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/window-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);


        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/window_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/image/1636379100999/CMU-2/CMU-2.mrxs/window?region=%7B%22left%22%3A10%2C%22top%22%3A20%2C%22width%22%3A30%2C%22height%22%3A40%7D&level=0"))
                .andExpect(status().isOk());

    }


    @Test
    @Transactional
    public void get_image_instance_camera() throws Exception {
        ImageInstance image = given_test_image_instance();

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + image.getBaseImage().getPath() + "/window";
        String body = "{\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(WireMock.post(urlEqualTo(url)).withRequestBody(WireMock.equalTo(body))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/camera-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);


        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/camera_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/image/1636379100999/CMU-2/CMU-2.mrxs/window?region=%7B%22left%22%3A10%2C%22top%22%3A20%2C%22width%22%3A30%2C%22height%22%3A40%7D&level=0"))
                .andExpect(status().isOk());

    }



    @Test
    public void download_image_instance() throws Exception {
        ImageInstance image = given_test_image_instance();

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost:8888/file/1636379100999/CMU-2/CMU-2.mrxs/export");


    }


    @Test
    @WithMockUser("download_image_instance_cannot_download")
    public void download_image_instance_cannot_download() throws Exception {
        User user = builder.given_a_user("download_image_instance_cannot_download");

        ImageInstance image = given_test_image_instance();
        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE);
        image.getProject().setAreImagesDownloadable(true);

        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost:8888/file/1636379100999/CMU-2/CMU-2.mrxs/export");

        image.getProject().setAreImagesDownloadable(false);

        mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(403);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isNotEqualTo("http://localhost:8888/file/1636379100999/CMU-2/CMU-2.mrxs/export");


    }






















}
