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
import be.cytomine.domain.image.*;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.PimsResponse;
import be.cytomine.exceptions.*;
import be.cytomine.service.dto.*;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.utils.SimplifyGeometryService;
import be.cytomine.utils.*;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class ImageServerService {

    private static final int GET_URL_MAX_LENGTH = 512;

    @Value("${application.internalImageServerURL}")
    String internalImageServerURL;


    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private SimplifyGeometryService simplifyGeometryService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    public void setImageInstanceService(ImageInstanceService imageInstanceService) {
        this.imageInstanceService = imageInstanceService;
    }

    public StorageStats storageSpace() throws IOException {
        return JsonObject.toObject(getContentFromUrl(internalImageServerURL + "/storage/size.json"), StorageStats.class);
    }

    public List<Map<String, Object>> formats() throws IOException {
        String response = getContentFromUrl(internalImageServerURL + "/formats");
        JsonObject jsonObject = JsonObject.toJsonObject(response);
        return ((List<Map<String,Object>>)jsonObject.get("items")).stream().map(x -> StringUtils.keysToCamelCase(x)).toList();
    }

    /**
     * Build a pims url path with valid escaping
     * @param targetResource The prefix resource prepend to the URI path (e.g. 'file', 'image')
     * @param imsPath The pims path of the resource (a relative file path to the pims file)
     * @param pathSuffix The suffix resource to append to the URI path  
     * @return '/{targetResource}/{path}/{pathSuffix}' but done smartly to avoid encoding issues and 
     */
    protected String buildEncodedUri(String targetResource, String imsPath, String pathSuffix) {
        if (imsPath == null || imsPath.trim().equals("")) {
            throw new InvalidRequestException("Uploaded file has no valid path.");
        }
        
        // strip "/" (to avoid double slash) and encode 
        targetResource = org.apache.commons.lang3.StringUtils.strip(targetResource, "/");
        pathSuffix = org.apache.commons.lang3.StringUtils.strip(pathSuffix, "/");
        
        // Apache reverse proxy does not support '%2F' encoding inside a path
        // whereas pims supports both '/' and '%2F'. Therefore, we revert the 
        // encoding of the `/` to support routing through an Apache proxy.
        // see issue cm/rnd/cytomine/core/core-ce#84
        String encodedPath = URLEncoder.encode(imsPath ,StandardCharsets.UTF_8).replace("%2F", "/");
        encodedPath = org.apache.commons.lang3.StringUtils.strip(encodedPath, "/");

        return "/" + targetResource + "/" + encodedPath + "/" + pathSuffix;
    }

    protected String buildEncodedUri(String targetResource, UploadedFile uploadedFile, String pathSuffix) {
        return this.buildEncodedUri(targetResource, uploadedFile.getPath(), pathSuffix);
    }

    protected String buildEncodedUri(String targetResource, AbstractImage abstractImage, String pathSuffix) {
        return this.buildEncodedUri(targetResource, abstractImage.getPath(), pathSuffix);
    }

    protected String buildEncodedUri(String targetResource, AbstractSlice abstractSlice, String pathSuffix) {
        return this.buildEncodedUri(targetResource, abstractSlice.getPath(), pathSuffix);
    }

    public String buildImageServerFullUrl(UploadedFile uploadedFile, String targetResource, String pathSuffix) {
        return this.internalImageServerURL + this.buildEncodedUri(targetResource, uploadedFile, pathSuffix);
    }

    public String buildImageServerFullUrl(AbstractImage abstractImage, String targetResource, String pathSuffix) {
        return this.internalImageServerURL + this.buildEncodedUri(targetResource, abstractImage, pathSuffix);
    }


    public String buildImageServerInternalFullUrl(AbstractImage abstractImage, String targetResource, String pathSuffix) {
        return this.internalImageServerURL + this.buildEncodedUri(targetResource, abstractImage, pathSuffix);
    }

    public ResponseEntity<byte[]> download(UploadedFile uploadedFile, ProxyExchange<byte[]> proxy) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL);
        request.addPathFragment("file");
        request.addPathFragment(uploadedFile.getPath(), true);
        request.addPathFragment("export");
        request.addQueryParameter("filename", uploadedFile.getOriginalFilename());
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> download(AbstractImage abstractImage, ProxyExchange<byte[]> proxy) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL);
        request.addPathFragment("image");
        request.addPathFragment(abstractImage.getPath(), true);
        request.addPathFragment("export");
        request.addQueryParameter("filename", abstractImage.getOriginalFilename());
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> download(CompanionFile companionFile, ProxyExchange<byte[]> proxy) throws IOException {
        return download(companionFile.getUploadedFile(), proxy);
    }

    public Map<String, Object> properties(AbstractImage image) throws IOException {
        String fullUrl = this.buildImageServerInternalFullUrl(image, "image", "/info");
        return JsonObject.toMap(getContentFromUrl(fullUrl));
    }

    public List<Map<String, Object>> rawProperties(AbstractImage image) throws IOException {
        String fullUrl = this.buildImageServerInternalFullUrl(image, "image", "/metadata");
        return JsonObject.toJsonObject(getContentFromUrl(fullUrl))
                .getJSONAttrListMap("items").stream().map(StringUtils::keysToCamelCase).toList();
    }

    public List<Map<String, Object>> rawProperties(ImageInstance image) throws IOException {
        return this.rawProperties(image.getBaseImage());
    }

    public Map<String, Object> imageHistogram(AbstractImage image, int nBins) {
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", image, "/histogram/per-image");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        return StringUtils.keysToCamelCase(json);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image) {
        return imageHistogramBounds(image, 256);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image, int nBins) {
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", image, "/histogram/per-image/bounds");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        return StringUtils.keysToCamelCase(json);
    }

    public List<Map<String, Object>> channelHistograms(AbstractImage image, int nBins) {
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", image, "/histogram/per-channels");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(Map.of("n_bins", nBins));
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> channelHistogramBounds(AbstractImage image) {
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", image, "/histogram/per-channels/bounds");
        PimsResponse pimsResponse = makeRequest("GET", server, uri, new LinkedHashMap<>(), "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> planeHistograms(AbstractSlice slice, int nBins, boolean allChannels) {
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", slice, "/histogram/per-plane/z/" + slice.getZStack() + "/t/" + slice.getTime());
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
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", slice, "/histogram/per-plane/z/" + slice.getZStack() + "/t/" + slice.getTime() + "/bounds");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        if (!allChannels) {
            params.put("channels", slice.getChannel());
        }
        PimsResponse pimsResponse = makeRequest("GET", server, uri, params, "json", Map.of());
        Map<String, Object> json = JsonObject.toMap(new String(pimsResponse.getContent()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }

    public List<String> associated(AbstractImage image) throws IOException {
        String fullUrl = this.buildImageServerInternalFullUrl(image, "image", "/info/associated");
        return JsonObject.toJsonObject(getContentFromUrl(fullUrl)).getJSONAttrListMap("items").stream().map(x -> (String)x.get("name")).toList();
    }

    public List<String> associated(ImageInstance image) throws IOException {
        return associated(image.getBaseImage());
    }

    public PimsResponse  label(ImageInstance image, LabelParameter params, String etag) {
        return label(image.getBaseImage(), params, etag);
    }

    public PimsResponse label(AbstractImage image, LabelParameter params, String etag) {
        String server = this.internalImageServerURL;
        String uri = buildEncodedUri("image", image, "/associated/" + params.getLabel().toLowerCase());
        String format = checkFormat(params.getFormat(), List.of("jpg", "png", "webp"));

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("length", params.getMaxSize());

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
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", slice, "/thumb");
        String format = checkFormat(params.getFormat(), List.of("jpg", "png", "webp"));
        
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
            uri = this.buildEncodedUri("image", slice, "/resized");
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

    public ResponseEntity<byte[]> normalizedTile(SliceInstance slice, TileParameters params, String etag, ProxyExchange<byte[]> proxy)  {
        return normalizedTile(slice.getBaseSlice(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> normalizedTile(AbstractSlice slice, TileParameters params, String etag, ProxyExchange<byte[]> proxy) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL);

        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment("normalized-tile");
        request.addPathFragment("zoom");
        request.addPathFragment(params.getZoom().toString());
        request.addPathFragment("tx");
        request.addPathFragment(params.getTx().toString());
        request.addPathFragment("ty");
        request.addPathFragment(params.getTy().toString());

        request.addQueryParameter("channels", params.getChannels() != null ? params.getChannels() : slice.getChannel());
        request.addQueryParameter("z_slices", params.getZSlices() != null ? params.getZSlices() : slice.getZStack());
        request.addQueryParameter("timepoints", params.getTimepoints() != null ? params.getTimepoints() : slice.getTime());

        request.addQueryParameter("gammas", params.getGammas());
        request.addQueryParameter("colormaps", params.getColormaps());
        request.addQueryParameter("min_intensities", params.getMinIntensities());
        request.addQueryParameter("max_intensities", params.getMaxIntensities());
        request.addQueryParameter("filters", params.getFilters());

        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(params.getFormat()));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }

        return request.toResponseEntity(proxy, byte[].class);
    }


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

        String server = this.internalImageServerURL;
        String uri = cropUri(slice.getPath(), cropParameter);
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

        return makeRequest("POST", server, uri, parameters, format, headers);
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

    public String cropUri(String filePath, CropParameter cropParameter) throws UnsupportedEncodingException, ParseException {
//        LinkedHashMap<String, Object> parameters = cropParameters(cropParameter);
        String uri;
        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            uri = this.buildEncodedUri("image", filePath, "/annotation/drawing");
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            uri = this.buildEncodedUri("image", filePath, "/annotation/mask");
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            uri = this.buildEncodedUri("image", filePath, "/annotation/crop");
        } else {
            uri = this.buildEncodedUri("image", filePath, "/annotation/crop");
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
        String server = this.internalImageServerURL;
        String uri = this.buildEncodedUri("image", slice, "/window");

        LinkedHashMap<String, Object> parameters = windowParameters(windowParameter);

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
        List<String> names = List.of("Cache-Control", "ETag", "X-Annotation-Origin", "X-Image-Size-Limit", "Content-Type", "Content-Disposition", "Last-Modified");
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

        String responseContentType = formatToMediaType(format);

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

    private static String formatToMediaType(String format) {
        return switch (format) {
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            case "jpg" -> MediaType.IMAGE_JPEG_VALUE;
            default -> MediaType.APPLICATION_JSON_VALUE;
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

}