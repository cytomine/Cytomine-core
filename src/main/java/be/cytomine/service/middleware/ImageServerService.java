package be.cytomine.service.middleware;

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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.image.*;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.PimsResponse;
import be.cytomine.exceptions.*;
import be.cytomine.repository.middleware.ImageServerRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.dto.*;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.SimplifyGeometryService;
import be.cytomine.utils.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static be.cytomine.utils.HttpUtils.getContentFromUrl;

@Slf4j
@Service
@Transactional
public class ImageServerService extends ModelService {

    private static final int GET_URL_MAX_LENGTH = 512;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private ImageServerRepository imageServerRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SimplifyGeometryService simplifyGeometryService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    public void setImageInstanceService(ImageInstanceService imageInstanceService) {
        this.imageInstanceService = imageInstanceService;
    }

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

    public StorageStats storageSpace(ImageServer imageServer) throws IOException {
        return JsonObject.toObject(getContentFromUrl(imageServer.getInternalUrl() + "/storage/size.json"), StorageStats.class);
    }

    public List<Map<String, Object>> formats(ImageServer imageServer) throws IOException {
        log.debug(imageServer.getInternalUrl() + "/formats");
        String response = getContentFromUrl(imageServer.getInternalUrl() + "/formats");
        JsonObject jsonObject = JsonObject.toJsonObject(response);
        return ((List<Map<String,Object>>)jsonObject.get("items")).stream().map(x -> StringUtils.keysToCamelCase(x)).toList();
    }


    public String downloadUri(UploadedFile uploadedFile) throws IOException {
        if (uploadedFile.getPath()==null || uploadedFile.getPath().trim().equals("")) {
            throw new InvalidRequestException("Uploaded file has no valid path.");
        }
        // It gets the file specified in the uri.
        String uri = "/file/"+URLEncoder.encode(uploadedFile.getPath() ,StandardCharsets.UTF_8) +"/export";
        return uploadedFile.getImageServer().getUrl()+uri;

    }

    public String downloadUri(AbstractImage abstractImage) throws IOException {
        UploadedFile uploadedFile = abstractImage.getUploadedFile();
        if (uploadedFile.getPath()==null || uploadedFile.getPath().trim().equals("")) {
            throw new InvalidRequestException("Uploaded file has no valid path.");
        }
        // It gets the file specified in the uri.
        String uri = "/image/"+URLEncoder.encode(uploadedFile.getPath() ,StandardCharsets.UTF_8) +"/export";
        return uploadedFile.getImageServer().getUrl()+uri;
    }

    public String downloadUri(CompanionFile companionFile) throws IOException {
        return downloadUri(companionFile.getUploadedFile());
    }

    public Map<String, Object> properties(AbstractImage image) throws IOException {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String content = getContentFromUrl(server + "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/info");
        return JsonObject.toMap(content);
    }

    public List<Map<String, Object>> rawProperties(AbstractImage image) throws IOException {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String content = getContentFromUrl(server + "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/metadata");
        return JsonObject.toJsonObject(content).getJSONAttrListMap("items");
    }
    public Map<String, Object> imageHistogram(AbstractImage image, int nBins) {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-image";
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        return StringUtils.keysToCamelCase(json);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image) {
        return imageHistogramBounds(image, 256);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image, int nBins) {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-image/bounds";
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        return StringUtils.keysToCamelCase(json);
    }

