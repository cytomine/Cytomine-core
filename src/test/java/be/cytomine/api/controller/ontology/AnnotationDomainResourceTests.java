package be.cytomine.api.controller.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
public class AnnotationDomainResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private MockMvc restAnnotationDomainControllerMockMvc;

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
    }
    @BeforeEach
    public void BeforeEach() throws ParseException {
        createAnnotationSet();
    }



    private static void checkForProperties(String response, List<String> expectedProperties, List<String> unexpectedProperties) {
        Map<String, Object> firstResult = ((List<Map<String, Object>>)JsonObject.toMap(response).get("collection")).get(0);
        for(String property : expectedProperties) {
            assertThat(firstResult).containsKey(property);
        }

        for(String property : unexpectedProperties) {
            assertThat(firstResult).doesNotContainKey(property);
        }
    }


    @Test
    @Transactional
    public void list_user_annotation_property_show() throws Exception {
        MvcResult result;
        result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andReturn();

        checkForProperties(result.getResponse().getContentAsString(),List.of("id","term","created","project","image"), List.of());

        result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("showBasic", "true")
                        .param("showWKT", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andReturn();

        checkForProperties(result.getResponse().getContentAsString(),List.of("id","location"), List.of("term", "created", "area", "project"));


        result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("showDefault", "true")
                        .param("hideMeta", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andReturn();

        checkForProperties(result.getResponse().getContentAsString(),List.of("id","term"), List.of("location", "created", "project"));

        result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("showBasic", "true")
                        .param("showImage", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andReturn();

        checkForProperties(result.getResponse().getContentAsString(),List.of("id","originalFilename"), List.of("term", "location"));

        result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("showWKT", "true")
                        .param("hideWKT", "true")
                        .param("hideBasic", "true")
                        .param("hideMeta", "true")
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();
    }


    @Test
    @Transactional
    public void list_user_annotation_with_parameters_image() throws Exception {
        a4.setImage(builder.given_an_image_instance(project));
        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();
    }



    @Test
    @Transactional
    public void list_annotation_search_with_no_terms() throws Exception {

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("project", this.project.getId().toString())
                        .param("noTerm", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").exists())
                .andReturn();
    }

    @Test
    @Transactional
    public void list_annotation_search_by_max_distance() throws Exception {
        a1.setLocation(new WKTReader().read("POINT(0 0)")); //base point
        a2.setLocation(new WKTReader().read("POINT(-10 0)")); //should be < 11
        a3.setLocation(new WKTReader().read("POLYGON((10 0,15 10,15 15,10 15,10 0))")); //should be < 11
        a4.setLocation(new WKTReader().read( "POINT(20 20)")); //should be > 11

        List<Tuple> tuples = userAnnotationRepository.listAnnotationWithDistance(project.getId(), a1.getLocation().toText());

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("project", this.project.getId().toString())
                        .param("baseAnnotation", a1.getId().toString())
                        .param("maxDistanceBaseAnnotation", "11")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("project", this.project.getId().toString())
                        .param("baseAnnotation", a1.getLocation().toText())
                        .param("maxDistanceBaseAnnotation", "11")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();


    }

    @Test
    @Transactional
    public void list_annotation_search_by_image_and_user() throws Exception {

        a1.setImage(builder.given_an_image_instance(project));
        a2.setUser(builder.given_a_user());

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("user", this.me.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").exists())
                .andReturn();


    }

    @Test
    @Transactional
    public void list_annotation_search_by_image_and_user_and_bbox() throws Exception {

        a1.setLocation(new WKTReader().read("POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"));
        a2.setLocation(new WKTReader().read("POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"));
        a3.setLocation(new WKTReader().read("POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"));
        a4.setLocation(new WKTReader().read( "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"));


        // intersect a,b and c
        String polygonIncluding_A1_A2_A3 = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))";

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.a1.getImage().getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("bbox", polygonIncluding_A1_A2_A3)
                        .param("notReviewedOnly", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();


        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.a1.getImage().getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("bbox", polygonIncluding_A1_A2_A3)
                        .param("notReviewedOnly", "true")
                        .param("kmeansValue", "1")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.a1.getImage().getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("bbox", polygonIncluding_A1_A2_A3)
                        .param("notReviewedOnly", "true")
                        .param("kmeansValue", "2")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.a1.getImage().getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("bbox", polygonIncluding_A1_A2_A3)
                        .param("notReviewedOnly", "true")
                        .param("kmeansValue", "3")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();
    }


    @Test
    @Transactional
    public void list_annotation_search_by_image_and_user_and_term() throws Exception {

        a1.setImage(builder.given_an_image_instance(project));
        a2.setUser(builder.given_a_user());

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("image", this.image.getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("term", this.term.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").doesNotExist()) //  wrong image
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist()) //wrong user
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist()) //no term
                .andReturn();


    }


    @Test
    @Transactional
    public void list_annotation_search_include_filter_user_annotation() throws Exception {

        a1.setLocation(new WKTReader().read("POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"));
        a2.setLocation(new WKTReader().read("POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"));
        a3.setLocation(new WKTReader().read("POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"));
        a4.setLocation(new WKTReader().read("POLYGON ((4 4,8 4, 8 7,4 7,4 4))"));


        // intersect a,b and c
        String polygonIncluding_A1_A2_A3 = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))";


        //api/imageinstance/$idImage/annotation/included.json?geometry=${geometry.replace(" ","%20")}" + (terms? "&terms=${terms.join(',')}" : "") + "&user=${idUser}"

        restAnnotationDomainControllerMockMvc.perform(get("/api/imageinstance/"+image.getId()+"/annotation/included.json")
                        .param("geometry", polygonIncluding_A1_A2_A3.toString())
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("bbox", polygonIncluding_A1_A2_A3.toString())
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

        UserAnnotation intersectAnnotation = builder.given_a_user_annotation(slice, polygonIncluding_A1_A2_A3, me, term);

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("bboxAnnotation",intersectAnnotation.getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + intersectAnnotation.getId() + ")]").exists())
                .andReturn();

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("bboxAnnotation",intersectAnnotation.getId().toString())
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                        .param("excludedAnnotation", intersectAnnotation.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + intersectAnnotation.getId() + ")]").doesNotExist())
                .andReturn();
    }



    void list_user_annotation_with_suggested_term() {
        throw new CytomineMethodNotYetImplementedException("todo after job/algoannot/...");

        //
//        //create annotation
//        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
//        annotationTerm.term = BasicInstanceBuilder.getTerm()
//        BasicInstanceBuilder.saveDomain(annotationTerm)
//        UserAnnotation annotation = annotationTerm.userAnnotation
//
//        //create job
//        UserJob userJob = BasicInstanceBuilder.getUserJob(annotation.project)
//        Job job = userJob.job
//
//        //create suggest with different term
//        AlgoAnnotationTerm suggest = BasicInstanceBuilder.getAlgoAnnotationTerm(job,annotation,userJob)
//        suggest.term = BasicInstanceBuilder.getAnotherBasicTerm()
//        BasicInstanceBuilder.saveDomain(suggest)
//
//        println "project=${annotation.project.id}"
//        println "a.term=${annotation.terms().collect{it.id}.join(",")}"
//        println "at.term=${suggest.term.id}"
//        println "job=${job.id}"
//        println "user=${UserJob.findByJob(job).id}"
//
//        def result = AnnotationDomainAPI.listByProjectAndTermWithSuggest(annotation.project.id, annotationTerm.term.id, suggest.term.id, UserJob.findByJob(job).id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert AnnotationDomainAPI.containsInJSONList(annotation.id,json)
//
//        //create annotation
//        AnnotationTerm annotationTerm2 = BasicInstanceBuilder.getAnnotationTerm()
//        annotationTerm2.userAnnotation = BasicInstanceBuilder.getUserAnnotationNotExist(annotationTerm2.container(),true)
//        annotationTerm2.term = BasicInstanceBuilder.getTerm()
//        BasicInstanceBuilder.saveDomain(annotationTerm2)
//        UserAnnotation annotation2 = annotationTerm2.userAnnotation
//
//        //create suggest with same term
//        AlgoAnnotationTerm suggest2 = BasicInstanceBuilder.getAlgoAnnotationTerm(job,annotation2,userJob)
//        suggest2.term = BasicInstanceBuilder.getTerm()
//        BasicInstanceBuilder.saveDomain(suggest)
//
//        //We are looking for a different term => annotation shouldn't be in result
//        result = AnnotationDomainAPI.listByProjectAndTermWithSuggest(annotation2.project.id, annotationTerm2.term.id, suggest.term.id, job.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert !AnnotationDomainAPI.containsInJSONList(annotation2.id,json)
    }



    @Test
    @Transactional
    public void list_user_annotation_without_term() throws Exception {

        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("noTerm", "true")
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").exists())
                .andReturn();

    }

    @Test
    @Transactional
    public void list_user_annotation_with_multiple_term() throws Exception {
        builder.given_an_annotation_term(a1);
        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("multipleTerm", "true")
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andReturn();

    }

    @Test
    @Transactional
    public void list_user_annotation_with_several_identical_term() throws Exception {
        AnnotationTerm annotationTerm = new AnnotationTerm();
        annotationTerm.setUserAnnotation(a1);
        annotationTerm.setUser(builder.given_a_user());
        annotationTerm.setTerm(builder.given_a_term(project.getOntology()));
        builder.persistAndReturn(annotationTerm);
        em.refresh(a1);
        restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json")
                        .param("multipleTerm", "true")
                        .param("user", this.me.getId().toString())
                        .param("image", this.image.getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a3.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a4.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")].userByTerm[?(@.term==" + a1.getTerms().get(0).getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")].userByTerm[?(@.term==" + a1.getTerms().get(1).getId() + ")]").exists())
                .andReturn();
    }


    @Test
    @Transactional
    public void list_user_annotation_with_same_request_as_default_viewer() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        // this is the kind of request that the viewer send (without much configuration)
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("max", 0);
        jsonObject.put("offset", 0);
        jsonObject.put("showDefault", true);
        jsonObject.put("showWKT", true);
        jsonObject.put("showGIS", true);
        jsonObject.put("showTerm", true);
        jsonObject.put("notReviewedOnly", false);
        jsonObject.put("image", userAnnotation.getImage().getId());
        jsonObject.put("slice", userAnnotation.getSlice().getId());
        jsonObject.put("user", userAnnotation.getUser().getId());
        jsonObject.put("kmeans", true);
        jsonObject.put("bbox", "0,0,102400,76288");
        jsonObject.put("showTrack", true);
        //{"max":0,"offset":0,"showDefault":true,"showWKT":true,"showGIS":true,"showTerm":true,"notReviewedOnly":false,"image":39958,"slice":39959,"user":58,"kmeans":true,"bbox":"0,0,102400,76288","showTrack":true}
        restAnnotationDomainControllerMockMvc.perform(post("/api/annotation/search.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + userAnnotation.getId() + ")]").exists())
                .andReturn();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    public void add_valid_user_annotation() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        restAnnotationDomainControllerMockMvc.perform(post("/api/annotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userAnnotation.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.userannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddUserAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.annotation.id").exists());

    }



}
