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
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
public class CompanionFileResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restCompanionFileControllerMockMvc;

    @Autowired
    private PropertyRepository propertyRepository;


    @Test
    @Transactional
    public void list_companion_file_by_abstract_image() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());

        restCompanionFileControllerMockMvc.perform(get("/api/abstractimage/{id}/companionfile.json", companionFile.getImage().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.collection[?(@.id=="+companionFile.getId()+")]").exists());

    }

    @Test
    @Transactional
    public void list_companion_file_by_uploaded_file() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());

        restCompanionFileControllerMockMvc.perform(get("/api/uploadedfile/{id}/companionfile.json", companionFile.getUploadedFile().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.collection[?(@.id=="+companionFile.getId()+")]").exists());

    }

    @Test
    @Transactional
    public void get_an_companion_file() throws Exception {
        CompanionFile image = builder.given_a_companion_file(builder.given_an_abstract_image());

        restCompanionFileControllerMockMvc.perform(get("/api/companionfile/{id}.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.CompanionFile"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.originalFilename").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath())
                .andExpect(jsonPath("$.type").hasJsonPath());
    }


    @Test
    @Transactional
    public void get_an_companion_file_not_exist() throws Exception {
        restCompanionFileControllerMockMvc.perform(get("/api/companionfile/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    @Transactional
    public void get_an_companion_file_from_uploaded_file() throws Exception {
        CompanionFile image =
                builder.given_a_companion_file(builder.given_an_abstract_image());

        restCompanionFileControllerMockMvc.perform(get("/api/uploadedfile/{id}/companionfile.json", image.getUploadedFile().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists());
    }


    @Test
    @Transactional
    public void add_valid_companion_file() throws Exception {
        CompanionFile companionFile = builder.given_a_not_persisted_companion_file(builder.given_an_abstract_image());
        restCompanionFileControllerMockMvc.perform(post("/api/companionfile.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companionFile.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.companionfileID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.companionfile.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_companion_file() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());
        JsonObject jsonObject = companionFile.toJsonObject();
        jsonObject.put("filename", "toto");
        restCompanionFileControllerMockMvc.perform(put("/api/companionfile/{id}.json", companionFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.companionfileID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditCompanionFileCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.companionfile.id").exists())
                .andExpect(jsonPath("$.companionfile.filename").value("toto"));


    }


    @Test
    @Transactional
    public void delete_companion_file() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());
        restCompanionFileControllerMockMvc.perform(delete("/api/companionfile/{id}.json", companionFile.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.companionfileID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteCompanionFileCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.companionfile.id").exists());


    }

    @Test
    @Transactional
    public void get_companion_file_uploader() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());

        restCompanionFileControllerMockMvc.perform(get("/api/companionfile/{id}/user.json", companionFile.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(builder.given_superadmin().getId()));
    }

    @Test
    @Transactional
    public void get_companion_file_uploader_with_unexisting_companion_file() throws Exception {
        restCompanionFileControllerMockMvc.perform(get("/api/companionfile/{id}/user.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound());
    }



    @Test
    public void download_companion_file() throws Exception {
        CompanionFile companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());
        companionFile.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        companionFile.getUploadedFile().getImageServer().setBasePath("/data/images");
        companionFile.setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        companionFile.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        companionFile.getUploadedFile().setContentType("MRXS");

        MvcResult mvcResult = restCompanionFileControllerMockMvc.perform(get("/api/companionfile/{id}/download", companionFile.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost:8888/file/" + URLEncoder.encode("1636379100999/CMU-2/CMU-2.mrxs", StandardCharsets.UTF_8).replace("%2F", "/") + "/export");


    }


}
