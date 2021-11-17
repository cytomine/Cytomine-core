package be.cytomine.service.search;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectSearchExtension {
    private boolean withDescription;
    private boolean withMembersCount;
    private boolean withLastActivity;
    private boolean withCurrentUserRoles;
}
