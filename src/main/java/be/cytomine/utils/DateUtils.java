package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class DateUtils {

    public static SimpleDateFormat MONGO_DB_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static String getTimeToString(Date date) {
        if (date == null) {
            return null;
        } else {
            return String.valueOf(date.getTime());
        }
    }

    public static Comparator<CytomineDomain> descCreatedComparator() {
        return (lhs, rhs) -> {
            if (lhs==null) {
                return 1;
            }
            if (rhs==null) {
                return -1;
            }
            if (lhs.getCreated().getTime() < rhs.getCreated().getTime())
                return 1;
            else if (lhs.getCreated().getTime() == rhs.getCreated().getTime())
                return 0;
            else
                return -1;
        };
    }
}
