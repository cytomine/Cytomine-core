package be.cytomine.config.properties;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LtiConsumerProperties {

    private String key;

    private String name;

    private String secret;

    private String usernameParameter;

}
