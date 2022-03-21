package be.cytomine.api.controller.utils;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.*;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.utils.ParamsService;
import be.cytomine.utils.GeometryUtils;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class AnnotationBuilder extends RestCytomineController {

    private final SecUserService secUserService;

    private final EntityManager entityManager;

    private final ParamsService paramsService;

    public AnnotationListing buildAnnotationListing(JsonObject params) {
        AnnotationListing al;
        if(isReviewedAnnotationAsked(params)) {
            al = new ReviewedAnnotationListing(entityManager);
        }
        else if(isRoiAnnotationAsked(params)) {
            al = new RoiAnnotationListing(entityManager);
        }
        else if(isAlgoAnnotationAsked(params)) {
            al = new AlgoAnnotationListing(entityManager);
        }
        else {
            al = new UserAnnotationListing(entityManager);
        }
        return buildAnnotationListing(al, params);
    }

    public AnnotationListing buildAnnotationListing(AnnotationListing al, JsonObject params) {
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
}
