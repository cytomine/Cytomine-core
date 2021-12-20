package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.dto.AnnotationLight;
import be.cytomine.dto.SimplifiedAnnotation;
import be.cytomine.exceptions.*;
import be.cytomine.repository.AlgoAnnotationListing;
import be.cytomine.repository.UserAnnotationListing;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.BoundariesCropParameter;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.SimplifyGeometryService;
import be.cytomine.service.utils.ValidateGeometryService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.GeometryUtils;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import liquibase.pro.packaged.A;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAnnotationService extends ModelService {

    private final UserAnnotationRepository userAnnotationRepository;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final AnnotationListingService annotationListingService;

    private final EntityManager entityManager;

    private final SliceInstanceService sliceInstanceService;

    private final ImageInstanceService imageInstanceService;

    private final SimplifyGeometryService simplifyGeometryService;

    private final TransactionService transactionService;

    private final ValidateGeometryService validateGeometryService;

    @Override
    public Class currentDomain() {
        return UserAnnotation.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new UserAnnotation().buildDomainFromJson(json, entityManager);
    }

    public UserAnnotation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<UserAnnotation> find(Long id) {
        Optional<UserAnnotation> optionalUserAnnotation = userAnnotationRepository.findById(id);
        optionalUserAnnotation.ifPresent(userAnnotation -> securityACLService.check(userAnnotation.container(),READ));
        return optionalUserAnnotation;
    }

// TODO: seems to be useless ; no migration?:
//    def list(Project project, def propertiesToShow = null) {
//        securityACLService.check(project.container(), READ)
//        annotationListingService.executeRequest(new UserAnnotationListing(
//                project: project.id,
//                columnToPrint: propertiesToShow
//        ))
//    }


    public List listIncluded(ImageInstance image, String geometry, SecUser user, List<Long> terms, AnnotationDomain annotation, List<String> propertiesToShow) {
        securityACLService.check(image.container(), READ);

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setColumnsToPrint(propertiesToShow);
        userAnnotationListing.setImage(image.getId());
        userAnnotationListing.setUser(user.getId());
        userAnnotationListing.setTerms(terms);
        userAnnotationListing.setExcludedAnnotation((annotation!=null? annotation.getId() : null));
        userAnnotationListing.setBbox(geometry);
        return annotationListingService.executeRequest(userAnnotationListing);
    }


    //TODO: seems to be useless ; no migration?: + need job
    /**
     * List annotation where a user from 'userList' has added term 'realTerm' and for which a specific job has predicted 'suggestedTerm'
     * @param project Annotation project
     * @return
     */
//    List list(Project project, List<Long> userList, Term realTerm, Term suggestedTerm, Job job,
//             def propertiesToShow = null) {
//        securityACLService.check(project.container(), READ)
//        if (userList.isEmpty()) {
//            return []
//        }
//        annotationListingService.executeRequest(new UserAnnotationListing(
//                columnToPrint: propertiesToShow,
//                project: project.id,
//                users: userList,
//                term: realTerm.id,
//                suggestedTerm: suggestedTerm.id,
//                userForTermAlgo: UserJob.findByJob(job)
//        ))
//    }
//TODO: seems to be useless ; no migration?:
//
//    /**
//     * List annotations according to some filters parameters (rem : use list light if you only need the response, not
//     * the objects)
//     * @param image the image instance
//     * @param bbox Geometry restricted Area
//     * @param termsIDS filter terms ids
//     * @param userIDS filter user ids
//     * @return Annotation listing
//     */
//    def list(ImageInstance image, Geometry bbox, List<Long> termsIDS, List<Long> userIDS) {
//        //:to do use listlight and parse WKT instead ?
//        Collection<UserAnnotation> annotations = UserAnnotation.createCriteria()
//                .add(Restrictions.isNull("deleted"))
//                .add(Restrictions.in("user.id", userIDS))
//                .add(Restrictions.eq("image.id", image.id))
//                .add(SpatialRestrictions.intersects("location", bbox))
//                .list()
//
//        if (!annotations.isEmpty() && termsIDS.size() > 0) {
//            annotations = (Collection<UserAnnotation>) AnnotationTerm.createCriteria().list {
//                isNull("deleted")
//                inList("term.id", termsIDS)
//                join("userAnnotation")
//                createAlias("userAnnotation", "a")
//                projections {
//                    inList("a.id", annotations.collect { it.id })
//                    groupProperty("userAnnotation")
//                }
//            }
//        }
//
//        return annotations
//    }
//TODO: seems to be useless ; no migration?:

//    def list(SliceInstance slice, Geometry bbox, List<Long> termsIDS, List<Long> userIDS) {
//        //:to do use listlight and parse WKT instead ?
//        Collection<UserAnnotation> annotations = UserAnnotation.createCriteria()
//                .add(Restrictions.isNull("deleted"))
//                .add(Restrictions.in("user.id", userIDS))
//                .add(Restrictions.eq("slice.id", slice.id))
//                .add(SpatialRestrictions.intersects("location", bbox))
//                .list()
//
//        if (!annotations.isEmpty() && termsIDS.size() > 0) {
//            annotations = (Collection<UserAnnotation>) AnnotationTerm.createCriteria().list {
//                isNull("deleted")
//                inList("term.id", termsIDS)
//                join("userAnnotation")
//                createAlias("userAnnotation", "a")
//                projections {
//                    inList("a.id", annotations.collect { it.id })
//                    groupProperty("userAnnotation")
//                }
//            }
//        }
//
//        return annotations
//    }

    //TODO: seems to be useless ; no migration?:
//    def count(User user, Project project = null) {
//        if (project) return UserAnnotation.countByUserAndProject(user, project)
//        return UserAnnotation.countByUser(user)
//    }


    public Long countByProject(Project project) {
        return countByProject(project, null, null);
    }

    public Long countByProject(Project project, Date startDate, Date endDate) {
        if (startDate!=null && endDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedBetween(project, startDate, endDate);
        } else if (startDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedAfter(project, startDate);
        } else if (endDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedBefore(project, endDate);
        } else {
            return userAnnotationRepository.countByProject(project);
        }
    }

    /**
     * List all annotation with a very light structure: id, project and crop url
     */
    public List<AnnotationLight> listLight() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return userAnnotationRepository.listLight();
    }


    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        SliceInstance slice = null;
        ImageInstance image = null;

        if (!jsonObject.isMissing("slice")) {
            slice = sliceInstanceService.find(jsonObject.getJSONAttrLong("slice"))
                    .orElseThrow(() -> new ObjectNotFoundException("SliceInstance with id " + jsonObject.get("slice")));
            image = slice.getImage();
        } else if (!jsonObject.isMissing("image")) {
            image = imageInstanceService.find(jsonObject.getJSONAttrLong("image"))
                    .orElseThrow(() -> new ObjectNotFoundException("ImageInstance with id " + jsonObject.get("image")));
            slice = imageInstanceService.getReferenceSlice(image);

        }
        Project project = slice.getProject();

        if (jsonObject.isMissing("location")) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + jsonObject.get("location"));
        }

        jsonObject.put("slice", slice.getId());
        jsonObject.put("image", image.getId());
        jsonObject.put("project", project.getId());

        securityACLService.check(project, READ);
        securityACLService.checkIsNotReadOnly(project);

        SecUser currentUser = currentUserService.getCurrentUser();
        if (jsonObject.isMissing("user")) {
            jsonObject.put("user", currentUser.getId());
        } else if (!Objects.equals(jsonObject.getJSONAttrLong("user"), currentUser.getId())) {
            securityACLService.checkFullOrRestrictedForOwner(project, null);
        }

        Geometry annotationShape;
        try {
            annotationShape = new WKTReader().read(jsonObject.getJSONAttrStr("location"));
        }
        catch (Exception ignored) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid");
        }


        Envelope envelope = annotationShape.getEnvelopeInternal();
        boolean isSizeDefined = image.getBaseImage().getWidth()!=null && image.getBaseImage().getHeight()!=null;
        if (isSizeDefined && (envelope.getMinX() < 0 || envelope.getMinY() < 0 ||
                envelope.getMaxX() > image.getBaseImage().getWidth() ||
                envelope.getMaxY() > image.getBaseImage().getHeight())) {
            double maxX = Math.min(envelope.getMaxX(), image.getBaseImage().getWidth());
            double maxY = Math.min(envelope.getMaxY(), image.getBaseImage().getHeight());
            Geometry insideBounds = null;
            try {
                insideBounds = new WKTReader().read("POLYGON((0 0,0 " + maxY + "," + maxX + " " + maxY + "," + maxX + " 0,0 0))");
            } catch (ParseException e) {
                throw new WrongArgumentException("Annotation cannot be parsed with maxX/maxY:" + e.getMessage());
            }
            annotationShape = annotationShape.intersection(insideBounds);
        }

        if(!(annotationShape.getGeometryType().equals("LineString"))) {
            BoundariesCropParameter boundaries = GeometryUtils.getGeometryBoundaries(annotationShape);
            if (boundaries == null || boundaries.getWidth() == 0 || boundaries.getHeight() == 0) {
                throw new WrongArgumentException("Annotation dimension not valid");
            }
        }

        //simplify annotation
        try {
            SimplifiedAnnotation simplifiedAnnotation =
                    simplifyGeometryService.simplifyPolygon(annotationShape, jsonObject.getJSONAttrLong("minPoint", null), jsonObject.getJSONAttrLong("maxPoint", null));
            jsonObject.put("location", simplifiedAnnotation.getNewAnnotation());
            jsonObject.put("geometryCompression", simplifiedAnnotation.getRate());
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e);
        }

        if (jsonObject.isMissing("location")) {
            jsonObject.put("location", annotationShape);
            jsonObject.put("geometryCompression", 0.0d);
        }

        if (jsonObject.get("location") instanceof Geometry) {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid((Geometry)jsonObject.get("location")));
        } else {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid(jsonObject.getJSONAttrStr("location")));
        }

        //Start transaction
        Transaction transaction = transactionService.start();

        CommandResponse commandResponse = executeCommand(new AddCommand(currentUser, transaction), null, jsonObject);
        UserAnnotation addedAnnotation = (UserAnnotation)commandResponse.getObject();

        if (addedAnnotation == null) {
            return commandResponse;
        }


        // Add annotation-term if any
        List<Long> termIds = new ArrayList<>();
        termIds.addAll(jsonObject.getJSONAttrListLong("term", new ArrayList<>()));
        termIds.addAll(jsonObject.getJSONAttrListLong("terms", new ArrayList<>()));

        List<Term> terms = new ArrayList<>();
        for (Long termId : termIds) {
            throw new CytomineMethodNotYetImplementedException("");
//            CommandResponse response = annotationTermService.addAnnotationTerm(addedAnnotation.getId(), termId, null, currentUser.getId(), currentUser, transaction);
//            terms.add(((AnnotationTerm)(response.getData().get("annotationterm"))).getTerm());
        }

        ((Map<String, Object>)commandResponse.getData().get("annotation")).put("term", terms);



        // Add properties if any
        //TODO: uncomment when properties will be implemented
        Map<String, String> properties = new HashMap<>();
        properties.putAll(jsonObject.getJSONAttrMapString("property", new HashMap<>()));
        properties.putAll(jsonObject.getJSONAttrMapString("properties", new HashMap<>()));

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            throw new CytomineMethodNotYetImplementedException("");
//            String key = entry.getKey();
//            String value = entry.getValue();
//            propertyService.addProperty("be.cytomine.ontology.UserAnnotation", addedAnnotation.getId(), key, value, currentUser, transaction);
        }

        //TODO: uncomment when track will be implemented
        // Add annotation-term if any
        List<Long> tracksIds = new ArrayList<>();
        tracksIds.addAll(jsonObject.getJSONAttrListLong("track", new ArrayList<>()));
        tracksIds.addAll(jsonObject.getJSONAttrListLong("tracks", new ArrayList<>()));
        if (!tracksIds.isEmpty()) {
            throw new CytomineMethodNotYetImplementedException("");
        }
