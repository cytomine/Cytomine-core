package be.cytomine.config.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LdapProperties {

    private Boolean enabled;

    private String server;

    private String principal;

    private String password;

    private String search;

    private String attributes;

    private String passwordAttributeName;
}
