package be.cytomine.security;

import be.cytomine.domain.security.SecUser;
import lombok.Data;

import java.util.Date;

@Data
public class AuthWithToken {

    SecUser user;
    Date expiryDate;
    String tokenKey;

    boolean isValid(){
        return expiryDate.after(new Date());
    }

}
