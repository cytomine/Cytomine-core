package be.cytomine.api;


import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.utils.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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



}
