package be.cytomine.service.middleware;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.*;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.InvalidRequestException;
import be.cytomine.repository.middleware.ImageServerRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.dto.*;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.StringUtils;
import be.cytomine.utils.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.HttpUtils.getContentFromUrl;
import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ImageServerService extends ModelService {

    private static final int GET_URL_MAX_LENGTH = 512;
    
    private final ImageInstanceService imageInstanceService;
    
    private final SecurityACLService securityACLService;

    private final ImageServerRepository imageServerRepository;

    private final CurrentUserService currentUserService;

    public ImageServer get(Long id) {
        securityACLService.checkGuest();
        return find(id).orElse(null);
    }

    public Optional<ImageServer> find(Long id) {
        securityACLService.checkGuest();
        return imageServerRepository.findById(id);
    }

    public List<ImageServer> list() {
        securityACLService.checkGuest();
        return imageServerRepository.findAll();
    }

    public Long storageSpace(ImageServer imageServer) throws IOException {
        return Long.parseLong(getContentFromUrl(imageServer.getInternalUrl() + "/storage/size.json"));
    }

    public String downloadUri(UploadedFile uploadedFile) throws IOException {
        if (uploadedFile.getPath()==null || uploadedFile.getPath().trim().equals("")) {
            throw new InvalidRequestException("Uploaded file has no valid path.");
        }
        return uploadedFile.getImageServer().getUrl() +"/image/download"
                + makeParameterUrl(Map.of("fif", uploadedFile.getPath(), "mimeType", uploadedFile.getContentType()));

    }

    public String downloadUri(AbstractImage abstractImage) throws IOException {
        return downloadUri(abstractImage.getUploadedFile());
    }

    public String downloadUri(CompanionFile companionFile) throws IOException {
        return downloadUri(companionFile.getUploadedFile());
    }

    public List<Map<String, Object>> properties(AbstractImage image) throws IOException {
        String server = retrieveImageServerInternalUrl(image);
        Map<String, Object> parameters = retrieveImageServerParameters(image);
        String content = getContentFromUrl(server + "/image/properties.json" + makeParameterUrl(parameters));
        return JsonObject.toObjectList(content);
    }

    public Map<String, Object> profile(AbstractImage image) throws IOException {
        String server = retrieveImageServerInternalUrl(image);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mimeType", image.getUploadedFile().getContentType());
        parameters.put("abstractImage", image.getId());
        parameters.put("uploadedFileParent", image.getUploadedFile().getId());
        parameters.put("user", currentUserService.getCurrentUser().getId());
        parameters.put("core", UrlApi.getServerUrl());

        String string = new String(makeRequest("POST", server, "/image/properties.json", parameters));
        return JsonObject.toObject(string);
    }

//TODO!!!!!!!
//    def profile(CompanionFile profile, AnnotationDomain annotation, def params) {
//        def (server, parameters) = imsParametersFromCompanionFile(profile)
//        parameters.location = annotation.location
//        parameters.minSlice = params.minSlice
//        parameters.maxSlice = params.maxSlice
//        return JSON.parse(new URL(makeGetUrl("/profile.json", server, parameters)).text)
//    }


    public Map<String, Object> associated(AbstractImage image) throws IOException {
        String server = retrieveImageServerInternalUrl(image);
        Map<String, Object> parameters = retrieveImageServerParameters(image);
        String content = getContentFromUrl(server + ("/image/associated.json"+ makeParameterUrl(parameters)));
        return JsonObject.toJsonObject(content);
    }

    public Map<String, Object> associated(ImageInstance image) throws IOException {
        return associated(image.getBaseImage());
    }


    public byte[]  label(ImageInstance image, LabelParameter params) {
        return label(image.getBaseImage(), params);
    }

    public byte[]  label(AbstractImage image, LabelParameter params) {
        String server = retrieveImageServerInternalUrl(image);
        Map<String, Object> parameters = retrieveImageServerParameters(image);
        String format = checkFormat(params.getFormat(), List.of("jpg", "png"));
        parameters.put("maxSize", params.getMaxSize());
        parameters.put("label", params.getLabel());
        return makeRequest(server, "/image/nested." + format, parameters);
    }

    public byte[] thumb(ImageInstance image, ImageParameter params)  {
        return thumb(imageInstanceService.getReferenceSlice(image), params);
    }

    public byte[] thumb(SliceInstance slice, ImageParameter params)  {
        return thumb(slice.getBaseSlice(), params);
    }

    public byte[] thumb(AbstractSlice slice, ImageParameter params) {
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

// TODO!!!!!!!!

//    def crop(AnnotationDomain annotation, def params, def urlOnly = false, def parametersOnly = false) {
//        params.geometry = annotation.location
//        crop(annotation.slice, params, urlOnly, parametersOnly)
//    }




    public byte[] crop(AbstractSlice slice, CropParameter cropParameter) throws UnsupportedEncodingException {
        String server = retrieveImageServerInternalUrl(slice);
        Map<String, Object> parameters = cropParameters(slice, cropParameter);
        String format;
        if (parameters.get("type").equals("alphaMask")) {
            format = checkFormat(cropParameter.getFormat(), List.of("png"));
        } else {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "tiff"));
        }
        return makeRequest(server, "/slice/crop." + format, parameters);
    }

    public String cropUrl(AbstractSlice slice, CropParameter cropParameter) throws UnsupportedEncodingException {
        String server = retrieveImageServerInternalUrl(slice);
        Map<String, Object> parameters = cropParameters(slice, cropParameter);
        String format;
        if (parameters.get("type").equals("alphaMask")) {
            format = checkFormat(cropParameter.getFormat(), List.of("png"));
        } else {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "tiff"));
        }
        return server + "/slice/crop." + format + makeParameterUrl(parameters);

    }

    public Map<String,Object> cropParameters(AbstractSlice slice, CropParameter cropParameter) {
        Map<String, Object> parameters = retrieveImageServerParameters(slice);
        
        String geometry = cropParameter.getGeometry();
        if (StringUtils.isBlank(cropParameter.getGeometry()) && StringUtils.isNotBlank(cropParameter.getLocation())) {
            geometry = new WKTReader().read(cropParameter.getLocation());
        }
        // In the window service, boundaries are already set and do not correspond to geometry/location boundaries
        BoundariesCropParameter boundaries = cropParameter.getBoundaries();
        if (boundaries==null && StringUtils.isNotBlank(geometry)) {
            boundaries = GeometryUtils.getGeometryBoundaries(geometry);
        }
        parameters.put("topLeftX", boundaries.getTopLeftX());
        parameters.put("topLeftY", boundaries.getTopLeftY());
        parameters.put("width", boundaries.getWidth());
        parameters.put("height", boundaries.getHeight());
                
        if (cropParameter.isComplete() && StringUtils.isNotBlank(geometry)) {
            parameters.put("location", simplifyGeometryService.reduceGeometryPrecision(geometry).toText());
        } else if (StringUtils.isNotBlank(geometry)) {
            parameters.put("location", simplifyGeometryService.simplifyPolygonForCrop(geometry);
        }

        parameters.put("imageWidth", slice.getImage().getWidth());
        parameters.put("imageHeight", slice.getImage().getHeight());
        parameters.put("maxSize", cropParameter.getMaxSize());
        parameters.put("zoom", (cropParameter.getMaxSize()!=null ? cropParameter.getZoom() : null));
        parameters.put("increaseArea", cropParameter.getIncreaseArea());
        parameters.put("safe", cropParameter.isSafe());
        parameters.put("square", cropParameter.isSquare());

        parameters.put("type", checkType(cropParameter, List.of("crop", "draw", "mask", "alphaMask")));



        parameters.put("drawScaleBar", cropParameter.isDrawScaleBar());
        parameters.put("resolution",(cropParameter.isDrawScaleBar()) ? cropParameter.getResolution() : null);
        parameters.put("magnification",(cropParameter.isDrawScaleBar()) ? cropParameter.getMagnification() : null);

        parameters.put("colormap", cropParameter.getColormap());
        parameters.put("inverse", cropParameter.isInverse());
        parameters.put("contrast", cropParameter.getContrast());
        parameters.put("gamma", cropParameter.getGamma());
        parameters.put("bits", cropParameter.getBits());
        parameters.put("alpha", cropParameter.getAlpha());
        parameters.put("thickness", cropParameter.getThickness());
        parameters.put("color", cropParameter.getColor());
        parameters.put("jpegQuality", cropParameter.getJpegQuality());

        return parameters;
    }


    public String windowUrl(AbstractSlice slice, WindowParameter params) throws UnsupportedEncodingException {
        return cropUrl(slice, extractCropParameter(slice, params));
    }

    public byte[] window(AbstractSlice slice, WindowParameter params) throws UnsupportedEncodingException {
        return crop(slice, extractCropParameter(slice, params));
    }


    private CropParameter extractCropParameter(AbstractSlice slice, WindowParameter params) {
        BoundariesCropParameter boundariesCropParameter = new BoundariesCropParameter();
        boundariesCropParameter.setTopLeftX(Math.max(params.getX(), 0));
        boundariesCropParameter.setTopLeftY(Math.max(params.getY(), 0));
        boundariesCropParameter.setWidth(params.getW());
        boundariesCropParameter.setHeight(params.getH());

        boolean withExterior = params.isWithExterior();
        if (!withExterior) {
            // Do not take part outside of the real image
            if(slice.getImage().getWidth()!=null && ((boundariesCropParameter.getWidth() + boundariesCropParameter.getTopLeftX()) > slice.getImage().getWidth())) {
                boundariesCropParameter.setWidth(slice.getImage().getWidth() - boundariesCropParameter.getTopLeftX());
            }
            if(slice.getImage().getHeight()!=null && (boundariesCropParameter.getHeight() + boundariesCropParameter.getTopLeftY()) > slice.getImage().getHeight()) {
                boundariesCropParameter.setHeight(slice.getImage().getHeight() - boundariesCropParameter.getTopLeftY());
            }
        }
        boundariesCropParameter.setTopLeftY(Math.max((int) (slice.getImage().getHeight() - boundariesCropParameter.getTopLeftY()), 0));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setBoundaries(boundariesCropParameter);
        return cropParameter;
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

    private static String checkType(CropParameter params, List<String> accepted) {
        if (params.getType()!=null && accepted.contains(params.getType())) {
            return params.getType();
        } else if (params.getDraw()) {
            return "draw";
        } else if (params.getMask()) {
            return "mask";
        } else if (params.getAlphaMask()) {
            return "alphaMask";
        } else {
            return "crop";
        }
    }



    private static String retrieveImageServerInternalUrl(AbstractImage image) {
        if (image.getPath()==null) {
            throw new InvalidRequestException("Abstract image has no valid path.");
        }

        return image.getImageServerInternalUrl();
    }

    private static Map<String, Object> retrieveImageServerParameters(AbstractImage image) {
        if (image.getPath()==null) {
            throw new InvalidRequestException("Abstract image has no valid path.");
        }
        return new HashMap<>(Map.of("fif", image.getPath(), "mimeType", image.getUploadedFile().getContentType()));
    }



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


    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        throw new CytomineMethodNotYetImplementedException("");
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new CytomineMethodNotYetImplementedException("");
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        throw new CytomineMethodNotYetImplementedException("");
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        throw new CytomineMethodNotYetImplementedException("");
    }

    @Override
    public Class currentDomain() {
        return ImageServer.class;
    }


}
