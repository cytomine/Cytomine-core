package be.cytomine.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "lti", ignoreUnknownFields = false)
public class LtiProperties {

    private boolean enabled;

    private List<LtiConsumerProperties> consumers;
}

