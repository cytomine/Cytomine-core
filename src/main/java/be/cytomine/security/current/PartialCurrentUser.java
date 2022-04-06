package be.cytomine.security.current;

import be.cytomine.domain.security.SecUser;
import lombok.Data;

@Data
public class PartialCurrentUser implements CurrentUser{

    String username;

    @Override
    public boolean isFullObjectProvided() {
        return false;
    }

    @Override
    public boolean isUsernameProvided() {
        return username!=null;
    }

    @Override
    public SecUser getUser() {
        SecUser secUser = new SecUser();
        secUser.setUsername(username);
        return secUser;
    }
}
