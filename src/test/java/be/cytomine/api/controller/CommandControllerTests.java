package be.cytomine.api.controller;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class CommandControllerTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private MockMvc restCommandControllerMockMvc;

    @Autowired
    private CommandRepository commandRepository;

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void undo_redo() throws Exception {

        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName("undo_redo");
        restCommandControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

        ontology = ontologyRepository.findAll().stream().filter(x -> x.getName().equals("undo_redo")).findFirst().get();

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));

        restCommandControllerMockMvc.perform(get("/command/undo.json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isNotFound());

        restCommandControllerMockMvc.perform(get("/command/redo.json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void undo_redo_with_command_id() throws Exception {

        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        ontology.setName("undo_redo");
        restCommandControllerMockMvc.perform(post("/api/ontology.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ontology.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ontology.name").value(ontology.getName()));

        ontology = ontologyRepository.findAll().stream().filter(x -> x.getName().equals("undo_redo")).findFirst().get();

        Command command = commandRepository.findAll(Sort.by(Sort.Direction.DESC, "created")).get(0);

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));

        restCommandControllerMockMvc.perform(get("/command/{id}/undo.json", command.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isNotFound());

        restCommandControllerMockMvc.perform(get("/command/{id}/redo.json", command.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/ontology/{id}.json", ontology.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ontology.getId().intValue()));
    }
}
