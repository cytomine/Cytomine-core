package be.cytomine.service.middleware;

import be.cytomine.api.JsonResponseEntity;
import be.cytomine.exceptions.MiddlewareException;
import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AppEngineService {

    @Value("${application.appEngine.url}")
    private String url;

    @Value("${application.appEngine.apiBasePath}")
    private String apiBasePath;

    private String buildFullUrl(String uri) {
        return url + apiBasePath + uri;
    }

    public ResponseEntity<String> get(String uri) {
        try {
            return new RestTemplate().getForEntity(buildFullUrl(uri), String.class);
        } catch (HttpClientErrorException e) {
            return JsonResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException.InternalServerError e) {
            throw new MiddlewareException("App engine returned a 500 HTTP error.");
        }
    }

    public ResponseEntity<String> post(String uri, MultiValueMap<String, Object> body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            return new RestTemplate().postForEntity(buildFullUrl(uri), request, String.class);
        } catch (HttpClientErrorException e) {
            return JsonResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException.InternalServerError e) {
            throw new MiddlewareException("App engine returned a 500 HTTP error.");
        }
    }
}
