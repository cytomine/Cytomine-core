package be.cytomine.service.search;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSearchExtension {
    private boolean withRoles;

    private boolean withLastImage;

    private boolean withLastConnection;

    private boolean withNumberConnections;

    private boolean withUserJob;

    public static UserSearchExtension onlyWithRoles() {
        UserSearchExtension userSearchExtension = new UserSearchExtension();
        userSearchExtension.setWithRoles(true);
        return userSearchExtension;
    }

    public boolean noExtension() {
        return !withRoles && !withLastImage && !withLastConnection && !withNumberConnections && !withUserJob;
    }
}
