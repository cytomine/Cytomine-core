package be.cytomine.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthenticationConfiguration {

    JwtConfiguration jwt = new JwtConfiguration();
}
