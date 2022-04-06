package be.cytomine.security.current;

import be.cytomine.domain.security.SecUser;
import lombok.Data;

@Data
public class FullCurrentUser implements CurrentUser {

    private SecUser user;

    @Override
    public boolean isFullObjectProvided() {
        return true;
    }

    @Override
    public boolean isUsernameProvided() {
        return true;
    }
}
