package be.cytomine.api.controller.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.meta.PropertyRepository;
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
    public void list_annotation_property_show() throws Exception {

        MvcResult result = restAnnotationDomainControllerMockMvc.perform(get("/api/annotation.json").param("image", this.image.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + a1.getId() + ")]").exists())
                .andReturn();
       
        checkForProperties(result.getResponse().getContentAsString(),List.of("id","term","created","project","image"), List.of());
    }


//
//    void list_annotation_property_show() {
//
//        def dataSet = createAnnotationSet()
//
//        def result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,null)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        println json
//        println json.collection.get(0)
//        checkForProperties(json.collection.get(0),['id','term','created','project','image'])
//
//        def expectedProp = ['showBasic', 'showWKT']
//        println "expectedProp=$expectedProp"
//        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
//        json = (JSON.parse(result.data))
//        println "x=" + json
//        println "x=" + json.collection
//
//        println  json.collection
//        checkForProperties(json.collection.get(0),['id',"location"],['term','created','area','project'])
//
//        expectedProp = ['showDefault', 'hideMeta']
//        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
//        json = (JSON.parse(result.data))
//        checkForProperties(json.collection.get(0),['id','term'],['location','created','project'])
//
//        expectedProp = ['showBasic', 'showImage']
//        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
//        json = (JSON.parse(result.data))
//        checkForProperties(json.collection.get(0),['id','originalFilename'],['term','location'])
//
//        expectedProp = ['showWKT', 'hideWKT','hideBasic','hideMeta']
//        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
//        assert 404 == result.code
//    }
//
//
//
//
//    void testListAnnotationSearchByImage() {
//
//        def dataSet = createAnnotationSet()
//
//        def result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==dataSet.annotations.size()
//        //generic way test
//        checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size())
//
//        dataSet.annotations[2].image = BasicInstanceBuilder.getImageInstanceNotExist( dataSet.project,true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[2])
//
//        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert JSON.parse(result.data).collection instanceof JSONArray
//        assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() -1
//        //generic way test
//        checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size()-1)
//
//        UserAnnotationAPI.delete(dataSet.annotations[1].id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size()-2)
//    }
//
//    void testListAnnotationSearchByMultipleTerm() {
//
//        def dataSet = createAnnotationSet()
//
//        def result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==0
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",0)
//
//        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist(dataSet.annotations[2],true)
//        BasicInstanceBuilder.saveDomain(at)
//
//        result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert JSON.parse(result.data).collection instanceof JSONArray
//        assert JSON.parse(result.data).collection.size()==1
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",1)
//
//        result = AnnotationTermAPI.deleteAnnotationTerm(at.userAnnotation.id,at.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//
//        result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert JSON.parse(result.data).collection instanceof JSONArray
//        assert JSON.parse(result.data).collection.size()==0
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",0)
//    }
//
//
//    void testListAnnotationSearchByNoTerm() {
//        def dataSet = createAnnotationSet()
//        def result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==1
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&noTerm=true&project=${dataSet.project.id}",1)
//
//        result = AnnotationTermAPI.deleteAnnotationTerm(dataSet.annotations[0].id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//
//        result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==2
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&noTerm=true&project=${dataSet.project.id}",2)
//    }
//
//    void testListAnnotationSearchByProjectTerm() {
//        def dataSet = createAnnotationSet()
//        def result = UserAnnotationAPI.listByProjectAndUsers(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==dataSet.annotations.size()
//        //generic way test
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&project=${dataSet.project.id}",dataSet.annotations.size())
//
//    }
//
//
//
//    void testListAnnotationSearchByMaxDistance() {
//        def dataSet = createAnnotationSet()
//        dataSet.annotations[0].location = new WKTReader().read("POINT(0 0)") //base point
//        dataSet.annotations[1].location = new WKTReader().read("POINT(-10 0)") //should be < 11
//        dataSet.annotations[2].location = new WKTReader().read("POLYGON((10 0,15 10,15 15,10 15,10 0))") //should be < 11
//        dataSet.annotations[3].location = new WKTReader().read( "POINT(20 20)") //should be > 11
//
//        dataSet.annotations.each {
//            BasicInstanceBuilder.saveDomain(it)
//        }
//
//        checkUserAnnotationResults("project=${dataSet.project.id}&baseAnnotation="+dataSet.annotations[0].location.toText().replace(" ","%20")+"&maxDistanceBaseAnnotation=11",dataSet.annotations.subList(0,3),dataSet.annotations.subList(3,4))
//
//        checkUserAnnotationResults("project=${dataSet.project.id}&baseAnnotation="+dataSet.annotations[0].id+"&maxDistanceBaseAnnotation=11",dataSet.annotations.subList(0,3),dataSet.annotations.subList(3,4))
//
//    }
//
//
//    void testListAnnotationSearchByImageAndUser() {
//
//        def dataSet = createAnnotationSet()
//
//        def result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==dataSet.annotations.size()
//
//        //change image and user
//        dataSet.annotations[2].image = BasicInstanceBuilder.getImageInstanceNotExist( dataSet.project,true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[2])
//        dataSet.annotations[3].user = BasicInstanceBuilder.getUserNotExist(true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[3])
//
//        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert JSON.parse(result.data).collection instanceof JSONArray
//        assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() - 2 //we change 1 for image and 1 for user
//
//    }
//
//
//
//    void testListAnnotationSearchByImageAndUserAndBBox() {
//
//        def dataSet = createAnnotationSet()
//
//        def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
//        def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
//        def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
//        def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
//        //e intersect a,b and c
//        def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
//
//        dataSet.annotations[0].location = new WKTReader().read(a)
//        dataSet.annotations[1].location = new WKTReader().read(b)
//        dataSet.annotations[2].location = new WKTReader().read(c)
//        dataSet.annotations[3].location = new WKTReader().read(d)
//
//        dataSet.annotations.each {
//            BasicInstanceBuilder.saveDomain(it)
//        }
//
//        def result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==3 //a,b,c
//        //generic way test
//        checkUserAnnotationResultNumber("notReviewedOnly=true&user=${dataSet.user.id}&image=${dataSet.image.id}&bbox=${e.replace(" ","%20")}",3)
//
//        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//                result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,2,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//                result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,3,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//    }
//
//    void testListAnnotationSearchByTermAndProjectAndUser() {
//
//        def dataSet = createAnnotationSet()
//
//        println "TERMS=${Term.list().collect{it}.join(", ")}"
//
//        def result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==dataSet.annotations.size() - 1 //just 1 has no term
//        //generic way test
//        checkUserAnnotationResultNumber("project=${dataSet.project.id}&user=${dataSet.user.id}&term=${dataSet.term.id}",dataSet.annotations.size() - 1)
//
//        //change image and user
//        AnnotationTerm at = AnnotationTerm.findByUserAnnotation(dataSet.annotations[0])
//        at.term = BasicInstanceBuilder.getTermNotExist(true)
//        BasicInstanceBuilder.saveDomain(at)
//        dataSet.annotations[1].user = BasicInstanceBuilder.getUserNotExist(true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[1])
//
//        result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert JSON.parse(result.data).collection instanceof JSONArray
//        assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() - 3 //we change 1 for term and 1 for user, and 1 has no term
//        //generic way test
//        checkUserAnnotationResultNumber("project=${dataSet.project.id}&user=${dataSet.user.id}&term=${dataSet.term.id}",dataSet.annotations.size() - 3)
//
//    }
//
//    void testListAnnotationSearchByTerm() {
//
//        def dataSet = createAnnotationSet()
//        Term term2 =  BasicInstanceBuilder.getTermNotExist(dataSet.project.ontology,true)
//        term2 = BasicInstanceBuilder.saveDomain(term2)
//        UserAnnotation a1 =  dataSet.annotations[0]
//        def at = BasicInstanceBuilder.getAnnotationTermNotExist(a1,term2,true)
//        def result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,term2.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//
//        assert json.collection instanceof JSONArray
//        assert json.collection.size()==1
//        assert json.collection[0].term.size() == 2
//        assert json.collection[0].term.findAll{term2.id == it}.size() == 1
//    }
//
//    def testAnnotationIncludeFilterUserAnnotation() {
//
//        def dataSet = createAnnotationSet()
//
//        def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
//        def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
//        def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
//        def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
//        //e intersect a,b and c
//        def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
//
//
//        dataSet.annotations[0].location = new WKTReader().read(a)
//        dataSet.annotations[1].location = new WKTReader().read(b)
//        dataSet.annotations[2].location = new WKTReader().read(c)
//        dataSet.annotations[3].location = new WKTReader().read(d)
//
//        dataSet.annotations.each {
//            BasicInstanceBuilder.saveDomain(it)
//        }
//
//        //tatic def listIncluded(String geometry, Long idImage, Long idUser,List<Long> terms,String username, String password) {
//        def result = AnnotationDomainAPI.listIncluded(e, dataSet.image.id, dataSet.user.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection.size()==3 //d is not included!
//        //generic way test
//        checkUserAnnotationResultNumber("bbox=${e.replace(" ","%20")}&image=${dataSet.image.id}&user=${dataSet.user.id}",3)
//
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(dataSet.slice,e,dataSet.user,dataSet.term)
//
//        checkUserAnnotationResultNumber("bboxAnnotation=${annotation.id}&image=${dataSet.image.id}&user=${dataSet.user.id}",4)
//
//        checkUserAnnotationResultNumber("bboxAnnotation=${annotation.id}&image=${dataSet.image.id}&user=${dataSet.user.id}&excludedAnnotation=${annotation.id}",3)
//
//    }
//
//
//
//
//
//
//
//    void testAnnotationSearchWithSuggestedTerm() {
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
//    }
//
//    void testListAnnotationSearchGeneric() {
//
//        def dataSet = createAnnotationSet()
//
//        def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
//        def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
//        def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
//        def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
//        //e intersect a,b and c
//        def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
//
//        dataSet.annotations[0].location = new WKTReader().read(a)
//        dataSet.annotations[1].location = new WKTReader().read(b)
//        dataSet.annotations[2].location = new WKTReader().read(c)
//        dataSet.annotations[3].location = new WKTReader().read(d)
//
//        dataSet.annotations.each {
//            BasicInstanceBuilder.saveDomain(it)
//        }
//
//        checkUserAnnotationResultNumber("project=${dataSet.project.id}&hideMeta=true",4)
//
//        dataSet.annotations[0].image = BasicInstanceBuilder.getImageInstanceNotExist(dataSet.project,true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[0])
//
//        checkUserAnnotationResultNumber("image=${dataSet.image.id}",3)
//
//        dataSet.annotations[1].user = BasicInstanceBuilder.getUserNotExist(true)
//        BasicInstanceBuilder.saveDomain(dataSet.annotations[1])
//
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&image=${dataSet.image.id}&project=${dataSet.project.id}",2)
//
//        checkUserAnnotationResultNumber("user=${dataSet.user.id}&image=${dataSet.image.id}&project=${dataSet.project.id}&term=${dataSet.term.id}",1)
//
//
//        checkUserAnnotationResultNumber("users=${dataSet.user.id}&images=${dataSet.image.id}&project=${dataSet.project.id}",2)
//
//        checkUserAnnotationResultNumber("noTerm=true&project=${dataSet.project.id}",1)
//
//    }
//
//
//
//    void testListingUserAnnotationWithoutTerm() {
//        //create annotation without term
//        User user = BasicInstanceBuilder.getUser()
//        Project project = BasicInstanceBuilder.getProjectNotExist(true)
//        Infos.addUserRight(user.username,project)
//        Ontology ontology = BasicInstanceBuilder.getOntology()
//        project.ontology = ontology
//        project.save(flush: true)
//
//        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
//        image.project = project
//        image.save(flush: true)
//
//
//        UserAnnotation annotationWithoutTerm = BasicInstanceBuilder.getUserAnnotationNotExist()
//        annotationWithoutTerm.project = project
//        annotationWithoutTerm.image = image
//        annotationWithoutTerm.user = user
//        assert annotationWithoutTerm.save(flush: true)
//
//        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at.term.ontology = ontology
//        at.term.save(flush: true)
//        at.user = user
//        at.save(flush: true)
//        UserAnnotation annotationWithTerm = at.userAnnotation
//        annotationWithTerm.user = user
//        annotationWithTerm.project = project
//        annotationWithTerm.image = image
//        assert annotationWithTerm.save(flush: true)
//
//        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at2.term.ontology = ontology
//        at2.term.save(flush: true)
//        at2.user = BasicInstanceBuilder.getUser()
//        at2.save(flush: true)
//        UserAnnotation annotationWithTermFromOtherUser = at.userAnnotation
//        annotationWithTermFromOtherUser.user = user
//        annotationWithTermFromOtherUser.project = project
//        annotationWithTermFromOtherUser.image = image
//        assert annotationWithTermFromOtherUser.save(flush: true)
//
//        //list annotation without term with this user
//        def result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
//        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
//
//
//        //list annotation without term with this user
//        result = AnnotationDomainAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
//        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
//
//        //all images
//        result = AnnotationDomainAPI.listByProjectAndUsersWithoutTerm(project.id, user.id,null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
//        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
//    }
//
//
//
//    void testListingUserAnnotationWithSeveralTerm() {
//        //create annotation without term
//        User user = BasicInstanceBuilder.getUser()
//        Project project = BasicInstanceBuilder.getProjectNotExist(true)
//        Infos.addUserRight(user.username,project)
//        Ontology ontology = BasicInstanceBuilder.getOntology()
//        project.ontology = ontology
//        project.save(flush: true)
//
//        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
//        image.project = project
//        image.save(flush: true)
//
//        //annotation with no multiple term
//        UserAnnotation annotationWithNoTerm = BasicInstanceBuilder.getUserAnnotationNotExist()
//        annotationWithNoTerm.project = project
//        annotationWithNoTerm.image = image
//        annotationWithNoTerm.user = user
//        assert annotationWithNoTerm.save(flush: true)
//
//        //annotation with multiple term
//        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at.term.ontology = ontology
//        at.term.save(flush: true)
//        at.user = user
//        at.save(flush: true)
//        UserAnnotation annotationWithMultipleTerm = at.userAnnotation
//        annotationWithMultipleTerm.user = user
//        annotationWithMultipleTerm.project = project
//        annotationWithMultipleTerm.image = image
//        assert annotationWithMultipleTerm.save(flush: true)
//        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at2.term.ontology = ontology
//        at2.term.save(flush: true)
//        at2.user = user
//        at2.userAnnotation=annotationWithMultipleTerm
//        at2.save(flush: true)
//        AnnotationTerm at3 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at3.term.ontology = ontology
//        at3.term.save(flush: true)
//        at3.user = user
//        at3.userAnnotation=annotationWithMultipleTerm
//        at3.save(flush: true)
//
//
//        //annotation with multiple term
//        AnnotationTerm at4 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at4.term = at.term
//        at4.user = user
//        at4.save(flush: true)
//        UserAnnotation annotationWithMultipleTerm2 = at4.userAnnotation
//        annotationWithMultipleTerm2.user = user
//        annotationWithMultipleTerm2.project = project
//        annotationWithMultipleTerm2.image = image
//        assert annotationWithMultipleTerm2.save(flush: true)
//        AnnotationTerm at5 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at5.term = at2.term
//        at5.user = user
//        at5.userAnnotation=annotationWithMultipleTerm2
//        at5.save(flush: true)
//
//
//        //list annotation without term with this user
//        def result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection.size() == 2
//        assert json.collection.collect{it.userByTerm.size()}.contains(2)
//        assert json.collection.collect{it.userByTerm.size()}.contains(3)
//
//        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
//        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
//
//
//        //list annotation without term with this user
//        result = AnnotationDomainAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
//        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
//
//        //all images
//        result = AnnotationDomainAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
//        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
//    }
//
//    void testListingUserAnnotationWithSeveralIdenticalTerm() {
//        //create annotation without term
//        User user = BasicInstanceBuilder.getUser()
//        Project project = BasicInstanceBuilder.getProjectNotExist(true)
//        Infos.addUserRight(user.username,project)
//        Ontology ontology = BasicInstanceBuilder.getOntology()
//        project.ontology = ontology
//        project.save(flush: true)
//
//        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
//        image.project = project
//        image.save(flush: true)
//
//        //annotation with multiple term
//        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at.term.ontology = ontology
//        at.term.save(flush: true)
//        at.user = user
//        at.save(flush: true)
//        UserAnnotation annotationWithMultipleTerm = at.userAnnotation
//        annotationWithMultipleTerm.user = user
//        annotationWithMultipleTerm.project = project
//        annotationWithMultipleTerm.image = image
//        assert annotationWithMultipleTerm.save(flush: true)
//        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at2.term.ontology = ontology
//        at2.term.save(flush: true)
//        at2.user = user
//        at2.userAnnotation=annotationWithMultipleTerm
//        at2.save(flush: true)
//
//        AnnotationTerm at3 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at3.term = at.term
//        //at3.user = user
//        at3.userAnnotation=annotationWithMultipleTerm
//        at3.save(flush: true)
//
//
//        //annotation with multiple term
//        AnnotationTerm at4 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at4.term = at.term
//        at4.user = user
//        at4.save(flush: true)
//        UserAnnotation annotationWithMultipleTerm2 = at4.userAnnotation
//        annotationWithMultipleTerm2.user = user
//        annotationWithMultipleTerm2.project = project
//        annotationWithMultipleTerm2.image = image
//        assert annotationWithMultipleTerm2.save(flush: true)
//        AnnotationTerm at5 = BasicInstanceBuilder.getAnnotationTermNotExist()
//        at5.term = at2.term
//        at5.user = user
//        at5.userAnnotation=annotationWithMultipleTerm2
//        at5.save(flush: true)
//
//
//        //list annotation without term with this user
//        def result = UserAnnotationAPI.listByProjectAndTerm(project.id, at.term.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//
//        assert json.collection instanceof JSONArray
//        assert json.collection.size() == 2
//        assert json.collection.collect{it.userByTerm.size()}.contains(2)
//        assert json.collection.collect{it.userByTerm.size()}.contains(2)
//
//        def users = []
//        json.collection.collect{it.userByTerm}.each {
//            it.collect{it.user}.each{ u->
//                    users << u
//            }
//        }
//        assert users.collect{it.size()}.contains(2)
//
//        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
//        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm2.id,json)
//    }
//
//    void testListUserAnnotationByImageWithCredential() {
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = UserAnnotationAPI.listByImage(annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//    }
//
//    void testListUserAnnotationByImageNotExistWithCredential() {
//        def result = UserAnnotationAPI.listByImage(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//    }
//
//    void testListUserAnnotationByProjectWithCredential() {
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = UserAnnotationAPI.listByProject(annotation.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//                result = UserAnnotationAPI.listByProject(annotation.project.id, true,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//    }
//
//    void testListUserAnnotationByProjectNotExistWithCredential() {
//        def result = UserAnnotationAPI.listByProject(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//    }
//
//    void testListUserAnnotationByProjecImageAndUsertWithCredential() {
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = UserAnnotationAPI.listByProject(annotation.project.id, annotation.user.id, annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//    }
//
//
//
//
//    void testListUserAnnotationByImageAndUserWithCredential() {
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//
//                result = UserAnnotationAPI.listByImageAndUser(-99, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, -99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//    }
//
//
//    void testListUserAnnotationByProjectAndTermAndUserWithCredential() {
//        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
//
//        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//
//        result = UserAnnotationAPI.listByProjectAndTerm(-99, annotationTerm.term.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//
//        result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, -99, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//    }
//
//    void testListUserAnnotationByProjectAndTermWithUserNullWithCredential() {
//        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
//        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id, -1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 404 == result.code
//    }
//
//    void testListUserAnnotationByProjectAndTermAndUserAndImageWithCredential() {
//        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
//        println "SecUser=${SecUser.list().collect{it.id}.join(', ')}"
//        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id,annotationTerm.userAnnotation.image.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        //assert json.collection instanceof JSONArray
//    }
//
//    void testListUserAnnotationyProjectAndUsersWithCredential() {
//        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = UserAnnotationAPI.listByProjectAndUsers(annotation.project.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        //assert json.collection instanceof JSONArray
//    }
//
//
//    private static void checkUserAnnotationResultNumber(String url,int expectedResult) {
//        String URL = Infos.CYTOMINEURL+"api/annotation.json?$url"
//        def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection.size()==expectedResult
//    }
//
//    private static void checkUserAnnotationResults(String url,List<AnnotationDomain> expected, List<AnnotationDomain> notExpected) {
//        String URL = Infos.CYTOMINEURL+"api/annotation.json?$url"
//        def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//
//        expected.each { annotation ->
//            assert DomainAPI.containsInJSONList(annotation.id,json)
//        }
//        notExpected.each { annotation ->
//            assert !DomainAPI.containsInJSONList(annotation.id,json)
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    @WithMockUser(username = "list_image_instance_light_by_user")
//    @Test
//    @Transactional
//    public void list_image_instance_light_by_user() throws Exception {
//        ImageInstance image = builder.given_an_image_instance();
//        image.getBaseImage().setWidth(500);
//        ImageInstance imageFromOtherProjectNotAccessibleForUser = builder.given_an_image_instance();
//        User user = builder.given_a_user("list_image_instance_light_by_user");
//        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/user/{id}/imageinstance/light.json", user.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id=="+image.getId()+")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id=="+imageFromOtherProjectNotAccessibleForUser.getId()+")]").doesNotExist());
//
//    }
//
//
//    @Test
//    @Transactional
//    public void list_image_instance_by_projects() throws Exception {
//        Project project1 = builder.given_a_project();
//        Project anotherProject = builder.given_a_project();
//
//        ImageInstance imageInProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
//        ImageInstance imageInAnotherProject = builder.given_an_image_instance(builder.given_an_abstract_image(), anotherProject);
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
//                        .param("light", "true"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id==" + imageInAnotherProject.getId() + ")]").doesNotExist());
//
//
//    }
//
//    @Test
//    @Transactional
//    @WithMockUser("list_image_instance_by_projects_blind_filenames")
//    public void list_image_instance_by_projects_blind_filenames() throws Exception {
//        User user = builder.given_a_user("list_image_instance_by_projects_blind_filenames");
//        ImageInstance image = given_test_image_instance();
//
//        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE); // contributor
//
//        image.getProject().setBlindMode(true);
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", image.getProject().getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[0].filename").isEmpty());
//    }
//
//
//    @Test
//    @Transactional
//    public void list_image_instance_by_projects_tree() throws Exception {
//        Project project1 = builder.given_a_project();
//        Project anotherProject = builder.given_a_project();
//
//        ImageInstance image1InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
//        ImageInstance image2InProject1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project1);
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
//                        .param("tree", "true"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.children", hasSize(2)));
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project1.getId())
//                    .param("tree", "true").param("max", "1"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.children", hasSize(1)));
//    }
//
//
//    @Test
//    @Transactional
//    public void list_image_instance_by_project_with_annotation_filter() throws Exception {
//        Project project = builder.given_a_project();
//        // we add width filter to get only the image set defined in this test
//        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
//        image1.setCountImageAnnotations(2L);
//        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
//        image2.setCountImageAnnotations(4L);
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "0")
//                        .param("sort", "created")
//                        .param("order", "desc")
//                        .param("numberOfAnnotations[lte]", "5")
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "0")
//                        .param("sort", "created")
//                        .param("order", "desc")
//                        .param("numberOfAnnotations[lte]", "3")
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").doesNotExist());
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "0")
//                        .param("sort", "created")
//                        .param("order", "desc")
//                        .param("numberOfAnnotations[lte]", "4")
//                        .param("numberOfAnnotations[gte]", "2")
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").exists())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "0")
//                        .param("sort", "created")
//                        .param("order", "desc")
//                        .param("numberOfAnnotations[lte]", "4")
//                        .param("numberOfAnnotations[gte]", "3")
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image1.getId() + ")]").doesNotExist())
//                .andExpect(jsonPath("$.collection[?(@.id==" + image2.getId() + ")]").exists());
//    }
//
//
//    @Test
//    @Transactional
//    public void list_image_instance_by_project_with_pagination() throws Exception {
//        Project project = builder.given_a_project();
//        int width = Math.abs(new Random().nextInt());
//        // we add width filter to get only the image set defined in this test
//        ImageInstance image1 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
//        image1.getBaseImage().setWidth(width);
//        ImageInstance image2 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
//        image2.getBaseImage().setWidth(width);
//        ImageInstance image3 = builder.given_an_image_instance(builder.given_an_abstract_image(), project);
//        image3.getBaseImage().setWidth(width);
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "0")
//                        .param("width[equals]",  String.valueOf(width))
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
//                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
//                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
//                .andExpect(jsonPath("$.offset").value(0))
//                .andExpect(jsonPath("$.perPage").value(3))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(1));
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "1")
//                        .param("width[equals]",  String.valueOf(width))
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
//                .andExpect(jsonPath("$.offset").value(0))
//                .andExpect(jsonPath("$.perPage").value(1))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(3));
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "1")
//                        .param("max", "1")
//                        .param("width[equals]",  String.valueOf(width))
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
//                .andExpect(jsonPath("$.offset").value(1))
//                .andExpect(jsonPath("$.perPage").value(1))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(3));
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "1")
//                        .param("max", "0")
//                        .param("width[equals]", String.valueOf(width)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
//                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
//                .andExpect(jsonPath("$.offset").value(1))
//                .andExpect(jsonPath("$.perPage").value(2))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(1));
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "0")
//                        .param("max", "500")
//                        .param("width[equals]", String.valueOf(width)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(3)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
//                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
//                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
//                .andExpect(jsonPath("$.offset").value(0))
//                .andExpect(jsonPath("$.perPage").value(3))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(1));
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/project/{id}/imageinstance.json", project.getId())
//                        .param("offset", "500")
//                        .param("max", "0")
//                        .param("width[equals]", String.valueOf(width)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
//                .andExpect(jsonPath("$.offset").value(500))
//                .andExpect(jsonPath("$.perPage").value(0))
//                .andExpect(jsonPath("$.size").value(3))
//                .andExpect(jsonPath("$.totalPages").value(1));
//    }
//
//
//
//
//
//    @Test
//    @Transactional
//    public void get_next_image_instance() throws Exception {
//        Project project = builder.given_a_project();
//        ImageInstance imageInstance1 = builder.given_an_image_instance(
//                builder.given_an_abstract_image(), project
//        );
//        ImageInstance imageInstance2 = builder.given_an_image_instance(
//                builder.given_an_abstract_image(), project
//        );
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance2.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(imageInstance1.getId().intValue()));
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/next.json", imageInstance1.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isEmpty());
//    }
//
//    @Test
//    @Transactional
//    public void get_previous_image_instance() throws Exception {
//        Project project = builder.given_a_project();
//        ImageInstance imageInstance1 = builder.given_an_image_instance(
//                builder.given_an_abstract_image(), project
//        );
//        ImageInstance imageInstance2 = builder.given_an_image_instance(
//                builder.given_an_abstract_image(), project
//        );
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance1.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(imageInstance2.getId().intValue()));
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/previous.json", imageInstance2.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isEmpty());
//    }
//
//
//
//    @Test
//    @Transactional
//    public void add_valid_image_instance() throws Exception {
//        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance();
//        restImageInstanceControllerMockMvc.perform(post("/api/imageinstance.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(imageInstance.toJSON()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.imageinstance.id").exists());
//
//    }
//
//    @Test
//    @Transactional
//    public void edit_valid_image_instance() throws Exception {
//        Project project = builder.given_a_project();
//        ImageInstance imageInstance = builder.given_an_image_instance();
//        JsonObject jsonObject = imageInstance.toJsonObject();
//        jsonObject.put("project", project.getId());
//        restImageInstanceControllerMockMvc.perform(put("/api/imageinstance/{id}.json", imageInstance.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(jsonObject.toJsonString()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
//                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditImageInstanceCommand"))
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.imageinstance.id").exists())
//                .andExpect(jsonPath("$.imageinstance.project").value(project.getId()));
//
//
//    }
//
//
//    @Test
//    @Transactional
//    public void delete_image_instance() throws Exception {
//        ImageInstance imageInstance = builder.given_an_image_instance();
//        restImageInstanceControllerMockMvc.perform(delete("/api/imageinstance/{id}.json", imageInstance.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.callback.imageinstanceID").exists())
//                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteImageInstanceCommand"))
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.imageinstance.id").exists());
//
//
//    }
//
//
//    @Test
//    @Transactional
//    public void get_image_instance_thumb() throws Exception {
//        ImageInstance image = given_test_image_instance();
//        configureFor("localhost", 8888);
//        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512"))
//                .willReturn(
//                        aResponse().withBody(new byte[]{0,1,2,3})
//                )
//        );
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png?maxSize=512", image.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3});
//    }
//
//    @Test
//    @Transactional
//    public void get_image_instance_thumb_if_image_not_exist() throws Exception {
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/thumb.png", 0))
//                .andDo(print())
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.errors").exists());
//    }
//
//    @Test
//    @Transactional
//    public void get_image_instance_preview() throws Exception {
//        ImageInstance image = given_test_image_instance();
//        configureFor("localhost", 8888);
//        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=1024"))
//                .willReturn(
//                        aResponse().withBody(new byte[]{0,1,2,3})
//                )
//        );
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/preview.png?maxSize=1024", image.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3});
//    }
//
//
//    @Test
//    @Transactional
//    public void get_image_instance_associeted_label() throws Exception {
//        ImageInstance image = given_test_image_instance();
//        configureFor("localhost", 8888);
//        stubFor(get(urlEqualTo("/image/associated.json?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs"))
//                .willReturn(
//                        aResponse().withBody("[\"macro\",\"thumbnail\",\"label\"]")
//                )
//        );
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated.json", image.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
//                .andExpect(jsonPath("$.collection", containsInAnyOrder("label","macro","thumbnail")));
//    }
//
//
//    @Test
//    @Transactional
//    public void get_image_instance_associeted_label_macro() throws Exception {
//        ImageInstance image = given_test_image_instance();
//        configureFor("localhost", 8888);
//
//        stubFor(get(urlEqualTo("/image/nested.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512&label=macro"))
//                .willReturn(
//                        aResponse().withBody(new byte[]{0,1,2,3,4})
//                )
//        );
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/associated/{label}.png", image.getId(), "macro")
//                        .param("maxSize", "512"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3,4});
//    }
//
//
//    @Test
//    @Transactional
//    public void get_image_instance_crop() throws Exception {
//        ImageInstance image = given_test_image_instance();
//
//        configureFor("localhost", 8888);
//
//
//        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=crop";
//        stubFor(get(urlEqualTo(url))
//                .willReturn(
//                        aResponse().withBody(new byte[]{99})
//                )
//        );
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/crop.png", image.getId())
//                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{99});
//    }
//
//    @Test
//    @Transactional
//    public void get_image_instance_window() throws Exception {
//        ImageInstance image = given_test_image_instance();
//
//        configureFor("localhost", 8888);
//        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";
//        stubFor(get(urlEqualTo(url))
//                .willReturn(
//                        aResponse().withBody(new byte[]{123})
//                )
//        );
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/window-10-20-30-40.png", image.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{123});
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/window_url-10-20-30-40.jpg", image.getId()))
//                .andDo(print())
//                .andExpect(jsonPath("$.url").value("http://localhost:8888/slice/crop.jpg?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop"))
//                .andExpect(status().isOk());
//
//    }
//
//
//    @Test
//    @Transactional
//    public void get_image_instance_camera() throws Exception {
//        ImageInstance image = given_test_image_instance();
//
//        configureFor("localhost", 8888);
//        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";
//        stubFor(get(urlEqualTo(url))
//                .willReturn(
//                        aResponse().withBody(new byte[]{123})
//                )
//        );
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/camera-10-20-30-40.png", image.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andReturn();
//        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
//        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{123});
//
//
//        restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/camera_url-10-20-30-40.jpg", image.getId()))
//                .andDo(print())
//                .andExpect(jsonPath("$.url").value("http://localhost:8888/slice/crop.jpg?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop"))
//                .andExpect(status().isOk());
//
//    }
//
//
//
//    @Test
//    public void download_image_instance() throws Exception {
//        ImageInstance image = given_test_image_instance();
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
//                .andDo(print()).andReturn();
//        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
//        assertThat(mvcResult.getResponse().getHeader("Location"))
//                .isEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");
//
//
//    }
//
//
//    @Test
//    @WithMockUser("download_image_instance_cannot_download")
//    public void download_image_instance_cannot_download() throws Exception {
//        User user = builder.given_a_user("download_image_instance_cannot_download");
//
//        ImageInstance image = given_test_image_instance();
//        builder.addUserToProject(image.getProject(), user.getUsername(), BasePermission.WRITE);
//        image.getProject().setAreImagesDownloadable(true);
//
//        MvcResult mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
//                .andDo(print()).andReturn();
//        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
//        assertThat(mvcResult.getResponse().getHeader("Location"))
//                .isEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");
//
//        image.getProject().setAreImagesDownloadable(false);
//
//        mvcResult = restImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{id}/download", image.getId()))
//                .andDo(print()).andReturn();
//        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(403);
//        assertThat(mvcResult.getResponse().getHeader("Location"))
//                .isNotEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");
//
//
//    }
//
//
//
//
//
//
//















}
