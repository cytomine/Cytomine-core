package be.cytomine.api.controller.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.vividsolutions.jts.io.ParseException;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
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
public class ReviewedAnnotationResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private CommandService commandService;

    @Autowired
    private MockMvc restReviewedAnnotationControllerMockMvc;

    @Autowired
    private ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

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

    @Test
    @Transactional
    public void get_a_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getTerms().add(builder.given_a_term());
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewedAnnotation.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.ontology.ReviewedAnnotation"))
                .andExpect(jsonPath("$.created").value(reviewedAnnotation.getCreated().getTime()))
                .andExpect(jsonPath("$.location").value(reviewedAnnotation.getWktLocation()))
                .andExpect(jsonPath("$.image").value(reviewedAnnotation.getImage().getId().intValue()))
                .andExpect(jsonPath("$.project").value(reviewedAnnotation.getProject().getId().intValue()))
                .andExpect(jsonPath("$.user").value(reviewedAnnotation.getUser().getId()))
                .andExpect(jsonPath("$.parentIdent").exists())
                .andExpect(jsonPath("$.parentClassName").exists())
                .andExpect(jsonPath("$.centroid.x").exists())
                .andExpect(jsonPath("$.centroid.y").exists())
                .andExpect(jsonPath("$.term", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.term[0]").value(reviewedAnnotation.getTerms().get(0).getId().intValue()))
        ;
    }

    @Test
    @Transactional
    public void get_a_reviewed_annotation_not_exists() throws Exception {
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void list_annotations() throws Exception {
        restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation.json"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void stats_annotations() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/imageinstance/{image}/reviewedannotation/stats.json", reviewedAnnotation.getImage().getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void count_annotations_by_project() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idUser}/reviewedannotation/count.json", reviewedAnnotation.getProject().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThan(0)));

        Project projectWithoutAnnotation = builder.given_a_project();
        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idUser}/reviewedannotation/count.json", projectWithoutAnnotation.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }


    @Test
    @Transactional
    public void count_annotations_by_project_with_dates() throws Exception {
        ReviewedAnnotation oldReviewedAnnotation = builder.given_a_reviewed_annotation();
        oldReviewedAnnotation.setCreated(DateUtils.addDays(new Date(), -1));

        ReviewedAnnotation newReviewedAnnotation =
                builder.persistAndReturn(builder.given_a_not_persisted_reviewed_annotation(oldReviewedAnnotation.getProject()));


        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(oldReviewedAnnotation.getCreated().getTime()))
                        .param("endDate", String.valueOf(newReviewedAnnotation.getCreated().getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(oldReviewedAnnotation.getCreated(),-1).getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(newReviewedAnnotation.getCreated(),1).getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("startDate", String.valueOf(DateUtils.addSeconds(newReviewedAnnotation.getCreated(),-1).getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addSeconds(oldReviewedAnnotation.getCreated(),1).getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        restReviewedAnnotationControllerMockMvc.perform(get("/api/project/{idProject}/reviewedannotation/count.json", oldReviewedAnnotation.getProject().getId())
                        .param("endDate", String.valueOf(DateUtils.addDays(oldReviewedAnnotation.getCreated(), -2).getTime())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }


    @Test
    @Transactional
    public void download_reviewed_annotation_document() throws Exception {
        Assertions.fail("todo...");
    }

    @Test
    @Transactional
    public void add_valid_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_not_persisted_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(post("/api/reviewedannotation.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }

    @Test
    @Transactional
    public void edit_valid_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(put("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }


    @Test
    @Transactional
    public void delete_reviewed_annotation() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/reviewedannotation/{id}.json", reviewedAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.reviewedannotationID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteReviewedAnnotationCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists());

    }


    @Test
    @Transactional
    public void delete_reviewed_annotation_not_exist_fails() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/reviewedannotation/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewedAnnotation.toJSON()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }



    @Test
    @Transactional
    public void start_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance.getReviewStart()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(post("/api/imageinstance/{image}/review.json", imageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andDo(print())
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStart()).isNotNull();
    }


    @Test
    @Transactional
    public void stop_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(imageInstance.getUser());
        assertThat(imageInstance.getReviewStop()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId()))
                .andDo(print())
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStop()).isNotNull();
    }

    @Test
    @Transactional
    public void cancel_image_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(imageInstance.getUser());
        assertThat(imageInstance.getReviewStop()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId())
                        .param("cancel", "true"))
                .andDo(print())
                .andExpect(status().isOk());
        assertThat(imageInstance.getReviewStart()).isNull();
        assertThat(imageInstance.getReviewStop()).isNull();
    }

    @Test
    @Transactional
    public void stop_image_review_refuse_if_image_not_started_to_review() throws Exception {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance.getReviewStart()).isNull();
        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/review.json", imageInstance.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    @Test
    @Transactional
    public void add_annotation_review() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());

        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();
        em.refresh(userAnnotation);
        restReviewedAnnotationControllerMockMvc.perform(post("/api/annotation/{annotation}/review.json", userAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andDo(print())
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId()).get().getTerms()).containsExactly(annotationTerm.getTerm());
    }



    @Test
    @Transactional
    public void add_annotation_review_with_terms_change() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        Term anotherTerm = builder.given_a_term(userAnnotation.getProject().getOntology());

        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();

        restReviewedAnnotationControllerMockMvc.perform(post("/api/annotation/{annotation}/review.json", userAnnotation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("terms", anotherTerm.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId()).get().getTerms()).containsExactly(anotherTerm);
    }


    @Test
    @Transactional
    public void remove_annotation_review() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isPresent();

        restReviewedAnnotationControllerMockMvc.perform(delete("/api/annotation/{annotation}/review.json", reviewedAnnotation.getParentIdent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isEmpty();
       }



    @Test
    @Transactional
    public void review_full_layer() throws Exception {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.getImage().setReviewStart(new Date());
        userAnnotation.getImage().setReviewUser(userAnnotation.getUser());

        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isEmpty();

        restReviewedAnnotationControllerMockMvc.perform(post("/api/imageinstance/{image}/annotation/review.json", userAnnotation.getImage().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("users", userAnnotation.getUser().getId().toString()))
                .andDo(print())
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(userAnnotation.getId())).isPresent();
    }


    @Test
    @Transactional
    public void unreview_full_layer() throws Exception {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(reviewedAnnotation.getUser());

        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isPresent();

        restReviewedAnnotationControllerMockMvc.perform(delete("/api/imageinstance/{image}/annotation/review.json", reviewedAnnotation.getImage().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("").param("users", reviewedAnnotation.getUser().getId().toString()))
                .andDo(print())
                .andExpect(status().isOk());


        assertThat(reviewedAnnotationRepository.findByParentIdent(reviewedAnnotation.getParentIdent())).isEmpty();
    }




    @Test
    @javax.transaction.Transactional
    public void get_reviewed_annotation_crop() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId() +"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/crop.png", annotation.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    @Test
    @javax.transaction.Transactional
    public void get_reviewed_annotation_crop_mask() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId() +"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=mask";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/mask.png", annotation.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }


    @Test
    @javax.transaction.Transactional
    public void get_reviewed_annotation_alpha_mask() throws Exception {
        ReviewedAnnotation annotation = given_a_reviewed_annotation_with_valid_image_server(builder);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        configureFor("localhost", 8888);
        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId() +"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=alphaMask";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        MvcResult mvcResult = restReviewedAnnotationControllerMockMvc.perform(get("/api/reviewedannotation/{id}/alphamask.png", annotation.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(mockResponse);
    }

    public static ReviewedAnnotation given_a_reviewed_annotation_with_valid_image_server(BasicInstanceBuilder builder) throws ParseException {
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
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation(
                        sliceInstance,
                "POLYGON((1 1,50 10,50 50,10 50,1 1))", builder.given_superadmin(), null);
        return reviewedAnnotation;
    }






}
