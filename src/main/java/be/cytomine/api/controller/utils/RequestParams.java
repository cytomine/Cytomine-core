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

    public Long getOffset() {
        return Long.parseLong(get("offset"));
    }

    public Long getMax() {
        return Long.parseLong(get("max"));
    }

    public String getSort() {
        return get("sort");
    }

    public String getOrder() {
        return get("order");
    }
}
