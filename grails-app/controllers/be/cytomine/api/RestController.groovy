package be.cytomine.api

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ServerException
import be.cytomine.utils.Task
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class RestController {

    def sessionFactory
    def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP
    def springSecurityService
    def grailsApplication

    static final int NOT_FOUND_CODE = 404

    def transactionService

    def currentDomain() {
        return null
    }


    //usefull for doc
//    def currentDomainName() {
//        return
//    }
    /**
     * Call add function for this service with the json
     * json parameter can be an array or a single item
     * If json is array => add multiple item
     * otherwise add single item
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public Object add(def service, def json) {
        try {
            if (json instanceof JSONArray) {
                responseResult(addMultiple(service, json))
            } else {
                def result = addOne(service, json)
                if(result) {
                    responseResult(result)
                }
            }
        } catch (CytomineException e) {
            log.error("add error:" + e.msg)
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Call update function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public Object update(def service, def json) {
        try {
            def domain =  service.retrieve(json)
            def result = service.update(domain,json)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Call delete function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public Object delete(def service, def json,Task task) {
        try {
            def domain = service.retrieve(json)
            def result = service.delete(domain,transactionService.start(),task,true)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg, errorValues : e.values], e.code)
        }
    }

    /**
     * Call add function for this service with the json
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public Object addOne(def service, def json) {
        return service.add(json)
    }

    /**
     * Call add function for this service for each item from the json array
     * @param service Service for this domain
     * @param json JSON data
     * @return response
     */
    public Object addMultiple(def service, def json) {
        return service.addMultiple(json)
    }

    /**
     * Response this data as HTTP response
     * @param data Data ro send
     * @return response
     */
    // TODO see ImageInstanceController to think how to make it more flexible
    protected def response(data) {
        withFormat {
            json {
                def result = data as JSON

                if(isFilterResponseEnabled()) result = filterResponse(result)

                render result
            }
            jsonp {
                response.contentType = 'application/javascript'
                render "${params.callback}(${data as JSON})"
            }
        }
    }

    private grails.converters.JSON filterResponse(grails.converters.JSON response){
        JSONObject json = JSON.parse(response.toString())
        if(json.containsKey("collection")) {
            for(JSONObject element : json.collection) {
                filterOneElement(element)
            }
        } else {
            filterOneElement(json)
        }

        return json as JSON
    }

    protected void filterOneElement(JSONObject element){
        if(isFilterResponseEnabled()) throw new ServerException("Filter enabled but no filter defined")
    }

    /**
     * Build a response message for an object return by a command
     * @param result Command result
     * @return response
     */
    protected def responseResult(result) {
        response.status = result.status
        withFormat {
            json { render result.data as JSON }
        }
    }

    /**
     * Response a successful HTTP message
     * @param data Message content
     */
    protected def responseSuccess(data) {
        if(data instanceof List) {
            return responseList(data)
        } else if(data instanceof Collection) {
            List list = []
            list.addAll(data)
            return responseList(list)
        }
        else {
            response(data)
        }
    }

    protected def responseList(List list) {

        Boolean datatables = (params.datatables != null)

        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer max = (params.max != null && params.getInt('max')!=0) ? params.getInt('max') : Integer.MAX_VALUE

        List subList
        if (offset >= list.size()) {
            subList = []
        } else {
            def maxForCollection = Math.min(list.size() - offset, max)
            subList = list.subList(offset,offset + maxForCollection)
        }

        if (datatables) {
            responseSuccess ([aaData: subList, sEcho: params.sEcho , iTotalRecords: list.size(), iTotalDisplayRecords : list.size()])
        } else {
            responseSuccess ([collection: subList, offset: offset, perPage : Math.min(max, list.size()), size: list.size(), totalPages: Math.ceil(list.size()/max)])
        }

    }

    /**
     * Response an HTTP message
     * @param data Message content
     * @param code HTTP code
     */
    protected def response(data, code) {
        response.status = code
        response(data)
    }

    public def responseError(CytomineException e) {
        response([success: false, errors: e.msg], e.code)
    }

    /**
     * Build a response message for a domain not found
     * E.g. annotation 34 was not found
     * className = annotation, id = 34.
     * @param className Type of domain not found
     * @param id Domain id
     */
    protected def responseNotFound(className, id) {
        log.info "responseNotFound $className $id"
        log.error className + " Id " + id + " does not exist"
        response.status = NOT_FOUND_CODE
        render(contentType: 'text/json') {
            errors(message: className + " not found with id : " + id)
        }
    }

    /**
     * Build a response message for a domain not found with 1 filter
     * E.g. relationterm find by relation => relationterm not found
     * className = relationterm, filter1 = relation, ids = relation.id, ...
     * @param className Type of domain not found
     * @param filter1 Type of domain for the first filter
     * @param id1 Id for the first filter
     */
    protected def responseNotFound(className, filter, id) {
        log.info className + ": " + filter + " " + id + " does not exist"
        response.status = NOT_FOUND_CODE
        render(contentType: 'text/json') {
            errors(message: className + " not found with id " + filter + " : " + id)
        }
    }

    /**
     * Build a response message for a domain not found with 2 filter
     * E.g. relationterm find by relation + term1 => relationterm not found
     * className = relationterm, filter1 = relation, ids = relation.id, ...
     * @param className Type of domain not found
     * @param filter1 Type of domain for the first filter
     * @param id1 Id for the first filter
     * @param filter2 Type of domain for the second filter
     * @param id2 Id for the second filter
     */
    protected def responseNotFound(className, filter1, filter2, id1, id2) {
        log.info className + ": " + filter1 + " " + id1 + ", " + filter2 + " " + id2 + " does not exist"
        response.status = NOT_FOUND_CODE
        render(contentType: 'text/json') {
            errors(message: className + " not found with id " + filter1 + " : " + id1 + " and  " + filter2 + " : " + id2)
        }
    }

    /**
     * Build a response message for a domain not found with 3 filter
     * E.g. relationterm find by relation + term1 + term 2 => relationterm not found
     * className = relationterm, filter1 = relation, ids = relation.id, ...
     * @param className Type of domain not found
     * @param filter1 Type of domain for the first filter
     * @param id1 Id for the first filter
     * @param filter2 Type of domain for the second filter
     * @param id2 Id for the second filter
     * @param filter3 Type of domain for the third filter
     * @param id3 Id for the third filter
     */
    protected def responseNotFound(className, filter1, id1, filter2, id2, filter3, id3) {
        log.info className + ": " + filter1 + " " + id1 + ", " + filter2 + " " + id2 + " and " + filter3 + " " + id3 + " does not exist"
        response.status = NOT_FOUND_CODE
        render(contentType: 'text/json') {
            errors(message: className + " not found with id " + filter1 + " : " + id1 + ",  " + filter2 + " : " + id2 + " and " + filter3 + " : " + id3)
        }
    }

    /**
     * Response an image as a HTTP response
     * @param bytes Image
     */
    protected def responseByteArray(byte[] bytes) {
        log.info params.format
        if (params.alphaMask || params.type == 'alphaMask')
            params.format = 'png'

        log.info params.format
        if (params.format == 'jpg') {
            if (request.method == 'HEAD') {
                render(text: "", contentType: "image/jpeg");
            }
            else {
                response.contentLength = bytes.length
                response.setHeader("Connection", "Keep-Alive")
                response.setHeader("Accept-Ranges", "bytes")
                response.setHeader("Content-Type", "image/jpeg")
                response.getOutputStream() << bytes
                response.getOutputStream().flush()
            }
        }
        else if (params.format == 'tiff' || params.format == 'tif') {
            if (request.method == 'HEAD') {
                render(text: "", contentType: "image/tiff")
            }
            else {
                response.contentLength = bytes.length
                response.setHeader("Connection", "Keep-Alive")
                response.setHeader("Accept-Ranges", "bytes")
                response.setHeader("Content-Type", "image/tiff")
                response.getOutputStream() << bytes
                response.getOutputStream().flush()
            }
        }
        else {
            if (request.method == 'HEAD') {
                render(text: "", contentType: "image/png")
            }
            else {
                response.contentLength = bytes.length
                response.setHeader("Connection", "Keep-Alive")
                response.setHeader("Accept-Ranges", "bytes")
                response.setHeader("Content-Type", "image/png")
                response.getOutputStream() << bytes
                response.getOutputStream().flush()
            }
        }
    }

    protected def responseFile(String name, File f) {
        response.setContentType "application/octet-stream"
        response.setHeader "Content-disposition", "attachment; filename=${name}"

        BufferedInputStream bufferedInputStream = f.newInputStream()
        response.outputStream << bufferedInputStream
        response.outputStream.flush()
        bufferedInputStream.close()
    }

    protected def responseFile(String name, byte[] array) {
        response.setContentType "application/octet-stream"
        response.setHeader "Content-disposition", "attachment; filename=${name}"
        response.outputStream << array
        response.outputStream.flush()
    }

    private static String SEARCH_PARAM_EQUALS = "equals"
    private static String SEARCH_PARAM_LIKE = "like"
    private static String SEARCH_PARAM_ILIKE = "ilike"

    static def equalsOperators = [SEARCH_PARAM_EQUALS]
    static def likeOperators = [SEARCH_PARAM_LIKE]
    static def ilikeOperators = [SEARCH_PARAM_ILIKE]
    static def equalsAndLikeOperators = [SEARCH_PARAM_EQUALS, SEARCH_PARAM_LIKE]
    static def equalsAndIlikeOperators = [SEARCH_PARAM_EQUALS, SEARCH_PARAM_ILIKE]
    static def likeAndIlikeOperators = [SEARCH_PARAM_LIKE, SEARCH_PARAM_ILIKE]
    static def equalsAndLikeAndIlikeOperators = [SEARCH_PARAM_EQUALS, SEARCH_PARAM_LIKE, SEARCH_PARAM_ILIKE]

    static def allowedOperators = ["equals","like","ilike","lte", "gte", "in"]
    final protected def getSearchParameters(){
        def searchParameters = []
        for(def param : params){
            if (param.key ==~ /.+\[.+\]/) {
                String[] tmp = param.key.split('\\[')
                String operator = tmp[1].substring(0,tmp[1].length()-1)
                String field = tmp[0]

                def values = param.value
                if(operator.equals("in")) {
                    if(values.contains(",")) values = values.split(",") as List
                }
                if(values instanceof List) values = values.collect {URLDecoder.decode(it.toString(), "UTF-8")}
                else values = URLDecoder.decode(values.toString(), "UTF-8")

                if(operator.contains("like")) {
                    values = values.replace('*','%')
                }

                if(allowedOperators.contains(operator)) searchParameters << [operator : operator, field : tmp[0], values : values]
            }
        }
        return searchParameters
    }

    final protected def getSearchParameters(def allowedParameters){
        def searchParameters = []
        for(def param : params){
            if (param.key ==~ /.+\[.+\]/) {
                String[] tmp = param.key.split('\\[')
                String operator = tmp[1].substring(0,tmp[1].length()-1)
                String field = tmp[0]

                def allowedParameter = allowedParameters.find { it.field == field }
                if (allowedParameter?.allowedOperators?.contains(operator)) {
                    String value = param.value
                    if (operator == SEARCH_PARAM_LIKE || operator == SEARCH_PARAM_ILIKE)
                        value = "%$value%"

                    def sqlOperator = (operator == SEARCH_PARAM_EQUALS) ? "=" : operator
                    searchParameters << [operator: operator, field: field, values: value, sqlOperator: sqlOperator]
                }
            }
        }
        return searchParameters
    }
    /**
     * Substract the collection with offset (min) and max
     * @param collection Full collection
     * @param offset Min index
     * @param max Maximum index
     * @return Substracted collection with first elem = min and last elem (= max)
     */
    protected def substract(List collection, Integer offset, Integer max) {
        if (offset >= collection.size()) {
            return []
        }
        def maxForCollection = Math.min(collection.size() - offset, max)
        return collection.subList(offset, offset + maxForCollection)
    }

    protected boolean isFilterResponseEnabled() {
        return false
    }

}