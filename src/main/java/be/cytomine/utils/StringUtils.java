package be.cytomine.utils;

public class StringUtils {

    public static boolean isNotBlank(String str) {
        return str != null && !str.equals("");
    }
}
