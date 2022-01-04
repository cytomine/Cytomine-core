package be.cytomine.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CytomineConfiguration {

    private String defaultLanguage;

    private List<String> imageServerURL;

    private String messageBrokerServerURL;

    private Software software;

    private Annotation annotation;

    public Software getSoftware() {
        return software;
    }

    public void setSoftware(Software software) {
        this.software = software;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
}
class Software {
    public Path path;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}

class Path {
    private String softwareImages;

    public String getSoftwareImages() {
        return softwareImages;
    }

    public void setSoftwareImages(String softwareImages) {
        this.softwareImages = softwareImages;
    }
}

class Annotation {
    int maxNumberOfPoint;

    public int getMaxNumberOfPoint() {
        return maxNumberOfPoint;
    }

    public void setMaxNumberOfPoint(int maxNumberOfPoint) {
        this.maxNumberOfPoint = maxNumberOfPoint;
    }
}
