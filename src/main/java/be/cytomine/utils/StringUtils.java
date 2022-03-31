package be.cytomine.utils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public static List<Long> extractListFromParameter(String parameter) {
        if (parameter==null ||  parameter.isEmpty()) {
            return null;
        }
        return Arrays.stream(parameter.replaceAll("_",",").split(",")).map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public static String decimalFormatter(Object value){
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(value);
    }
}
