package be.cytomine.dto;

import be.cytomine.domain.security.User;
import lombok.Data;

@Data
public class LoginWithRedirection {

    User user;

    String redirection;

}