//        List<AnnotationTrack> annotationTracks = new ArrayList<>();
//        for (Long trackId : tracksIds) {
//            CommandResponse response =
//                    annotationTrackService.addAnnotationTrack("be.cytomine.ontology.UserAnnotation", addedAnnotation.getId(), trackId, addedAnnotation.getSlice().getId(), currentUser, transaction);
//            annotationTracks.add((AnnotationTrack) response.getData().get("annotationtrack"));
//        }
//        ((Map<String, Object>)commandResponse.getData().get("annotation")).put("annotationTrack", annotationTracks);
//        ((Map<String, Object>)commandResponse.getData().get("annotation")).put("track", annotationTracks.stream().map(x -> x.getTrack()).collect(Collectors.toList()));



        return commandResponse;
    }

    protected void beforeAdd(CytomineDomain domain) {
        // this will be done in the PrePersist method ; but the validation is done before PrePersist
        ((UserAnnotation)domain).setWktLocation(((UserAnnotation)domain).getLocation().toText());
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
    }


    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        //securityACLService.checkIsSameUserOrAdminContainer(annotation,annotation.user,currentUser)
        securityACLService.checkFullOrRestrictedForOwner(domain, ((UserAnnotation)domain).getUser());

        // TODO: what about image/project ??

        Geometry annotationShape;
        try {
            annotationShape = new WKTReader().read(jsonNewData.getJSONAttrStr("location"));
        }
        catch (ParseException ignored) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        ImageInstance image = imageInstanceService.find(jsonNewData.getJSONAttrLong("image"))
                .orElseThrow(() -> new WrongArgumentException("Annotation not associated with a valid image"));

        Envelope envelope = annotationShape.getEnvelopeInternal();
        boolean isSizeDefined = image.getBaseImage().getWidth()!=null && image.getBaseImage().getHeight()!=null;
        if (isSizeDefined && (envelope.getMinX() < 0 || envelope.getMinY() < 0 ||
                envelope.getMaxX() > image.getBaseImage().getWidth() ||
                envelope.getMaxY() > image.getBaseImage().getHeight())) {
            double maxX = Math.min(envelope.getMaxX(), image.getBaseImage().getWidth());
            double maxY = Math.min(envelope.getMaxY(), image.getBaseImage().getHeight());
            Geometry insideBounds = null;
            try {
                insideBounds = new WKTReader().read("POLYGON((0 0,0 " + maxY + "," + maxX + " " + maxY + "," + maxX + " 0,0 0))");
            } catch (ParseException e) {
                throw new WrongArgumentException("Annotation cannot be parsed with maxX/maxY:" + e.getMessage());
            }
            annotationShape = annotationShape.intersection(insideBounds);
        }

        //simplify annotation
        try {
            double rate = jsonNewData.getJSONAttrDouble("geometryCompression", 0d);
            SimplifiedAnnotation data = simplifyGeometryService.simplifyPolygon(annotationShape, rate);
            jsonNewData.put("location", data.getNewAnnotation());
            jsonNewData.put("geometryCompression", data.getRate());
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e);
        }

        if (jsonNewData.get("location") instanceof Geometry) {
            jsonNewData.put("location", validateGeometryService.tryToMakeItValidIfNotValid((Geometry)jsonNewData.get("location")));
        } else {
            jsonNewData.put("location", validateGeometryService.tryToMakeItValidIfNotValid(jsonNewData.getJSONAttrStr("location")));
        }
        CommandResponse result = executeCommand(new EditCommand(currentUser, null), domain, jsonNewData);

        // TODO: retrieval
