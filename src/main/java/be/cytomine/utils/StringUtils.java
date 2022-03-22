package be.cytomine.utils;

public class StringUtils {

    public static boolean isNotBlank(String str) {
        return str != null && !str.equals("");
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().equals("");
    }

    public static String obscurify(String property, int numberOfCharsToKeepOnEachSide) {
        if (property==null) {
            return "<NULL>";
        }
        if (numberOfCharsToKeepOnEachSide*2<property.length()) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i< property.length()-(numberOfCharsToKeepOnEachSide*2);i++) {
                buffer.append("*");
            }
            return property.substring(0,numberOfCharsToKeepOnEachSide)+ buffer.toString() + property.substring(property.length()-numberOfCharsToKeepOnEachSide,property.length());
        } else {
            return property;
        }

    }
}
