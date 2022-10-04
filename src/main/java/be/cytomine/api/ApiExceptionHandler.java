package be.cytomine.api;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.exceptions.*;
import be.cytomine.utils.JsonObject;
import com.sun.mail.smtp.SMTPSendFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  ex.getMessage()));
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.info("Request refused because body cannot be parsed:" + ex, ex);
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  "The body cannot be parsed, is the body content in the good format?"));
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  ex.getMessage()));
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(MiddlewareException.class)
    public ResponseEntity<?> handleException(MiddlewareException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  "Internal error"), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(jsonObject.toJsonString());
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<?> handleException(ObjectNotFoundException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleException(ForbiddenException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(jsonObject.toJsonString());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleException(AccessDeniedException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  "Cannot identify user or user is not authorized to log in"));
        return JsonResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleException(AuthenticationException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()));
        return JsonResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(jsonObject.toJsonString());
    }



    @ExceptionHandler(CytomineMethodNotYetImplementedException.class)
    public ResponseEntity<?> handleException(CytomineMethodNotYetImplementedException exception) {
        exception.printStackTrace();
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  "Method is not yet implemented"));
        return JsonResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(WrongArgumentException.class)
    public ResponseEntity<?> handleException(WrongArgumentException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }

    @ExceptionHandler(AlreadyExistException.class)
    public ResponseEntity<?> handleException(AlreadyExistException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }


    @ExceptionHandler(ConstraintException.class)
    public ResponseEntity<?> handleException(ConstraintException exception) {
        JsonObject jsonObject = JsonObject.of("errors", Map.of("message",  exception.getMessage()), "errorValues", exception.getValues());
        return JsonResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(jsonObject.toJsonString());
    }

    @ExceptionHandler(NotModifiedException.class)
    public ResponseEntity<?> handleException(NotModifiedException exception) {
        log.debug("NotModifiedException");
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, String> entry : exception.getHeaders().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }

        return JsonResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .headers(headers).build();
    }

}
