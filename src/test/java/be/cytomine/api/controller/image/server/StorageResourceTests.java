package be.cytomine.api.controller.image.server;

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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class StorageResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restStorageControllerMockMvc;

    @Test
    @Transactional
    public void list_user_storages() throws Exception {
        Storage storage = builder.given_a_storage();
        Storage otherUserStorage = builder.given_a_storage(builder.given_a_user());
        restStorageControllerMockMvc.perform(get("/api/storage.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+storage.getName()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.name=='"+otherUserStorage.getName()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_storages_with_search_string() throws Exception {
        Storage storage = builder.given_a_storage();
        storage.setName("abracadabra");
        restStorageControllerMockMvc.perform(get("/api/storage.json").param("searchString", "acada"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))));

        restStorageControllerMockMvc.perform(get("/api/storage.json").param("searchString", "not"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0))));
    }


    @Test
    @Transactional
    public void list_all_storages() throws Exception {
        Storage storage = builder.given_a_storage();
        Storage otherUserStorage = builder.given_a_storage(builder.given_a_user());
        restStorageControllerMockMvc.perform(get("/api/storage.json").param("all", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+storage.getName()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.name=='"+otherUserStorage.getName()+"')]").exists());
    }

    @Test
    @Transactional
    public void get_a_storage() throws Exception {
        Storage storage = builder.given_a_storage();

        restStorageControllerMockMvc.perform(get("/api/storage/{id}.json", storage.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storage.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.server.Storage"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.name").value(storage.getName()))
                .andExpect(jsonPath("$.user").value(storage.getUser().getId().intValue()))
                .andExpect(jsonPath("$.basePath").doesNotExist()); //since multidim
    }

    @Test
    @Transactional
    public void add_valid_storage() throws Exception {
        Storage storage = BasicInstanceBuilder.given_a_not_persisted_storage(builder.given_superadmin());
        restStorageControllerMockMvc.perform(post("/api/storage.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storage.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.storageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddStorageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.storage.id").exists())
                .andExpect(jsonPath("$.storage.name").value(storage.getName()));

    }

    @Test
    @Transactional
    public void add_storage_refused_if_name_not_set() throws Exception {
        Storage storage = BasicInstanceBuilder.given_a_not_persisted_storage(builder.given_superadmin());
        storage.setName(null);
        restStorageControllerMockMvc.perform(post("/api/storage.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storage.toJSON()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Transactional
    public void edit_valid_storage() throws Exception {
        Storage storage = builder.given_a_storage();
        restStorageControllerMockMvc.perform(put("/api/storage/{id}.json", storage.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storage.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.storageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditStorageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.storage.id").exists())
                .andExpect(jsonPath("$.storage.name").value(storage.getName()));

    }

    @Test
    @Transactional
    public void delete_storage() throws Exception {
        Storage storage = builder.given_a_storage();
        restStorageControllerMockMvc.perform(delete("/api/storage/{id}.json", storage.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storage.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.storageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteStorageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.storage.id").exists())
                .andExpect(jsonPath("$.storage.name").value(storage.getName()));
    }

    @Test
    @Transactional
    public void fail_when_delete_storage_not_exists() throws Exception {
        restStorageControllerMockMvc.perform(delete("/api/storage/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }


    @Test
    @Transactional
    public void user_stats() throws Exception {
        Storage storage = builder.given_a_storage();

        restStorageControllerMockMvc.perform(get("/api/storage/{id}/usersstats.json", storage.getId())
                        .param("sortColumn", "created").param("sortDirection", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+storage.getUser().getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void storage_access() throws Exception {
        Storage storage = builder.given_a_storage();

        restStorageControllerMockMvc.perform(get("/api/storage/access.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+storage.getId()+"')]").exists());
    }
}
