package be.cytomine.utils;

import org.apache.commons.text.CaseUtils;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.capitalize;

public class SQLUtils {

    public static String toCamelCase( String text) {
        return toCamelCase(text, false);
    }

    public static String toCamelCase( String text, boolean capitalized ) {
        return CaseUtils.toCamelCase(text, capitalized, new char[]{'_'});
    }


    public static Map<String, Object> keysToCamelCase(Map<String, Object> m) {
        Map<String, Object> newMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : m.entrySet()) {
            newMap.put(toCamelCase(entry.getKey()), entry.getValue());
        }
        return newMap;
    }
//
//    public static Map<String, Object> keysToCamelCase(Map<String, Object> m) {
//        Map<String, Object> newMap = new HashMap<>();
//
//        for (Map.Entry<String, Object> entry : m.entrySet()) {
//            newMap.put(toCamelCase(entry.getKey()), entry.getValue());
//        }
//        return newMap;
//    }

//    public static String toSnakeCase(String text) {
//        return CaseUtils.toSnakeCase(text).toLowerCase();
//    }

    public static String toSnakeCase(String str)
    {

        // Empty String
        String result = "";

        // Append first character(in lower case)
        // to result string
        char c = str.charAt(0);
        result = result + Character.toLowerCase(c);

        // Traverse the string from
        // ist index to last index
        for (int i = 1; i < str.length(); i++) {

            char ch = str.charAt(i);

            // Check if the character is upper case
            // then append '_' and such character
            // (in lower case) to result string
            if (Character.isUpperCase(ch)) {
                result = result + '_';
                result
                        = result
                        + Character.toLowerCase(ch);
            }

            // If the character is lower case then
            // add such character into result string
            else {
                result = result + ch;
            }
        }

        // return the result
        return result;
    }
}
