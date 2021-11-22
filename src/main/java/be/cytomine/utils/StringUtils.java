package be.cytomine.utils;

public class StringUtils {

    public static boolean isNotBlank(String str) {
        return str != null && !str.equals("");
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().equals("");
    }
}
