package be.cytomine.config;

import lombok.Data;

@Data
public class AuthenticationConfiguration {

    JwtConfiguration jwt = new JwtConfiguration();
}
