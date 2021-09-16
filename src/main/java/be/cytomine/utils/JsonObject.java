package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JsonObject extends HashMap<String, Object> {

    public static JsonObject of(String key, Object value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2) {
        JsonObject jsonObject = of(key, value);
        jsonObject.put(key2, value2);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        return jsonObject;
    }
    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        jsonObject.put(key5, value5);
        return jsonObject;
    }

    public String toJsonString() {
        return toJsonString(this);
    }

    public static String toJsonString(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "invalid json string";
        }
    }

    public static Map<String, Object> toObject(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getJSONAttrStr(String attr) {
        return getJSONAttrStr(attr, false, null);
    }

    public String getJSONAttrStr(String attr, boolean mandatory) {
        return getJSONAttrStr(attr, mandatory, null);
    }

    public String getJSONAttrStr(String attr, String defaultValue) {
        return getJSONAttrStr(attr, false, defaultValue);
    }

    public String getJSONAttrStr(String attr, boolean mandatory, String defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            return this.get(attr).toString();
        } else {
            if (mandatory) {
                throw new ServerException(attr + " must be set! value=" + this.get(attr));
            }
            return defaultValue;
        }
    }

    public Long getJSONAttrLong(String attr, Long defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Long.parseLong(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Boolean getJSONAttrBoolean(String attr, Boolean defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Boolean.parseBoolean(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Date getJSONAttrDate(String attr) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            return new Date(Long.parseLong(this.get(attr).toString()));
        } else {
            return null;
        }
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument class and its id (domain.read)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    public CytomineDomain getJSONAttrDomain(EntityManager entityManager, String attr, CytomineDomain domain, boolean mandatory) {
        return getJSONAttrDomain(entityManager, attr, domain, "id", "Long", mandatory);
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument, get the correct object thanks to value from column (type: columnType)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    public CytomineDomain getJSONAttrDomain(EntityManager entityManager, String attr, CytomineDomain domain, String column, String columnType, boolean mandatory) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            CytomineDomain domainRead;
            if(column.equals("id")) {
                domainRead = entityManager.find(domain.getClass(), Long.parseLong(this.get(attr).toString()));
            } else {
                throw new RuntimeException("Only retrieving domain from id is supported! Not yet supported");
            }
            if (domainRead == null) {
                throw new WrongArgumentException(attr + " was not found with id: " + this.get(attr));
            }
            return domainRead;
        } else {
            if (mandatory) {
                throw new WrongArgumentException(attr + " was not found with id: " + this.get(attr));
            }
            return null;
        }
    }




}