    public List<Map<String, Object>> channelHistograms(AbstractImage image, int nBins) {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-channels";
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> channelHistogramBounds(AbstractImage image) {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-channels/bounds";
        PimsResponse pimsResponse = makeRequest("GET", server, uri, new LinkedHashMap<>(), "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();

    }


    public List<Map<String, Object>> planeHistograms(AbstractSlice slice, int nBins, boolean allChannels) {
        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-plane/z/" + slice.getZStack() + "/t/" + slice.getTime();
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        if (!allChannels) {
            params.put("channels", slice.getChannel());
        }
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> planeHistogramBounds(AbstractSlice slice, boolean allChannels) {
        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();
        String uri = "/image/"+URLEncoder.encode(path ,StandardCharsets.UTF_8)+"/histogram/per-plane/z/" + slice.getZStack() + "/t/" + slice.getTime() + "/bounds";
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        if (!allChannels) {
            params.put("channels", slice.getChannel());
        }
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }

    private String hmsInternalUrl() {
        String url = applicationProperties.getHyperspectralServerURL();
        return (applicationProperties.getUseHTTPInternally()) ? url.replace("https", "http") : url;
    }

    //TODO
    private LinkedHashMap<String, Object> hmsParametersFromCompanionFile(CompanionFile cf) {
        if (cf.getPath()==null) {
            throw new InvalidRequestException("Companion file has no valid path.");
        }
        LinkedHashMap parameters = new LinkedHashMap();
        parameters.put("fif", applicationProperties.getStoragePath() + "/" + cf.getPath());
        return parameters;
    }


    public void makeHDF5(Long imageId, Long companionFileId, Long uploadedFileId) {
        String server = hmsInternalUrl();
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("image", imageId);
        parameters.put("uploadedFile", uploadedFileId);
        parameters.put("companionFile", companionFileId);
        parameters.put("core", UrlApi.getServerUrl());
        makeRequest("POST", server, "/hdf5.json", parameters, "json", Map.of(), true);
    }

    //TODO
    public Map<String, Object> profile(CompanionFile profileCf, AnnotationDomain annotation, Map<String, String> params) {
        return profile(profileCf, annotation.getLocation(), params);
    }

    //TODO
    public Map<String, Object> profile(CompanionFile profile, Geometry geometry, Map<String, String> params) {
        String server = hmsInternalUrl();
        LinkedHashMap<String, Object> parameters = hmsParametersFromCompanionFile(profile);
        parameters.put("location", geometry.toString());
        parameters.put("minSlice", params.get("minSlice"));
        parameters.put("maxSlice", params.get("maxSlice"));
        PimsResponse response = makeRequest(null, server, "/profile.json", parameters, "json", Map.of(), true);
        return JsonObject.toMap(new String(response.getContent()));
    }

    //TODO
    public Map<String, Object> profileProjections(CompanionFile profile, AnnotationDomain annotation, LinkedHashMap<String, String> params) {
        return profileProjections(profile, annotation.getLocation(), params);
    }

    //TODO
    public Map<String, Object> profileProjections(CompanionFile profile, Geometry geometry, LinkedHashMap<String, String> params) {
        String server = hmsInternalUrl();
        LinkedHashMap<String, Object> parameters = hmsParametersFromCompanionFile(profile);
        parameters.put("location", geometry.toString());
        parameters.put("minSlice", params.get("minSlice"));
        parameters.put("maxSlice", params.get("maxSlice"));
        parameters.put("axis", params.get("axis"));
        parameters.put("dimension", params.get("dimension"));
        PimsResponse response = makeRequest(null, server, "/profile/projections.json", parameters, "json", Map.of(), true);
        return JsonObject.toMap(new String(response.getContent()));
    }

    //TODO
    public Map<String, Object> profileImageProjection(CompanionFile profile, AnnotationDomain annotation, LinkedHashMap<String, String> params) {
        return profileImageProjection(profile, annotation.getLocation(), params);
    }

    //TODO
    public Map<String, Object> profileImageProjection(CompanionFile profile, Geometry geometry, LinkedHashMap<String, String> params) {
        String server = hmsInternalUrl();
        String format = checkFormat((String) params.get("format"), List.of("jpg", "png"));

        LinkedHashMap<String, Object> parameters = hmsParametersFromCompanionFile(profile);
        parameters.put("location", geometry.toString());
        parameters.put("minSlice", params.get("minSlice"));
        parameters.put("maxSlice", params.get("maxSlice"));

        PimsResponse response = makeRequest(null, server, "/profile/" + params.get("projection") + "-projection." +format, parameters, format, Map.of(), true);
        return JsonObject.toMap(new String(response.getContent()));
    }



    public List<String> associated(AbstractImage image) throws IOException {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String content = getContentFromUrl(server + ("/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/info/associated"));
        return JsonObject.toJsonObject(content).getJSONAttrListMap("items").stream().map(x -> (String)x.get("name")).toList();
    }

    public List<String> associated(ImageInstance image) throws IOException {
        return associated(image.getBaseImage());
    }



    public PimsResponse  label(ImageInstance image, LabelParameter params, String etag) {
        return label(image.getBaseImage(), params, etag);
    }

    public PimsResponse label(AbstractImage image, LabelParameter params, String etag) {
        String server = image.getImageServerInternalUrl();
        String path = image.getPath();
        String format = checkFormat(params.getFormat(), List.of("jpg", "png", "webp"));

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("length", params.getMaxSize());

        String uri = "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/associated/" + params.getLabel().toLowerCase();

        Map<String, Object> headers = new LinkedHashMap<>();
        if (etag!=null) {
            headers.put("If-None-Match", etag);
        }
        return makeRequest("GET", server, uri, parameters, format, headers);
    }

    public PimsResponse thumb(ImageInstance image, ImageParameter params, String etag)  {
        return thumb(imageInstanceService.getReferenceSlice(image), params, etag);
    }

    public PimsResponse thumb(SliceInstance slice, ImageParameter params, String etag)  {
        return thumb(slice.getBaseSlice(), params, etag);
    }

    public PimsResponse thumb(AbstractSlice slice, ImageParameter params, String etag) {

        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();

        String format = checkFormat(params.getFormat(), List.of("jpg", "png", "webp"));
        String uri = "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/thumb";

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            parameters.put("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        parameters.put("z_slices", slice.getZStack());
        parameters.put("timepoints", slice.getTime());

        parameters.put("length", params.getMaxSize());
        parameters.put("gammas", params.getGamma());
        if (params.getColormap()!=null) {
            parameters.put("colormaps", Arrays.stream(params.getColormap().split(",")).toList());
        }

        if (params.getBits()!=null) {
            parameters.put("bits", params.getMaxBits() ? "AUTO" : params.getBits());
            uri = "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/resized";
        }

        if (params.getInverse()!=null && params.getInverse()) {
            if (parameters.containsKey("colormaps")) {
                parameters.put("colormaps", ((List<String>)parameters.get("colormaps")).stream().map(x -> invertColormap(x)).toList());
            }
            else {
                parameters.put("colormaps", "!DEFAULT");
            }
        }

        Map<String, Object> headers = new LinkedHashMap<>();
        if (etag!=null) {
            headers.put("If-None-Match", etag);
        }
        return makeRequest("GET", server, uri, parameters, format, headers);
    }

//    public PimsResponse thumb(String server, String path, ImageParameter params, String etag) {
//
//    }



    public PimsResponse crop(AnnotationDomain annotation, CropParameter params, String etag) throws UnsupportedEncodingException, ParseException {
        params.setLocation(annotation.getWktLocation());
        return crop(annotation.getSlice().getBaseSlice(), params, etag);
    }
//
//    public PimsResponse crop(ImageInstance image, CropParameter params, String etag) throws UnsupportedEncodingException, ParseException {
//        return crop(image.getBaseImage(), params, etag);
//    }
//
//    public PimsResponse crop(AbstractImage image, CropParameter cropParameter, String etag) throws UnsupportedEncodingException, ParseException {
//        String server = image.getImageServerInternalUrl();
//        String path = image.getPath();
//
//        String cropUrl = cropUrl(path, cropParameter);
//        LinkedHashMap<String,Object> parameters = cropParameters(cropParameter);
//
//        String format = retrieveCropFormat(cropParameter);
//
//        LinkedHashMap<String, Object> headers = retrieveHeaders(cropParameter, etag);
//
//        if (cropParameter.getInverse()) {
//            if (parameters.containsKey("colormaps")) {
//                parameters.put("colormaps", ((List<String>)parameters.get("colormaps")).stream().map(x -> invertColormap(x)).toList());
//            }
//            else {
//                parameters.put("colormaps", "!DEFAULT");
//            }
//        }
//        return makeRequest("POST", server, cropUrl, parameters, format, headers);
//    }
    public PimsResponse crop(SliceInstance slice, CropParameter params, String etag) throws UnsupportedEncodingException, ParseException {
        return crop(slice.getBaseSlice(), params, etag);
    }

    public PimsResponse crop(AbstractSlice slice, CropParameter cropParameter, String etag) throws UnsupportedEncodingException, ParseException {

        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();
        String cropUrl = cropUrl(path, cropParameter);
        LinkedHashMap<String,Object> parameters = cropParameters(cropParameter);

        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            parameters.put("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        parameters.put("z_slices", slice.getZStack());
        parameters.put("timepoints", slice.getTime());

        String format = retrieveCropFormat(cropParameter);

        LinkedHashMap<String, Object> headers = retrieveHeaders(cropParameter, etag);

        if (cropParameter.getInverse()!=null && cropParameter.getInverse()) {
            if (parameters.containsKey("colormaps")) {
                parameters.put("colormaps", ((List<String>)parameters.get("colormaps")).stream().map(x -> invertColormap(x)).toList());
            }
            else {
                parameters.put("colormaps", "!DEFAULT");
            }
        }

        return makeRequest("POST", server, cropUrl, parameters, format, headers);
    }


    private String retrieveCropFormat(CropParameter cropParameter) {
        String format;
        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            format = checkFormat(cropParameter.getFormat(), List.of("png", "webp"));
        } else {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        }
        return format;
    }

    private LinkedHashMap<String, Object> retrieveHeaders(CropParameter cropParameter, String etag) {
        LinkedHashMap<String, Object> headers = new LinkedHashMap<>(Map.of("X-Annotation-Origin", "LEFT_BOTTOM"));
        if (cropParameter.getSafe()!=null && cropParameter.getSafe()) {
            headers.put("X-Image-Size-Safety", "SAFE_RESIZE");
        }
        if (etag!=null) {
            headers.put("If-None-Match", etag);
        }
        return headers;
    }

    public String cropUrl(String filePath, CropParameter cropParameter) throws UnsupportedEncodingException, ParseException {
//        LinkedHashMap<String, Object> parameters = cropParameters(cropParameter);
        String uri;
        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            uri = "/image/"+URLEncoder.encode(filePath, StandardCharsets.UTF_8)+"/annotation/drawing";
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            uri = "/image/"+URLEncoder.encode(filePath, StandardCharsets.UTF_8)+"/annotation/mask";
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            uri = "/image/"+URLEncoder.encode(filePath, StandardCharsets.UTF_8)+"/annotation/crop";
        } else {
            uri = "/image/"+URLEncoder.encode(filePath, StandardCharsets.UTF_8)+"/annotation/crop";
        }
        return  uri;
    }

    public LinkedHashMap<String,Object> cropParameters(AnnotationDomain annotationDomain, CropParameter cropParameter) throws ParseException {
        cropParameter.setLocation(annotationDomain.getWktLocation());
        return cropParameters(cropParameter);
    }

    public LinkedHashMap<String,Object> cropParameters(CropParameter cropParameter) throws ParseException {

        Object geometry = cropParameter.getGeometry();
        if (geometry!=null && geometry instanceof String) {
            geometry = new WKTReader().read((String)geometry);
        }

        if (StringUtils.isBlank(cropParameter.getGeometry()) && StringUtils.isNotBlank(cropParameter.getLocation())) {
            geometry = new WKTReader().read(cropParameter.getLocation());
        }
        String wkt = null;
        if (cropParameter.getComplete()!=null && cropParameter.getComplete() && geometry!=null) {
            wkt = simplifyGeometryService.reduceGeometryPrecision((Geometry)geometry).toText();
        } else if (geometry!=null) {
            wkt = simplifyGeometryService.simplifyPolygonForCrop((Geometry)geometry).toText();
        }

        LinkedHashMap<String, Object> pimsParameter = new LinkedHashMap<>();
        pimsParameter.put("length", cropParameter.getMaxSize());
        pimsParameter.put("context_factor", cropParameter.getIncreaseArea());
        pimsParameter.put("gammas", cropParameter.getGamma());
        pimsParameter.put("annotations", (wkt!=null? new LinkedHashMap<>(Map.of("geometry", wkt)) :Map.of()));

        if (cropParameter.getColormap()!=null) {
            pimsParameter.put("colormaps", Arrays.stream(cropParameter.getColormap().split(",")).toList());
        }

        if (cropParameter.getMaxSize()==null) {
            pimsParameter.put("level", cropParameter.getZoom()!=null ? cropParameter.getZoom() : 0);
        }

        if (cropParameter.getBits()!=null) {
            pimsParameter.put("bits", cropParameter.getMaxBits() ? "AUTO" : cropParameter.getBits());
        }


        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            pimsParameter.put("try_square", cropParameter.getSquare());
            ((Map<String, Object>)pimsParameter.get("annotations")).put("stroke_color", cropParameter.getColor()!=null? cropParameter.getColor().replace("0x", "#") : null);
            ((Map<String, Object>)pimsParameter.get("annotations")).put("stroke_width", cropParameter.getThickness());
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            ((Map<String, Object>)pimsParameter.get("annotations")).put("fill_color", "#fff");
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            pimsParameter.put("background_transparency", cropParameter.getAlpha() != null ? cropParameter.getAlpha() : 100);
        } else {
            pimsParameter.put("background_transparency", 0);
        }

        pimsParameter.put("contrast", cropParameter.getContrast());
        pimsParameter.put("jpegQuality", cropParameter.getJpegQuality());

        //pimsParameter.put("annotations", JsonObject.toJsonString(pimsParameter.get("annotations")));

        return pimsParameter;
    }

//    public PimsResponse window(ImageInstance image, WindowParameter params, String etag) throws UnsupportedEncodingException, ParseException {
//        return window(image.getBaseImage(), params, etag);
//    }
//
//    public PimsResponse window(AbstractImage image, WindowParameter windowParameter, String etag) throws UnsupportedEncodingException, ParseException {
//        String server = image.getImageServerInternalUrl();
//        String path = image.getPath();
//        LinkedHashMap<String, Object> parameters = windowParameters(windowParameter);
//        String format;
//
//        if (checkType(windowParameter).equals("alphamask")) {
//            format = checkFormat(windowParameter.getFormat(), List.of("png", "webp"));
//        } else {
//            format = checkFormat(windowParameter.getFormat(), List.of("png", "jpg", "webp"));
//        }
//
//        LinkedHashMap<String, Object> headers = new LinkedHashMap<>(Map.of("X-Annotation-Origin", "LEFT_BOTTOM"));
//        if (windowParameter.getSafe()) {
//            headers.put("X-Image-Size-Safety", "SAFE_RESIZE");
//        }
//        if (etag!=null) {
//            headers.put("If-None-Match", etag);
//        }
//
//        return makeRequest("POST", server, path, parameters, format, headers);
//    }
//
//    public PimsResponse window(SliceInstance slice, WindowParameter params, String etag) throws UnsupportedEncodingException, ParseException {
//        return window(slice.getBaseSlice(), params, etag);
//    }

    public PimsResponse window(AbstractSlice slice, WindowParameter windowParameter, String etag) throws UnsupportedEncodingException, ParseException {
        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();

        LinkedHashMap<String, Object> parameters = windowParameters(windowParameter);

        String uri = "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/window";

        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            parameters.put("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        parameters.put("z_slices", slice.getZStack());
        parameters.put("timepoints", slice.getTime());

        String format;

        if (checkType(windowParameter).equals("alphamask")) {
            format = checkFormat(windowParameter.getFormat(), List.of("png", "webp"));
        } else {
            format = checkFormat(windowParameter.getFormat(), List.of("png", "jpg", "webp"));
        }

        LinkedHashMap<String, Object> headers = new LinkedHashMap<>(Map.of("X-Annotation-Origin", "LEFT_BOTTOM"));
        if (windowParameter.getSafe()!=null && windowParameter.getSafe()) {
            headers.put("X-Image-Size-Safety", "SAFE_RESIZE");
        }
        if (etag!=null) {
            headers.put("If-None-Match", etag);
        }

        return makeRequest("POST", server, uri, parameters, format, headers);
    }

    public String windowUrl(AbstractSlice slice, WindowParameter windowParameter) throws UnsupportedEncodingException, ParseException {
        String server = slice.getImageServerInternalUrl();
        String path = slice.getPath();

        LinkedHashMap<String, Object> parameters = windowParameters(windowParameter);

        String uri = "/image/"+URLEncoder.encode(path, StandardCharsets.UTF_8)+"/window";
        return server + uri + "?" + makeParameterUrl(parameters);
    }

    public LinkedHashMap<String,Object> windowParameters(WindowParameter windowParameter) throws ParseException {


        LinkedHashMap<String, Object> pimsParameter = new LinkedHashMap<>();

        LinkedHashMap<String, Object> region = new LinkedHashMap<>();
        region.put("left", windowParameter.getX());
        region.put("top", windowParameter.getY());
        region.put("width", windowParameter.getW());
        region.put("height", windowParameter.getH());

        pimsParameter.put("region", region);
        pimsParameter.put("length", windowParameter.getMaxSize());
        pimsParameter.put("gammas", windowParameter.getGamma());

        if (windowParameter.getColormap()!=null) {
            pimsParameter.put("colormaps", Arrays.stream(windowParameter.getColormap().split(",")).toList());
        }

        if (windowParameter.getMaxSize()==null) {
            pimsParameter.put("level", windowParameter.getZoom()!=null ? windowParameter.getZoom() : 0);
        }

        if (windowParameter.getBits()!=null) {
            pimsParameter.put("bits", windowParameter.getMaxBits() ? "AUTO" : windowParameter.getBits());
        }


        if (windowParameter.getInverse()!=null && windowParameter.getInverse()) {
            if (pimsParameter.containsKey("colormaps")) {
                pimsParameter.put("colormaps", ((List<String>)pimsParameter.get("colormaps")).stream().map(x -> invertColormap(x)).toList());
            }
            else {
                pimsParameter.put("colormaps", "!DEFAULT");
            }
        }

        if (windowParameter.getGeometries()!=null) {
            String strokeColor = windowParameter.getColor()!=null? windowParameter.getColor().replace("0x", "#") : "black";
            Integer strokeWidth = windowParameter.getThickness()!=null? windowParameter.getThickness() : 1; // TODO: check scale
            String annotationType = checkType(windowParameter);


            List<Map<String, Object>> annotations = (List<Map<String, Object>>) pimsParameter.get("annotations");
            List<Map<String, Object>> annotationsResults = new ArrayList<>();
            for (Map<String, Object> geometry : annotations) {
                String wkt = null;
                if (windowParameter.getComplete()!=null && windowParameter.getComplete() && geometry!=null) {
                    wkt = simplifyGeometryService.reduceGeometryPrecision((Geometry)geometry).toText();
                } else if (geometry!=null) {
                    wkt = simplifyGeometryService.simplifyPolygonForCrop((Geometry)geometry).toText();
                }
                Map<String, Object> annot = new LinkedHashMap<>(wkt!=null? Map.of("geometry", wkt) : Map.of());

                if (annotationType.equals("draw")) {
                    annot.put("stroke_color", strokeColor);
                    annot.put("stroke_width", strokeWidth);
                } else if (annotationType.equals("mask")) {
                    annot.put("fill_color", "#fff");
                }
                annotationsResults.add(annot);
            }

            Map<String, Object> annotationStyle = new HashMap<>();
            switch (annotationType) {
                case "draw":
                    annotationStyle.put("mode","DRAWING");
                    break;
                case "mask":
                    annotationStyle.put("mode","MASK");
                    break;
                case "alphaMask":
                case "alphamask":
                    annotationStyle.put("mode","CROP");
                    annotationStyle.put("background_transparency", windowParameter.getAlpha() != null ? windowParameter.getAlpha() : 100);
                    break;
                default:
                    annotationStyle.put("mode","CROP");
                    annotationStyle.put("background_transparency", 0);
            }
            pimsParameter.put("annotation_style", annotationStyle);

        }
        return pimsParameter;
    }

//    private CropParameter extractCropParameter(AbstractSlice slice, WindowParameter params) {
//        BoundariesCropParameter boundariesCropParameter = new BoundariesCropParameter();
//        boundariesCropParameter.setTopLeftX(Math.max(params.getX(), 0));
//        boundariesCropParameter.setTopLeftY(Math.max(params.getY(), 0));
//        boundariesCropParameter.setWidth(params.getW());
//        boundariesCropParameter.setHeight(params.getH());
//
//        boolean withExterior = params.isWithExterior();
//        if (!withExterior) {
//            // Do not take part outside of the real image
//            if(slice.getImage().getWidth()!=null && ((boundariesCropParameter.getWidth() + boundariesCropParameter.getTopLeftX()) > slice.getImage().getWidth())) {
//                boundariesCropParameter.setWidth(slice.getImage().getWidth() - boundariesCropParameter.getTopLeftX());
//            }
//            if(slice.getImage().getHeight()!=null && (boundariesCropParameter.getHeight() + boundariesCropParameter.getTopLeftY()) > slice.getImage().getHeight()) {
//                boundariesCropParameter.setHeight(slice.getImage().getHeight() - boundariesCropParameter.getTopLeftY());
//            }
//        }
//        boundariesCropParameter.setTopLeftY(Math.max((int) (slice.getImage().getHeight() - boundariesCropParameter.getTopLeftY()), 0));
//
//        CropParameter cropParameter = new CropParameter();
//        cropParameter.setBoundaries(boundariesCropParameter);
//        cropParameter.setFormat(params.getFormat());
//        return cropParameter;
//    }


    private static Map<String, String> extractPIMSHeaders(HttpHeaders headers) {
        List<String> names = List.of("Cache-Control", "ETag", "X-Annotation-Origin", "X-Image-Size-Limit", "Content-Type");
        Map<String, String> extractedHeaders = new LinkedHashMap<>();
        for (String name : names) {
            String value = headers.firstValue(name).isEmpty() ? headers.firstValue(name.toLowerCase()).orElse(null) : headers.firstValue(name).get();
            if (value!=null) {
                extractedHeaders.put(name, value); //h.getValue()?)
            }
        }
        return extractedHeaders;
    }

    private PimsResponse makeRequest(String httpMethod, String imageServerInternalUrl, String path, LinkedHashMap<String, Object> parameters, String format, Map<String, Object> headers) {
        return makeRequest(httpMethod, imageServerInternalUrl, path, parameters, format, headers, false);
    }

    private PimsResponse makeRequest(String httpMethod, String imageServerInternalUrl, String path, LinkedHashMap<String, Object> parameters, String format, Map<String, Object> headers, boolean hms)  {

        parameters = filterParameters(parameters);
        String parameterUrl = "";
        String fullUrl = "";

        String responseContentType = formatToContentType(format);

        try {
            parameterUrl = makeParameterUrl(parameters);
            fullUrl = imageServerInternalUrl + path + "?" + parameterUrl;
            log.debug(fullUrl);
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            if ((fullUrl).length() < GET_URL_MAX_LENGTH && (httpMethod==null || httpMethod.equals("GET"))) {
                log.debug("GET " + fullUrl);
                requestBuilder.GET()
                        .uri(URI.create(fullUrl));
            } else {
                log.debug("POST " + imageServerInternalUrl + path);
                log.debug(JsonObject.toJsonString(parameters));
                String requestContentType = "application/json";
                if (hms) {
                    requestContentType = "application/x-www-form-urlencoded";
                }

                HttpRequest.BodyPublisher bodyPublisher;
                if(hms) {
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(StringUtils.urlEncodeUTF8(parameters));
                } else {
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(JsonObject.toJsonString(parameters));
                }

                requestBuilder.POST(bodyPublisher)
                        .uri(URI.create(imageServerInternalUrl + path))
                        .setHeader("content-type", requestContentType);
            }
            requestBuilder.setHeader("Accept", responseContentType);
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                requestBuilder.setHeader(entry.getKey(), (String) entry.getValue());
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return processResponse(fullUrl, responseContentType, response);
        } catch(NotModifiedException e){
            throw e;
        } catch(Exception e){
            log.error("Error for url : " + fullUrl + " with parameters " + parameterUrl, e);
            throw new InvalidRequestException("Cannot generate thumb for " + fullUrl + " with " + parameterUrl);
        }
    }

    private PimsResponse processResponse(String fullUrl, String responseContentType, HttpResponse<byte[]> response) {
        if (response.statusCode()==200) {
            return new PimsResponse(response.body(), extractPIMSHeaders(response.headers()));
        } else  if (response.statusCode()==304) {
            throw new NotModifiedException(extractPIMSHeaders(response.headers()));
        } else  if (response.statusCode()==400) {
            throw new InvalidRequestException(fullUrl + " returned a 400 bad request");
        } else  if (response.statusCode()==404) {
            throw new ObjectNotFoundException(fullUrl + " returned a 404 not found");
        } else  if (response.statusCode()==422) {
            throw new InvalidRequestException(fullUrl + " returned a 422");
        } else  if (response.statusCode()==422) {
            throw new ServerException(fullUrl + " returned a 500");
        }
        throw new ServerException(fullUrl + " returned a " + response.statusCode() + ". Cannot catch this.");
    }


    private static String makeParameterUrl(LinkedHashMap<String, Object> parameters) throws UnsupportedEncodingException {
        parameters = filterParameters(parameters);
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            //TODO!!!
//            if (it.getValue() instanceof Geometry) {
//                value = it.getValue().toText();
//            }
            if (entry.getValue() instanceof String) {
                value = URLEncoder.encode((String)entry.getValue(), StandardCharsets.UTF_8);
            } else if (entry.getValue() instanceof Map<?,?>) {
                value = URLEncoder.encode(JsonObject.toJsonString(entry.getValue()), StandardCharsets.UTF_8);
            }
            joiner.add(entry.getKey()+"="+value);
        }
        return joiner.toString();
    }


    private static LinkedHashMap<String, Object> filterParameters(LinkedHashMap<String, Object> parameters) {
        LinkedHashMap<String, Object> processed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue()!=null && !entry.getValue().toString().equals("")) {
                processed.put(entry.getKey(), entry.getValue());
            }
        }
        return processed;
    }

