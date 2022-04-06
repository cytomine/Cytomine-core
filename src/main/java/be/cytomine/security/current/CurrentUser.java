package be.cytomine.security.current;

import be.cytomine.domain.security.SecUser;

public interface CurrentUser {

    boolean isFullObjectProvided();

    boolean isUsernameProvided();

    SecUser getUser();
}
