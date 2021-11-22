package be.cytomine.service;

import org.springframework.stereotype.Service;

import java.util.List;


public class UrlApi {

    private static String serverUrl = "http://localhost:8090";

    private static boolean httpInternally = false;

    private static final List<String> formatsWithMacro = List.of("openslide/ndpi", "openslide/vms", "openslide/mrxs", "openslide/svs", "openslide/scn", "ventana/bif", "ventana/tif", "philips/tif");

    public static void setServerURL(String url, Boolean useHTTPInternally) {
        serverUrl = useHTTPInternally ? url.replace("https", "http") : url;
        httpInternally = useHTTPInternally;
    }

    public static String getServerUrl() {
        return serverUrl;
    }


    public static boolean isUsingHttpInternally() {
        return httpInternally;
    }

    public static String getAbstractImageThumbUrl(Long idImage, String format) {
        return  serverUrl + "/api/abstractimage/" + idImage + "/thumb." + format;
    }

    public static String getImageInstanceThumbUrlWithMaxSize(Long idImage) {
        return (getImageInstanceThumbUrlWithMaxSize(idImage, 256, "png"));
    }

    public static String getImageInstanceThumbUrlWithMaxSize(Long idImage, Integer maxSize, String format) {
        return serverUrl + "/api/imageinstance/"+idImage+"/thumb."+format+"?maxSize=" + maxSize;
    }


    public static String getAssociatedImageInstance(Long id, String label, String contentType, Integer maxSize, String format) {

        if("macro".equals(label) && contentType!=null && !formatsWithMacro.contains(contentType)) {
            return null;
        }
        String size = maxSize!=null && maxSize!=0 ? "?maxWidth=" + maxSize : "";
        return serverUrl + "/api/imageinstance/" + id + "/associated/" + label +"." + format + size;
    }

}
