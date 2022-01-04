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

    public boolean noExtension() {
        return !withRoles && !withLastImage && !withLastConnection && !withNumberConnections && !withUserJob;
    }
}
