package be.cytomine.api;


import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<?> handleException(ObjectNotFoundException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()));
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleException(ForbiddenException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(jsonObject.toJsonString());
    }

    @ExceptionHandler(CytomineMethodNotYetImplementedException.class)
    public ResponseEntity<?> handleException(CytomineMethodNotYetImplementedException exception) {
        exception.printStackTrace();
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  "Method is not yet implemented"));
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(WrongArgumentException.class)
    public ResponseEntity<?> handleException(WrongArgumentException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }

}
