package be.cytomine.utils;

import java.util.Date;

public class DateUtils {

    public static String getTimeToString(Date date) {
        if (date == null) {
            return null;
        } else {
            return String.valueOf(date.getTime());
        }
    }
}
