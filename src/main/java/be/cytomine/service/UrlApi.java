package be.cytomine.service;

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

    public static String getImageInstanceThumbUrl(Long idImage) {
        return serverUrl + "/api/imageinstance/"+idImage+"/thumb.png";
    }

    public static String getImageInstanceThumbUrlWithMaxSize(Long idImage) {
        return (getImageInstanceThumbUrlWithMaxSize(idImage, 256, "png"));
    }

    public static String getImageInstanceThumbUrlWithMaxSize(Long idImage, Integer maxSize, String format) {
        return serverUrl + "/api/imageinstance/"+idImage+"/thumb."+format+"?maxSize=" + maxSize;
    }

    public static String getAbstractImageThumbUrlWithMaxSize(Long idAbstractImage, Integer maxSize, String format) {
        return serverUrl+"/api/abstractimage/"+idAbstractImage+"/thumb."+format+"?maxSize=" + maxSize;
    }



    public static String getAssociatedImageInstance(Long id, String label, String contentType, Integer maxSize, String format) {

        if("macro".equals(label) && contentType!=null && !formatsWithMacro.contains(contentType)) {
            return null;
        }
        String size = maxSize!=null && maxSize!=0 ? "?maxWidth=" + maxSize : "";
        return serverUrl + "/api/imageinstance/" + id + "/associated/" + label +"." + format + size;
    }


    public static String getAssociatedImage(Long idAbstractImage, String label, String contentType, Integer maxSize, String format) {

        if("macro".equals(label) && contentType!=null && !formatsWithMacro.contains(contentType)) {
            return null;
        }
        String size = maxSize!=null && maxSize!=0 ? "?maxWidth=" + maxSize : "";
        return serverUrl + "/api/abstractimage/" + idAbstractImage + "/associated/" + label +"." + format + size;
    }


    public static String getAnnotationURL(Long idProject, Long idImage, Long idAnnotation) {
        return  serverUrl + "/#/project/"+idProject+"/image/"+idImage+"/annotation/" + idAnnotation;
    }

    public static String getAbstractSliceThumbUrl(Long idSlice, String format) {
        return serverUrl + "/api/abstractslice/" + idSlice + "/thumb." + format;
    }

    public static String getUserAnnotationCropWithAnnotationId(Long idAnnotation, String format) {
        return serverUrl + "/api/userannotation/"+idAnnotation+"/crop." + format;
    }

    public static String getUserAnnotationCropWithAnnotationIdWithMaxSize(Long idAnnotation, int maxSize, String format) {
        return serverUrl + "/api/userannotation/"+idAnnotation+"/crop." + format + "?maxSize=" + maxSize;
    }

    public static String getROIAnnotationCropWithAnnotationId(Long idAnnotation, String format) {
        return serverUrl + "/api/roiannotation/"+idAnnotation+"/crop."+format;
    }

    public static String getROIAnnotationCropWithAnnotationIdWithMaxSize(Long idAnnotation, int maxSize, String format) {
        return serverUrl + "/api/roiannotation/"+idAnnotation+"/crop."+format+"?maxSize=" + maxSize;
    }

    public static String getAlgoAnnotationCropWithAnnotationId(Long idAnnotation, String format) {
        return serverUrl + "/api/algoannotation/"+idAnnotation+"/crop." + format;
    }

    public static String getAlgoAnnotationCropWithAnnotationIdWithMaxSize(Long idAnnotation, int maxsize, String format) {
        return serverUrl + "/api/algoannotation/"+idAnnotation+"/crop."+format+"?maxSize=" + maxsize;
    }

    public static String getReviewedAnnotationCropWithAnnotationId(Long idAnnotation, String format) {
        return serverUrl + "/api/reviewedannotation/"+idAnnotation+"/crop." + format;
    }

    public static String getReviewedAnnotationCropWithAnnotationIdWithMaxSize(Long idAnnotation, int maxSize, String format) {
        return "serverUrl + \"/api/reviewedannotation/"+idAnnotation+"/crop."+format+"?maxSize=" + maxSize;
    }

    public static String getAnnotationCropWithAnnotationId(Long idAnnotation, int maxSize, String format) {
        return serverUrl + "/api/annotation/"+idAnnotation+"/crop." + format + (maxSize!=0? "?maxSize=" + maxSize :"");
    }
}