    private static String formatToContentType(String format) {
        return switch (format) {
            case "png"->"image/png";
            case "webp"->"image/webp";
            case "jpg"->"image/jpeg";
            default -> "application/json";
        };
    }


    private static String checkFormat(String format, List<String> accepted) {
        if (accepted==null) {
            accepted = List.of("jpg");
        }
        if (format==null) {
            throw new WrongArgumentException("Format must be specified");
        }
        return (!accepted.contains(format)) ? accepted.get(0) : format;
    }

//    private static String checkType(CropParameter params, List<String> accepted) {
//        if (params.getType()!=null && accepted.contains(params.getType())) {
//            return params.getType();
//        } else if (params.getDraw()!=null && params.getDraw()) {
//            return "draw";
//        } else if (params.getMask()!=null && params.getMask()) {
//            return "mask";
//        } else if (params.getAlphaMask()!=null && params.getAlphaMask()) {
//            return "alphaMask";
//        } else {
//            return "crop";
//        }
//    }
//
//
//    String checkType(CropParameter params) {
//        if (params.getDraw() || params.getType().equals("draw"))
//            return "draw";
//        else if (params.getMask() || params.getType().equals("mask"))
//            return "mask";
//        else if (params.getAlphaMask() || params.getType().equals("alphaMask")
//             || params.getType().equals("alphamask")) {
//            return "alphaMask";
//        } else {
//            return "crop";
//        }
//    }

    String checkType(WindowParameter params) {
        if ((params.getDraw()!=null && params.getDraw()) || (params.getType()!=null && params.getType().equals("draw")))
            return "draw";
        else if ((params.getMask()!=null && params.getMask()) || (params.getType()!=null && params.getType().equals("mask")))
            return "mask";
        else if ((params.getAlphaMask()!=null && params.getAlphaMask()) || (params.getType()!=null && (params.getType().equals("alphaMask") || params.getType().equals("alphamask")))) {
            return "alphaMask";
        } else {
            return "crop";
        }
    }

    private static Map<String, Object> renameChannelHistogramKeys(Map<String, Object> hist) {
        Object channel = hist.get("concreteChannel");
        Object apparentChannel = hist.get("channel");
        hist.put("channel",channel);
        hist.put("apparentChannel", apparentChannel);
        hist.remove("concreteChannel");
        return hist;
    }

    private static String invertColormap(String colormap) {
        if (colormap.charAt(0) == '!') {
            return colormap.substring(1);
        }
        return '!' + colormap;
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