//        if (result.success) {
//            Long id = result.userannotation.id
//            try {
//                updateRetrievalAnnotation(id)
//            } catch (Exception e) {
//                log.error "Cannot update in retrieval:" + e.toString()
//            }
//        }

        return result;
    }


    protected void afterUpdate(CytomineDomain domain, CommandResponse response) {
        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
    }



    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),DELETE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }

    protected void afterDelete(CytomineDomain domain, CommandResponse response) {
        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
    }

    // TODO!
//    List repeat(Long userAnnotation, Long baseSliceId, int repeat) {
//        SliceInstance currentSlice = sliceInstanceService.find(baseSliceId)
//                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance with id " + baseSliceId));
//
//        def slices = SliceInstance.createCriteria().list {
//            createAlias("baseSlice", "as")
//            eq("image", userAnnotation.image)
//            order("as.time", "asc")
//            order("as.zStack", "asc")
//            order("as.channel", "asc")
//            fetchMode("baseSlice", FetchMode.JOIN)
//            ge("as.time", currentSlice.baseSlice.time)
//            ge("as.zStack", currentSlice.baseSlice.zStack)
//            ge("as.channel", currentSlice.baseSlice.channel)
//            ne("id", userAnnotation.slice.id)
//            maxResults(repeat)
//        }
//
//        def collection = []
//        slices.each { slice ->
//                collection << add(new JSONObject([
//                        slice   : slice.id,
//                location: userAnnotation.location.toString(),
//                terms   : userAnnotation.termsId(),
//                tracks  : userAnnotation.tracksId()
//            ]))
//        }
//
//        return [collection: collection]
//    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        UserAnnotation annotation = (UserAnnotation)domain;
        return List.of(currentUserService.getCurrentUser().toString(), annotation.getImage().getBlindInstanceFilename(), ((UserAnnotation) domain).getUser().toString());
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        return;
    }



