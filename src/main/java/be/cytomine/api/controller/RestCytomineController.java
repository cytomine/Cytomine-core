package be.cytomine.api.controller;

import be.cytomine.api.controller.utils.RequestParams;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.InvalidRequestException;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.OffsetBasedPageRequest;
import be.cytomine.utils.Task;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SearchParametersUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.util.*;

import static be.cytomine.domain.image.UploadedFile_.contentType;
import static java.awt.SystemColor.text;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@Slf4j
public abstract class RestCytomineController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

//    /**
//     * Response a successful HTTP message
//     * @param data Message content
//     */
//    protected JsonObject responseSuccess(Object data, Map<String,String> params) {
//        if(data instanceof List) {
//            return responseList((List)data, params);
//        } else if(data instanceof Collection) {
//            List list = new ArrayList();
//            list.addAll((Collection)data);
//            return responseList(list, params);
//        }
//        else {
//            response(data);
//        }
//    }

    protected Pageable retrievePageable() {
        RequestParams requestParams = retrieveRequestParam();
        requestParams.putIfAbsent("offset", "0");
        requestParams.putIfAbsent("max", "0");
        requestParams.putIfAbsent("sort", "created");
        requestParams.putIfAbsent("order", "desc");

        Sort sort = Sort.by(requestParams.get("sort")).ascending();
        if (requestParams.get("order").equals("desc")) {
            sort = Sort.by(requestParams.get("sort")).descending();
        }

        int realMax = requestParams.get("max").equals("0") ? Integer.MAX_VALUE : Integer.parseInt(requestParams.get("max"));
//        int pageIndex =  Math.floor(Double.parseDouble(requestParams.get("offset")) / (double)realMax);
//
//        return PageRequest.of(
//                pageIndex,
//                realMax,
//                sort);

        return new OffsetBasedPageRequest(Long.parseLong(requestParams.get("offset")), realMax, sort);
    }



    protected List<SearchParameterEntry> retrieveSearchParameters() {
        return SearchParametersUtils.getSearchParameters(retrieveRequestParam());
    }


    protected JsonObject buildJsonList(List list) {
        RequestParams requestParams = retrieveRequestParam();
        requestParams.putIfAbsent("offset", "0");
        requestParams.putIfAbsent("max", "0");
        return buildJsonList(list, Integer.parseInt(requestParams.get("offset")), Integer.parseInt(requestParams.get("max")));
    }

    protected JsonObject buildJsonList(List list, Map<String,String> params) {
        return buildJsonList(list, Integer.parseInt(params.get("offset")), Integer.parseInt(params.get("max")));
    }

    protected JsonObject buildJsonList(List list, Integer offsetParameter, Integer maxParameter) {

        Integer offset = offsetParameter != null ? offsetParameter : 0;
        Integer max = (maxParameter != null && maxParameter!=0) ? maxParameter : Integer.MAX_VALUE;

        List subList;
        if (offset >= list.size()) {
            subList = new ArrayList();
        } else {
            int maxForCollection = Math.min(list.size() - offset, max);
            subList = list.subList(offset,offset + maxForCollection);
        }
        return JsonObject.of("collection", subList, "offset", offset, "perPage", Math.min(max, list.size()), "size", list.size(), "totalPages", Math.ceil((double)list.size()/(double)max));

    }

    protected JsonObject buildJsonList(Page page, Map<String,String> params) {
        // TODO: should we need params if we have page
        return buildJsonList(page, Integer.parseInt(params.get("offset")), Integer.parseInt(params.get("max")));
    }

    protected JsonObject buildJsonList(Page page, Integer offsetParameter, Integer maxParameter) {
        // TODO: should we need params if we have page
        List finalContent = page.getContent();
        if (page.getContent().size() > 0 && page.getContent().get(0) instanceof CytomineDomain) {
            finalContent = convertCytomineDomainListToJSON(page.getContent());
        }
        Integer offset = offsetParameter != null ? offsetParameter : 0;
        Integer max = (maxParameter != null && maxParameter!=0) ? maxParameter : Integer.MAX_VALUE;
        return JsonObject.of("collection", finalContent, "offset", offset, "perPage", Math.min(max, page.getContent().size()), "size", page.getTotalElements(), "totalPages", Math.ceil((double)page.getTotalElements()/(double)max));
    }

