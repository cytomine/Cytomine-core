package be.cytomine.exceptions;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class ForbiddenException extends CytomineException {

    /**
     * Message map with this exception
     * @param message Message
     */
    public ForbiddenException(String message) {
        this(message, new LinkedHashMap<Object, Object>());
    }
    public ForbiddenException(String message, Map<Object, Object> values) {
        super(message,403, values);
    }

}