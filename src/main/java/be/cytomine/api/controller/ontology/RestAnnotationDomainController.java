package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.*;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.utils.ParamsService;
import be.cytomine.utils.GeometryUtils;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationDomainController extends RestCytomineController {

    private final AnnotationListingService annotationListingService;

    private final SecUserService secUserService;

    private final EntityManager entityManager;
    
    private final ParamsService paramsService;

    private final RestUserAnnotationController restUserAnnotationController;
    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */

    @RequestMapping(value = { "/annotation/search.json"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> searchSpecified() throws IOException {
        return search();
    }

    @RequestMapping(value = {"/annotation.json"}, method = {RequestMethod.GET})
    public ResponseEntity<String> search() throws IOException {
        JsonObject params = mergeQueryParamsAndBodyParams();
        AnnotationListing annotationListing = buildAnnotationListing(params);
        List annotations = annotationListingService.listGeneric(annotationListing);
        return responseSuccess(annotations);
    }



    /**
     * Add an annotation
     * Redirect to the controller depending on the user type
     */
    @RequestMapping(value = "/annotation.json", method = {RequestMethod.POST})
    public ResponseEntity<String> add(@RequestBody JsonObject jsonObject) throws IOException {
        SecUser secUser = secUserService.getCurrentUser();
        if(jsonObject.getJSONAttrBoolean("roi", false)) {
            throw new CytomineMethodNotYetImplementedException("");
        } else if (secUser.isAlgo()) {
            throw new CytomineMethodNotYetImplementedException("");
        } else {
            return restUserAnnotationController.add(jsonObject);
        }
    }






    /**
     * Check if we ask reviewed annotation
     */
    private boolean isReviewedAnnotationAsked(JsonObject params) {
        return params.getJSONAttrBoolean("reviewed", false);
    }

    /**
     * Check if we ask reviewed annotation
     */
    private boolean isRoiAnnotationAsked(JsonObject params) {
        return params.getJSONAttrBoolean("roi", false);
    }

    /**
     * Check if we ask algo annotation
     */
    private boolean isAlgoAnnotationAsked(JsonObject params) {
        if(params.getJSONAttrBoolean("includeAlgo", false)) {
            return true;
        }

        Long idUser = params.getJSONAttrLong("user");
        if(idUser!=null) {
            return secUserService.find(idUser)
                    .orElseThrow(() -> new ObjectNotFoundException("User", idUser))
                    .isAlgo();
        } else {
            String idUsers = params.getJSONAttrStr("users");
            if(idUsers!=null && !idUsers.isEmpty()) {
                List<Long> collect = Arrays.stream(idUsers.replaceAll("_", ",")
                        .split(",")).map(Long::parseLong).collect(Collectors.toList());
                return collect.stream().anyMatch(secUserService::isUserJob);
            }
        }
        //if no other filter, just take user annotation
        return false;
    }



    private AnnotationListing buildAnnotationListing(JsonObject params) {
        AnnotationListing al;

        if(isReviewedAnnotationAsked(params)) {
            al = new ReviewedAnnotationListing(entityManager);
        }
        else if(isRoiAnnotationAsked(params)) {
            al = new RoiAnnotationListing(entityManager);
        }
        else if(isAlgoAnnotationAsked(params)) {
            al = new AlgoAnnotationListing(entityManager);
            // TODO
//            result.addAll(createRequest(al, params))
//
//            //if algo, we look for user_annotation JOIN algo_annotation_term  too
//            params.suggestedTerm = params.term
//            params.term = null
//            params.usersForTermAlgo = null
//            al = new UserAnnotationListing()
//            result.addAll(createRequest(al, params))
        }
        else {
            al = new UserAnnotationListing(entityManager);
            //result = createRequest(al, params)
        }
        al.setColumnsToPrint(paramsService.getPropertyGroupToShow(params));

        // Project
        al.setProject(params.getJSONAttrLong("project"));

        // Images
        al.setImage(params.getJSONAttrLong("image"));
        al.setImages(extractListFromParameter(params.getJSONAttrStr("images")));

        // Slices
        al.setSlice(params.getJSONAttrLong("slice"));
        al.setSlices(extractListFromParameter(params.getJSONAttrStr("slices")));

        // Tracks
        al.setTrack(params.getJSONAttrLong("track"));
        al.setTracks(extractListFromParameter(params.getJSONAttrStr("tracks")));

        if (al.getTrack()!=null || al.getTracks()!=null) {
            al.setBeforeSlice(params.getJSONAttrLong("beforeSlice"));
            al.setAfterSlice(params.getJSONAttrLong("afterSlice"));
            al.setSliceDimension(params.getJSONAttrLong("sliceDimension"));
        }

        // Users
        al.setUser(params.getJSONAttrLong("user"));
        al.setUsers(extractListFromParameter(params.getJSONAttrStr("users")));

        al.setUsersForTerm(extractListFromParameter(params.getJSONAttrStr("usersForTerm")));

        // Users for term algo
        al.setUserForTermAlgo(params.getJSONAttrLong("userForTermAlgo"));
        al.setUsersForTermAlgo(extractListFromParameter(params.getJSONAttrStr("usersForTermAlgo")));



        // Jobs
        if(params.getJSONAttrLong("job")!=null) {

            al.setUser(secUserService.findByJobId(params.getJSONAttrLong("job")).map(SecUser::getId).orElse(null));
        }

        // Jobs for term algo
        if(params.getJSONAttrLong("jobForTermAlgo")!=null) {
            al.setUserForTermAlgo(secUserService.findByJobId(params.getJSONAttrLong("jobjobForTermAlgo")).map(SecUser::getId).orElse(null));
        }

        // Tags
        al.setTag(params.getJSONAttrLong("tag"));
        al.setTags(extractListFromParameter(params.getJSONAttrStr("tags")));
        al.setNoTag(params.getJSONAttrBoolean("noTag", false));

        // Terms
        al.setTerm(params.getJSONAttrLong("term"));
        al.setTerms(extractListFromParameter(params.getJSONAttrStr("terms")));


        // Suggested terms
        al.setSuggestedTerm(params.getJSONAttrLong("suggestedTerm"));
        al.setSuggestedTerms(extractListFromParameter(params.getJSONAttrStr("suggestedTerms")));

        // Boolean for terms
        al.setNoTerm(params.getJSONAttrBoolean("noTerm", false));
        al.setNoAlgoTerm(params.getJSONAttrBoolean("noAlgoTerm", false));
        al.setMultipleTerm(params.getJSONAttrBoolean("multipleTerm", false));
        al.setNoTrack(params.getJSONAttrBoolean("noTrack", false));
        al.setMultipleTrack(params.getJSONAttrBoolean("multipleTrack", false));

        // Review
        al.setNotReviewedOnly(params.getJSONAttrBoolean("notReviewedOnly", false));

        // Review users
        // TODO: reviewUser ?
        al.setReviewUsers(extractListFromParameter(params.getJSONAttrStr("reviewUsers")));

        // Kmeans
        al.setKmeans(params.getJSONAttrBoolean("kmeans", false));
        al.setKmeansValue(params.getJSONAttrInteger("kmeansValue", null));

        // BBOX
        if(params.get("bbox")!=null) {
            try {
                al.setBbox(GeometryUtils.createBoundingBox(params.getJSONAttrStr("bbox")).toText());
            } catch (ParseException e) {
                throw new WrongArgumentException("Geometry cannot be parsed for annotation search request:" + e);
            }
        }
        if(params.get("bboxAnnotation")!=null) {
            AnnotationDomain annotationDomain = AnnotationDomain.getAnnotationDomain(entityManager, params.getJSONAttrLong("bboxAnnotation"));
            al.setBboxAnnotation(annotationDomain.getWktLocation());
        }

        // Base annotation
        al.setBaseAnnotation(params.getJSONAttrLong("baseAnnotation")!=null?
                params.getJSONAttrLong("baseAnnotation") : // can be an annotation id
                params.getJSONAttrStr("baseAnnotation")); // can be a string (wkt) too
        al.setMaxDistanceBaseAnnotation(params.getJSONAttrLong("maxDistanceBaseAnnotation"));

        // Date
        al.setAfterThan(params.getJSONAttrDate("afterThan"));
        al.setBeforeThan(params.getJSONAttrDate("beforeThan"));

        al.setExcludedAnnotation(params.getJSONAttrLong("excludedAnnotation")); // TODO ?

        return al;
    }


    private JsonObject mergeQueryParamsAndBodyParams() throws IOException {
        JsonObject response = new JsonObject();
        Map<String, String[]> parameterMap = super.request.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue()!= null  && entry.getValue().length>1) {
                throw new CytomineMethodNotYetImplementedException("Multiple request params are not supported in this method");
            } else if(entry.getValue()!= null && entry.getValue().length==1) {
                response.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        if (request.getMethod().equals("POST")) {
            String bodyData = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (!bodyData.isEmpty()) {
                Map<String, Object> bodyMap = JsonObject.toMap(bodyData);
                response.putAll(bodyMap);
            }
        }

        return response;
    }







}