//    protected ResponseEntity<String> response(Map<String, Object> response, int code) {
//        return ResponseEntity.status(code).body(convertObjectToJSON(response));
//    }

    protected ResponseEntity<String> responseSuccess(Page page) {
        RequestParams requestParams = retrieveRequestParam();
        requestParams.putIfAbsent("offset", "0");
        requestParams.putIfAbsent("max", "0");
        return ResponseEntity.status(200).body(buildJsonList(page, requestParams).toJsonString());
    }

    protected ResponseEntity<String> responseSuccess(Page page, Integer offsetParameter, Integer maxParameter) {
        return ResponseEntity.status(200).body(buildJsonList(page, offsetParameter, maxParameter).toJsonString());
    }

    protected ResponseEntity<String> responseSuccess(Page page, Map<String,String> params) {
        return ResponseEntity.status(200).body(buildJsonList(page, params).toJsonString());
    }

    public ResponseEntity<String> responseSuccess(List list, Integer offsetParameter, Integer maxParameter) {
        if (list.isEmpty() || list.get(0) instanceof CytomineDomain) {
            return responseSuccessDomainList((List<? extends CytomineDomain>) list, offsetParameter, maxParameter);
        } else {
            return responseSuccessGenericList(list, offsetParameter, maxParameter);
        }
    }

    public ResponseEntity<String> responseSuccess(List list) {
        return responseSuccess(list, 0, 0);
    }

    private ResponseEntity<String> responseSuccessDomainList(List<? extends CytomineDomain> list) {
        return responseSuccessDomainList(list, 0, 0);
    }

    private ResponseEntity<String> responseSuccessDomainList(List<? extends CytomineDomain> list, Integer offsetParameter, Integer maxParameter) {
        return ResponseEntity.status(200).body(buildJsonList(convertCytomineDomainListToJSON(list), offsetParameter, maxParameter).toJsonString()); //TODO: perf convert after buildJsonList will avoid converting unused items (out of page)
    }

    private ResponseEntity<String> responseSuccessGenericList(List list) {
        return responseSuccessGenericList(list, 0, 0);
    }

    private ResponseEntity<String> responseSuccessGenericList(List list, Integer offsetParameter, Integer maxParameter) {
        return ResponseEntity.status(200).body(buildJsonList(list, offsetParameter, maxParameter).toJsonString()); //TODO: perf convert after buildJsonList will avoid converting unused items (out of page)
    }

    protected ResponseEntity<String> responseSuccess(CytomineDomain response) {
        return ResponseEntity.status(200).body(response.toJSON());
    }

    protected ResponseEntity<String> responseSuccess(JsonObject response) {
        return ResponseEntity.status(200).body(response.toJsonString());
    }


    protected ResponseEntity<String> buildJson(CytomineDomain response, int code) {
        return ResponseEntity.status(code).body(response.toJSON());
    }

    protected ResponseEntity<String> buildJson(Map<String, Object> response, int code) {
        return ResponseEntity.status(code).body(convertObjectToJSON(response));
    }

    protected ResponseEntity<String> responseSuccess(CommandResponse commandResponse) {
        return ResponseEntity.status(commandResponse.getStatus()).body(
                convertObjectToJSON(commandResponse.getData()));
    }

    protected String convertObjectToJSON(Object o) {
        return JsonObject.toJsonString(o);
    }

    protected String convertObjectToJsonObject(Object o) {
        return JsonObject.toJsonString(o);
    }



    protected String convertCytomineDomainToJSON(CytomineDomain cytomineDomain) {
        return cytomineDomain.toJSON();
    }

    protected JsonObject buildJson(List<? extends CytomineDomain> list, Map<String,String> params) {
        return buildJsonList(convertCytomineDomainListToJSON(list), params);
    }


    protected List<JsonObject> convertCytomineDomainListToJSON(List<? extends CytomineDomain> list) {
        List<JsonObject> results = new ArrayList<>();
        for (CytomineDomain cytomineDomain : list) {
            results.add(cytomineDomain.toJsonObject());
        }
        return results;
    }

//    protected List<JsonObject> convertObjectToJSON(List list) {
//        List<JsonObject> results = new ArrayList<>();
//        for (Object o : list) {
//            results.add(convertObjectToJSON(o));
//        }
//        return results;
//    }

    protected String convertListToJSON(List o) {
        return JsonObject.toJsonString(o);
    }

    /**
     * Call add function for this service with the json
     * json parameter can be an array or a single item
     * If json is array => add multiple item
     * otherwise add single item
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public ResponseEntity<String> add(ModelService service, JsonObject json) {
        try {
//            if (json instanceof JSONArray) {
//                responseResult(addMultiple(service, json))
//            } else {
            CommandResponse result = addOne(service, json);
            if(result!=null) {
                return responseSuccess(result);
            }
        } catch (CytomineException e) {
            log.error("add error:" + e.msg);
            log.error(e.toString());
            return buildJson(Map.of("success", false, "errors", e.msg), e.code);
        }
        return null;
    }

    /**
     * Call update function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public ResponseEntity<String> update(ModelService service, JsonObject json) {
        try {
            CytomineDomain domain =  service.retrieve(json);
            CommandResponse result = service.update(domain, json);
            return responseSuccess(result);
        } catch (CytomineException e) {
            log.error(e.toString());
            return buildJson(Map.of("success", false, "errors", e.msg), e.code);
        }
    }

    /**
     * Call delete function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public ResponseEntity<String> delete(ModelService service, JsonObject json, Task task) {
        try {
            CytomineDomain domain =  service.retrieve(json);
            CommandResponse result = service.delete(domain, transactionService.start(), task,true);
            return responseSuccess(result);
        } catch (CytomineException e) {
            log.error(e.toString());
            return buildJson(Map.of("success", false, "errors", e.msg, "errorValues", e.values), e.code);
        }
    }

    /**
     * Call add function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public CommandResponse addOne(ModelService service, JsonObject json) {
        return service.add(json);
    }

    /**
     * Build a response message for a domain not found
     * E.g. annotation 34 was not found
     * className = annotation, id = 34.
     * @param className Type of domain not found
     * @param id Domain id
     */
