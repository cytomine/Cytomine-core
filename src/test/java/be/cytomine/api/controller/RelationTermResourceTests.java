package be.cytomine.api.controller;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class RelationTermResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restRelationTermControllerMockMvc;

    @Test
    @Transactional
    public void list_by_term_position_1() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{i}/{id}", "1", relationTerm.getTerm1().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+relationTerm.getId()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_by_term_position_2() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{i}/{id}", "2", relationTerm.getTerm1().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }

    @Test
    @Transactional
    public void list_by_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(get("/api/relation/term/{id}",  relationTerm.getTerm1().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[?(@.id=='"+relationTerm.getId()+"')]").exists());
    }





    @Test
    @Transactional
    public void get_a_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();

        restRelationTermControllerMockMvc.perform(get("/api/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}", relationTerm.getRelation().getId(), relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationTerm.getId().intValue()))
                .andExpect(jsonPath("$.term1").value(relationTerm.getTerm1().getId().intValue()))
                .andExpect(jsonPath("$.term2").value(relationTerm.getTerm2().getId().intValue()))
                .andExpect(jsonPath("$.relation").value(relationTerm.getRelation().getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void get_a_parent_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();

        restRelationTermControllerMockMvc.perform(get("/api/relation/parent/term1/{idTerm1}/term2/{idTerm2}", relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationTerm.getId().intValue()))
                .andExpect(jsonPath("$.term1").value(relationTerm.getTerm1().getId().intValue()))
                .andExpect(jsonPath("$.term2").value(relationTerm.getTerm2().getId().intValue()))
                .andExpect(jsonPath("$.relation").value(relationTerm.getRelation().getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void add_valid_relation() throws Exception {
        Ontology ontology = builder.given_an_ontology();
        RelationTerm relationTerm = BasicInstanceBuilder.given_a_not_persisted_relation_term(builder.given_a_relation(), builder.given_a_term(ontology), builder.given_a_term(ontology));
        restRelationTermControllerMockMvc.perform(post("/api/relation/parent/term")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.relationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddRelationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.relationterm.id").exists())
                .andExpect(jsonPath("$.relationterm.term1").exists());

    }




    @Test
    @Transactional
    public void delete_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(delete("/api/relation/{idRelation}/term1/{idTerm1}/term2/{idTerm2}", relationTerm.getRelation().getId(), relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.relationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteRelationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.relationterm.id").exists());
    }

    @Test
    @Transactional
    public void delete_parent_relation_term() throws Exception {
        RelationTerm relationTerm = builder.given_a_relation_term();
        restRelationTermControllerMockMvc.perform(delete("/api/relation/parent/term1/{idTerm1}/term2/{idTerm2}", relationTerm.getTerm1().getId(), relationTerm.getTerm2().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relationTerm.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.relationtermID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteRelationTermCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.relationterm.id").exists());
    }

}
