package be.cytomine.api.controller;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.*;

@Slf4j
public abstract class RestCytomineController {

    @Autowired
    private TransactionService transactionService;

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

    protected JsonObject responseList(List list, Map<String,String> params) {
        return responseList(list, Integer.parseInt(params.get("offset")), Integer.parseInt(params.get("max")));
    }

    protected JsonObject responseList(List list, Integer offsetParameter, Integer maxParameter) {

        Integer offset = offsetParameter != null ? offsetParameter : 0;
        Integer max = (maxParameter != null && maxParameter!=0) ? maxParameter : Integer.MAX_VALUE;

        List subList;
        if (offset >= list.size()) {
            subList = new ArrayList();
        } else {
            int maxForCollection = Math.min(list.size() - offset, max);
            subList = list.subList(offset,offset + maxForCollection);
        }
        return JsonObject.of("collection", subList, "offset", offset, "perPage", Math.min(max, list.size()), "size", list.size(), "totalPages", Math.ceil(list.size()/max));

    }

    protected JsonObject responseList(Page page, Map<String,String> params) {
        // TODO: should we need params if we have page
        return responseList(page, Integer.parseInt(params.get("offset")), Integer.parseInt(params.get("max")));
    }

    protected JsonObject responseList(Page page, Integer offsetParameter, Integer maxParameter) {
        // TODO: should we need params if we have page
        Integer offset = offsetParameter != null ? offsetParameter : 0;
        Integer max = (maxParameter != null && maxParameter!=0) ? maxParameter : Integer.MAX_VALUE;
        return JsonObject.of("collection", page.getContent(), "offset", offset, "perPage", Math.min(max, page.getTotalElements()), "size", page.getTotalElements(), "totalPages", Math.ceil(page.getTotalElements()/max));
    }

//    protected ResponseEntity<String> response(Map<String, Object> response, int code) {
//        return ResponseEntity.status(code).body(convertObjectToJSON(response));
//    }
    protected ResponseEntity<JsonObject> responseSuccess(Page page, Map<String,String> params) {
        return ResponseEntity.status(200).body(responseList(page, params));
    }

    protected ResponseEntity<String> response(Map<String, Object> response, int code) {
        return ResponseEntity.status(code).body(convertObjectToJSON(response));
    }

    protected ResponseEntity<String> responseSuccess(CommandResponse commandResponse) {
        return ResponseEntity.status(commandResponse.getStatus()).body(
                convertObjectToJSON(commandResponse.getData()));
    }

    protected String convertObjectToJSON(Object o) {
        return JsonObject.toJsonString(o);
    }

    protected String convertCytomineDomainToJSON(CytomineDomain cytomineDomain) {
        return cytomineDomain.toJSON();
    }

    protected JsonObject response(List<? extends CytomineDomain> list, Map<String,String> params) {
        return responseList(convertCytomineDomainListToJSON(list), params);
    }


    protected List<JsonObject> convertCytomineDomainListToJSON(List<? extends CytomineDomain> list) {
        List<JsonObject> results = new ArrayList<>();
        for (CytomineDomain cytomineDomain : list) {
            results.add(cytomineDomain.toJsonObject());
        }
        return results;
    }

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
            return response(Map.of("success", false, "errors", e.msg), e.code);
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
            return response(Map.of("success", false, "errors", e.msg), e.code);
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
            return response(Map.of("success", false, "errors", e.msg, "errorValues", e.values), e.code);
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
    public static JsonObject responseNotFound(String className, String id) {
        return responseNotFound(className, Map.of("id", id));
    }

    public static JsonObject responseNotFound(String className, Map<String, Object> filters) {
        log.info("responseNotFound $className $id");
        log.error(className + " with filter " + filters + " does not exist");
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("errors", Map.of("message",  className + " not found with filters : " + filters));
        return jsonObject;
    }

    public static JsonObject responseNotFound(String className, Long id) {
        return responseNotFound(className, String.valueOf(id));
    }
}
