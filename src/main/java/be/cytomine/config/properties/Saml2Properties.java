package be.cytomine.config.properties;


import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

@Data
public class Saml2Properties {


    @NestedConfigurationProperty
    private Map<String, String> shibboleth;
    //Add configuration mapping for your SAML2 identity provider here
    //Example:
    //@NestedConfigurationProperty
    //private Map<String, String> okta;


}
