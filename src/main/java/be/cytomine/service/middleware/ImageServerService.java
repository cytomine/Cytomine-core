package be.cytomine.service.middleware;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.*;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.InvalidRequestException;
import be.cytomine.repository.image.CompanionFileRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.dto.ThumbParameter;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ImageServerService extends ModelService {

    private static final int GET_URL_MAX_LENGTH = 512;

    @Autowired
    ImageInstanceService imageInstanceService;

    private static String retrieveImageServerInternalUrl(AbstractSlice slice) {
        if (slice.getPath()==null) {
            throw new InvalidRequestException("Abstract slice has no valid path.");
        }

        return slice.getImageServerInternalUrl();
    }

    private static Map<String, Object> retrieveImageServerParameters(AbstractSlice slice) {
        if (slice.getPath()==null) {
            throw new InvalidRequestException("Abstract slice has no valid path.");
        }
        return new HashMap<>(Map.of("fif", slice.getPath(), "mimeType", slice.getMimeType()));
    }


    public byte[] thumb(ImageInstance image, ThumbParameter params)  {
        return thumb(imageInstanceService.getReferenceSlice(image), params);
    }

    public byte[] thumb(SliceInstance slice, ThumbParameter params)  {
        return thumb(slice.getBaseSlice(), params);
    }

    public byte[] thumb(AbstractSlice slice, ThumbParameter params) {
        String imageServerInternalUrl = retrieveImageServerInternalUrl(slice);
        Map<String, Object> parameters = retrieveImageServerParameters(slice);

        String format = checkFormat(params.getFormat(), List.of("jpg", "png"));
        parameters.put("maxSize", params.getMaxSize());
        parameters.put("colormap", params.getColormap());
        parameters.put("inverse", params.getInverse());
        parameters.put("contrast", params.getContrast());
        parameters.put("gamma", params.getGamma());
        parameters.put("bits", params.getMaxBits() ? Optional.ofNullable(slice.getImage().getBitDepth()).orElse(8) : params.getBits());

        return makeRequest(imageServerInternalUrl,"/slice/thumb." + format, parameters);
    }

    private byte[] makeRequest(String imageServerInternalUrl, String path, Map<String, Object> parameters) {
        return makeRequest("GET", imageServerInternalUrl, path,parameters);
    }

    private byte[] makeRequest(String httpMethod, String imageServerInternalUrl, String path, Map<String, Object> parameters)  {

        parameters = filterParameters(parameters);
        String parameterUrl = "";
        String fullUrl = "";

        try {
            parameterUrl = makeParameterUrl(parameters);
            fullUrl = imageServerInternalUrl + path + "?" + parameterUrl;
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        if ((fullUrl).length() > GET_URL_MAX_LENGTH && (httpMethod==null || httpMethod.equals("GET"))) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(fullUrl + "?" + parameterUrl))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.body();
        } else {
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(parameterUrl))
                    .uri(URI.create(imageServerInternalUrl + path))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.body();
        }
        } catch(Exception e){
            log.error("Error for url : " + fullUrl + " with parameters " + parameterUrl, e);
            throw new InvalidRequestException("Cannot generate thumb for " + fullUrl + " with " + parameterUrl);
        }
    }


    private static String makeParameterUrl(Map<String, Object> parameters) throws UnsupportedEncodingException {
        parameters = filterParameters(parameters);
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            //TODO!!!
//            if (it.getValue() instanceof Geometry) {
//                value = it.getValue().toText();
//            }
            if (entry.getValue() instanceof String) {
                value = URLEncoder.encode((String)entry.getValue(), "UTF-8");
            }
            joiner.add(entry.getKey()+"="+value);
        }
        return joiner.toString();
    }


    private static Map<String, Object> filterParameters(Map<String, Object> parameters) {
        return parameters.entrySet().stream().filter(it -> it.getValue() != null && !it.getValue().equals(""))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private static String checkFormat(String format, List<String> accepted) {
        if (accepted==null) {
            accepted = List.of("jpg");
        }
        return (!accepted.contains(format)) ? accepted.get(0) : format;
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return null;
    }

    @Override
    public Class currentDomain() {
        return null;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        return null;
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        return null;
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }
}
