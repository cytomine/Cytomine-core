package be.cytomine.domain;

import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * When a domain has not a real reference to its container (e.g. algoAnnotationTerm.annotationIdent + class ; description.domainIdent + class), we fill this object with id/class.
 * When we perform an ACL, there is a special case to load the object from the database before calling its .container().
 * This is a hack because we cannot load the object directly from the DOMAIN.container() method
 */
public class GenericCytomineDomainContainer extends CytomineDomain {

    private String containerClass;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    @Override
    public String toString() {
        return "GenericCytomineDomainContainer{" +
                "id=" + id +
                ", containerClass='" + containerClass + '\'' +
                '}';
    }
}
