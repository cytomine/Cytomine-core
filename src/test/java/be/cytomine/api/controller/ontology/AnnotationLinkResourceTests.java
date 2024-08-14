package be.cytomine.api.controller.ontology;

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
import be.cytomine.domain.ontology.AnnotationLink;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AnnotationLinkResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAnnotationLinkControllerMockMvc;

    @Test
    @Transactional
    public void add_valid_annotation_link() throws Exception {
        AnnotationLink annotationLink = builder.given_a_not_persisted_annotation_link();
        restAnnotationLinkControllerMockMvc.perform(post("/api/annotationlink.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationLink.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationlinkID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddAnnotationLinkCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationlink.id").exists())
                .andExpect(jsonPath("$.annotationlink.group").exists());
    }

    @Test
    @Transactional
    public void list_annotation_link_by_annotation_group() throws Exception {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        restAnnotationLinkControllerMockMvc.perform(get("/api/annotationgroup/{id}/annotationlink.json", annotationLink.getGroup().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + annotationLink.getId() + ")]").exists());
    }

    @Test
    @Transactional
    public void list_annotation_link_by_annotation() throws Exception {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        restAnnotationLinkControllerMockMvc.perform(get("/api/annotation/{id}/annotationlink.json", annotationLink.getAnnotationIdent()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + annotationLink.getId() + ")]").exists());
    }

    @Test
    @Transactional
    public void delete_annotation_link() throws Exception {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        restAnnotationLinkControllerMockMvc.perform(delete("/api/annotationgroup/{annotationGroup}/annotation/{annotation}.json", annotationLink.getGroup().getId(), annotationLink.getAnnotationIdent()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.annotationlinkID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAnnotationLinkCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotationlink.id").exists());
    }
}
