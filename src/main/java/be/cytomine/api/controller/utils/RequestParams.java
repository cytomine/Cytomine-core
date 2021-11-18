package be.cytomine.api.controller.utils;

import java.util.HashMap;
import java.util.Map;

public class RequestParams extends HashMap<String, String> {

    public boolean isTrue(String key) {
        return !isNull(key) && get(key).equals("true");
    }

    public boolean isNull(String key) {
        return get(key)==null;
    }

    public boolean isValue(String key, String value) {
        return !isNull(key) && get(key).equals(value);
    }
}
