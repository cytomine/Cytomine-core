package be.cytomine.exceptions;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class AuthenticationException extends CytomineException {

    /**
     * Message map with this exception
     * @param message Message
     */
    public AuthenticationException(String message) {
        this(message, new LinkedHashMap<Object, Object>());
    }
    public AuthenticationException(String message, Map<Object, Object> values) {
        super(message,401, values);
        log.warn(message);
    }

}