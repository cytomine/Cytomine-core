package be.cytomine.security;

import be.cytomine.domain.security.SecUser;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AuthWithToken {

    SecUser user;
    Date expiryDate;
    String tokenKey;

    boolean isValid(){
        return expiryDate.after(new Date());
    }

}
