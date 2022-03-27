package be.cytomine.service.utils;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportColumn;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecUserServiceTests;
import be.cytomine.utils.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ReportFormatServiceTests {

    private Object[][] expectedDataObject;

    @Autowired
    ReportFormatService reportFormatService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    SecUserService secUserService;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    ProjectService projectService;

    @Autowired
    TermService termService;

    @Test
    public void annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(true, false),
                true,
                false);
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(false, false),
                true,
                false);
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void reviewed_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(true, true),
                true,
                true);
        assertArrayEquals(expectedDataObject, dataObject);
    }
    @Test
    public void incomplete_reviewed_annotations_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.ANNOTATION_REPORT_COLUMNS,
                buildAnnotations(false, true),
                true,
                true);
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void users_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.USER_REPORT_COLUMNS,
                buildUsers(true),
                false,
                false);
        assertArrayEquals(expectedDataObject, dataObject);
    }

    @Test
    public void incomplete_users_to_report_format() {
        Object[][] dataObject = reportFormatService.formatDataForReport(
                ReportService.USER_REPORT_COLUMNS,
                buildUsers(false),
                false,
                false);
        assertArrayEquals(expectedDataObject, dataObject);
    }

    private List<Map<String,Object>> buildAnnotations(boolean isComplete, boolean isReviewed){
        AnnotationDomain userAnnotation = builder.given_a_user_annotation();
        if(isReviewed){
            userAnnotation = builder.given_a_reviewed_annotation();
        }
        User user = builder.given_a_user();

        ImageInstance imageInstance = builder.given_an_image_instance();
        Term term1 = builder.given_a_term();
        Term term2 = builder.given_a_term();

        expectedDataObject = new Object[][]{
                {"Id", "Area (microns²)", "Perimeter (mm)", "X", "Y", "Image Id", "Image Filename", "User", "Term", "View annotation picture", "View annotation on image"},
                {
                    userAnnotation.getId(),
                    StringUtils.decimalFormatter(userAnnotation.getArea()),
                    StringUtils.decimalFormatter(userAnnotation.getPerimeter()),
                    StringUtils.decimalFormatter(userAnnotation.getCentroid().getX()),
                    StringUtils.decimalFormatter(userAnnotation.getCentroid().getY()),
                    imageInstance.getId(),
                    imageInstanceService.find(imageInstance.getId()).get().getBlindInstanceFilename(),
                    secUserService.findUser(user.getId()).get().humanUsername(),
                    termService.find(term1.getId()).get().getName() + "- " + termService.find(term2.getId()).get().getName(),
                    "http://cropURL",
                    "http://imageURL"
                },
        };
        Map<String,Object> annotations = new HashMap<>(Map.of(
                "id", userAnnotation.getId(),
                "image", imageInstance.getId(),
                "user", user.getId(),
                "term", term1.getId() + "," + term2.getId(),
                "cropURL", "http://cropURL",
                "imageURL", "http://imageURL"
        ));
        if(!isComplete){
            expectedDataObject[1][10] = "";
            annotations.remove("imageURL");
        }
        return new ArrayList<>(List.of(annotations));
    }

    private List<Map<String,Object>> buildUsers(boolean isComplete){
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();
        expectedDataObject = new Object[][]{
                {"User Name", "First Name", "Last Name"},
                {user1.getUsername(), user1.getUsername(), user1.humanUsername()},
                {user2.getUsername(), user2.getUsername(), user2.humanUsername()},
        };
        if(!isComplete){
            expectedDataObject[1][1] = "";
            return new ArrayList<>(List.of(
                    Map.of("username", user1.getUsername(), "lastname", user1.humanUsername()),
                    Map.of("username", user2.getUsername(), "firstname", user2.humanUsername(), "lastname", user2.humanUsername())));
        }else{
            return new ArrayList<>(List.of(
                    Map.of("username", user1.getUsername(), "firstname", user1.humanUsername(), "lastname", user1.humanUsername()),
                    Map.of("username", user2.getUsername(), "firstname", user2.humanUsername(), "lastname", user2.humanUsername())));
        }
    }
}
