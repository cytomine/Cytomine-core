package be.cytomine;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.StringUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestUtils {

    public static String getResourceFileAsString(String fileName) {
        InputStream is = getResourceFileAsInputStream(fileName);
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return (String)reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } else {
            throw new RuntimeException("resource not found");
        }
    }

    public static InputStream getResourceFileAsInputStream(String fileName) {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public static void checkSpreadsheetAnnotationResult(String delimiter, MvcResult result, AnnotationDomain annotationDomain, Project project, ImageInstance imageInstance, User user, Term term) throws UnsupportedEncodingException {
        String[] rows = result.getResponse().getContentAsString().split("\n");
        String[] userAnnotationResult = rows[1].split(delimiter);
        assertThat(userAnnotationResult[0]).isEqualTo(annotationDomain.getId().toString());
        assertThat(userAnnotationResult[1]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getArea()));
        assertThat(userAnnotationResult[2]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getPerimeter()));
        assertThat(userAnnotationResult[3]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getX()));
        assertThat(userAnnotationResult[4]).isEqualTo(StringUtils.decimalFormatter(annotationDomain.getCentroid().getY()));
        assertThat(userAnnotationResult[5]).isEqualTo(imageInstance.getId().toString());
        assertThat(userAnnotationResult[6]).isEqualTo(imageInstance.getBlindInstanceFilename());
        assertThat(userAnnotationResult[7]).isEqualTo(user.getUsername());
        assertThat(userAnnotationResult[8]).isEqualTo(term.getName());
        assertThat(userAnnotationResult[9]).isEqualTo("http://localhost:8080/api/userannotation/"+ annotationDomain.getId() +"/crop.png");
        assertThat(userAnnotationResult[10].replace("\r","")).isEqualTo("http://localhost:8080/#/project/"+project.getId()+"/image/"+imageInstance.getId()+"/annotation/"+annotationDomain.getId());
    }
}
