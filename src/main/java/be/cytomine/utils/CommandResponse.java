package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.UrlApi;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CommandResponse {

    Integer status;

    CytomineDomain object;

    Map<String, Object> data;

    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public static JsonObject getDataFromDomain(CommandResponse domain) {
       JsonObject jsonObject = new JsonObject();
       jsonObject.put("status", domain.getStatus());
       jsonObject.put("object", domain.getObject().toJsonObject());
       jsonObject.put("data", domain.getData());
       return jsonObject;
    }

}
