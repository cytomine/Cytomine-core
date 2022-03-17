package be.cytomine.config;

import lombok.Data;

@Data
public class JwtConfiguration {

    String secret;

    Long tokenValidityInSeconds;

    Long tokenValidityInSecondsForRememberMe;

    Long tokenValidityInSecondsForShortTerm;
}
