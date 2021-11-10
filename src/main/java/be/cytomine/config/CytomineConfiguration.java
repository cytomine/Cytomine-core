package be.cytomine.config;

import lombok.Data;

import java.util.List;

@Data
public class CytomineConfiguration {

    private String defaultLanguage;

    private List<String> imageServerURL;

    private String messageBrokerServerURL;

    private Software software;

    public Software getSoftware() {
        return software;
    }

    public void setSoftware(Software software) {
        this.software = software;
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
