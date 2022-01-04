package be.cytomine.api.controller.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class RestAnnotationIndexResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private MockMvc restAnnotationIndexControllerMockMvc;

    Project project;
    ImageInstance image;
    SliceInstance slice;
    User me;
    Term term;
    UserAnnotation a1;
    UserAnnotation a2;
    UserAnnotation a3;
    UserAnnotation a4;


    void createAnnotationSet() throws ParseException {
        project = builder.given_a_project();
        image = builder.given_an_image_instance(project);
        slice = builder.given_a_slice_instance(image,0,0,0);
        me = builder.given_superadmin();
        term =  builder.given_a_term(project.getOntology());

        a1 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);
        a2 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);
        a3 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);

        a4 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, null);
        em.flush();
    }
    @BeforeEach
    public void BeforeEach() throws ParseException {
        createAnnotationSet();
    }

    @Test
    @Transactional
    public void list_user_annotation_property_show() throws Exception {
        restAnnotationIndexControllerMockMvc.perform(get("/api/sliceinstance/{id}/annotationindex.json", slice.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].slice").value(slice.getId()))
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].countAnnotation").value(4))
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].countReviewedAnnotation").value(0))
                .andReturn();
    }





}
