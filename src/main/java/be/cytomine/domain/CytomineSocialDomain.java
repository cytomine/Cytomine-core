package be.cytomine.domain;

import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;

public abstract class CytomineSocialDomain {

//    public static JsonObject getDataFromDomain(CytomineSocialDomain domain) {
//        throw new WrongArgumentException("getDataFromDomain is not implemented for this class");
//    }

    public abstract JsonObject toJsonObject();
}
