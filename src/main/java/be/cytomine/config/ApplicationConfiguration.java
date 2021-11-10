package be.cytomine.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Data
public class ApplicationConfiguration {

    private String version;

    private String serverURL;

    private CytomineConfiguration cytomine;

    private String storagePath;

}