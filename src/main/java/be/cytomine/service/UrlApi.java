package be.cytomine.service;

import org.springframework.stereotype.Service;


public class UrlApi {

    public static String serverUrl = "http://localhost:8090";

    public static String getAbstractImageThumbUrl(Long idImage, String format) {
        return  serverUrl + "/api/abstractimage/" + idImage + "/thumb." + format;
    }

}