//TODO
    public void deleteDependentAlgoAnnotationTerm(UserAnnotation ua, Transaction transaction, Task task) {
//        AlgoAnnotationTerm.findAllByAnnotationIdent(ua.id).each {
//            algoAnnotationTermService.delete(it, transaction, null, false)
//        }

    }

//    def deleteDependentAnnotationTerm(UserAnnotation ua, Transaction transaction, Task task = null) {
//        AnnotationTerm.findAllByUserAnnotation(ua).each {
//            try {
//                annotationTermService.delete(it, transaction, null, false)
//            } catch (ForbiddenException fe) {
//                throw new ForbiddenException("This annotation has been linked to the term " + it.term + " by " + it.userDomainCreator() + ". " + it.userDomainCreator() + " must unlink its term before you can delete this annotation.")
//            }
//        }
//    }

//    def deleteDependentReviewedAnnotation(UserAnnotation ua, Transaction transaction, Task task = null) {
////        ReviewedAnnotation.findAllByParentIdent(ua.id).each {
////            reviewedAnnotationService.delete(it,transaction,null,false)
////        }
//    }

//    def deleteDependentSharedAnnotation(UserAnnotation ua, Transaction transaction, Task task = null) {
//        //TODO: we should implement a full service for sharedannotation and delete them if annotation is deleted
////        if(SharedAnnotation.findByUserAnnotation(ua)) {
////            throw new ConstraintException("There are some comments on this annotation. Cannot delete it!")
////        }
//
//        SharedAnnotation.findAllByAnnotationClassNameAndAnnotationIdent(ua.class.name, ua.id).each {
//            sharedAnnotationService.delete(it, transaction, null, false)
//        }
//
//    }
//
//    def annotationTrackService
//
//    def deleteDependentAnnotationTrack(UserAnnotation ua, Transaction transaction, Task task = null) {
//        AnnotationTrack.findAllByAnnotationIdent(ua.id).each {
//            annotationTrackService.delete(it, transaction, task)
//        }
//    }


}