//    private static JsonObject buildJsonNotFound(String className, String id) {
//        return buildJsonNotFound(className, Map.of("id", id));
//    }
//
//    private static JsonObject buildJsonNotFound(String className, String filter, String id) {
//        return buildJsonNotFound(className, Map.of(filter, id));
//    }

    private static JsonObject buildJsonNotFound(String className, Map<String, Object> filters) {
        log.info("responseNotFound $className $id");
        log.error(className + " with filter " + filters + " does not exist");
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("errors", Map.of("message",  className + " not found with filters : " + filters));
        return jsonObject;
    }

//    private static JsonObject buildJsonNotFound(String className, Long id) {
//        return buildJsonNotFound(className, String.valueOf(id));
//    }

    protected ResponseEntity<String> responseNotFound(String className, String id) {
        return responseNotFound(className, "id", id);
    }
    protected ResponseEntity<String> responseNotFound(String className, Long id) {
        return responseNotFound(className, "id", String.valueOf(id));
    }
    protected ResponseEntity<String> responseNotFound(String className, String filter, String id) {
        return responseNotFound(className, Map.of(filter, id));
    }
    protected ResponseEntity<String> responseNotFound(String className, String filter, Long id) {
        return responseNotFound(className, Map.of(filter, String.valueOf(id)));
    }
    protected ResponseEntity<String> responseNotFound(String className, Map<String, Object> filters) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildJsonNotFound(className, filters).toJsonString());
    }

    protected RequestParams retrieveRequestParam() {
        Map<String, String[]> parameterMap = request.getParameterMap();
        RequestParams flatMap = new RequestParams();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String value = null;
            if (entry.getValue()!=null && entry.getValue().length>1) {
                throw new CytomineMethodNotYetImplementedException("Multiple request params are not supported in this method");
            } else if (entry.getValue()!=null && entry.getValue().length==1) {
                value = entry.getValue()[0];
            }
            flatMap.put(entry.getKey(), value);
        }
        return flatMap;
    }


    protected void responseImageByteArray(String contentType, byte[] bytes) throws IOException {
        response.setContentLength(bytes.length);
        response.setStatus(200);
        response.setHeader("Connection", "Keep-Alive");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "image/jpeg");
        try(OutputStream os = response.getOutputStream()) {
            os.write(bytes , 0, bytes.length);
            os.flush();
        }
    }

    protected void responseString(String contentType, String string) throws IOException {
        response.setContentType(contentType);
        response.setStatus(200);
        response.getWriter().write(string);
        response.getWriter().flush();
    }

    /**
     * Response an image as a HTTP response
     * @param bytes Image
     */
    protected void responseByteArray(byte[] bytes, String expectedFormat)  {
        try {
            RequestParams params = retrieveRequestParam();
            String format = expectedFormat;
            if (params.isTrue("alphaMask") || params.isValue("type", "alphaMask")) {
                format = "png";
            }

            if (format.equals("jpg")) {
                if (request.getMethod().equals("HEAD")) {
                    responseString("image/jpeg", "");
                } else {
                    responseImageByteArray("image/jpeg", bytes);
                }
            } else if (format.equals("tiff") || format.equals("tif")) {
                if (request.getMethod().equals("HEAD")) {
                    responseString("image/tiff", "");
                } else {
                    responseImageByteArray("image/tiff", bytes);
                }
            } else if (format.equals("png")) {
                if (request.getMethod().equals("HEAD")) {
                    responseString("image/png", "");
                } else {
                    responseImageByteArray("image/png", bytes);
                }
            } else {
                throw new InvalidRequestException("Format " + format + " is not supported");
            }
        }
        catch (IOException e) {
            log.error("Cannot response bytes from controller", e);
        }
    }
}
