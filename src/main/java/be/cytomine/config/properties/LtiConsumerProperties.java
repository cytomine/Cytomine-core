package be.cytomine.config.properties;

import lombok.Data;

@Data
public class LtiConsumerProperties {

    private String key;

    private String name;

    private String secret;

    private String usernameParameter;

}
