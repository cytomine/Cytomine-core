package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.api.controller.utils.RequestParams;
import be.cytomine.domain.ontology.AlgoAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.AlgoAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.ParamsService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAlgoAnnotationController extends RestCytomineController {

    private final AlgoAnnotationService algoAnnotationService;

    private final ProjectService projectService;

    private final SecUserService secUserService;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final ImageServerService imageServerService;

    private final ParamsService paramsService;

    /**
     * List all annotation (created by algo) visible for the current user
     */
    @GetMapping("/algoannotation.json")
    public ResponseEntity<String> list(
    ) throws IOException {
        log.debug("REST request to list algo annotation");
        List result = new ArrayList<>();
        List<String> propertyGroupToShow = paramsService.getPropertyGroupToShow(mergeQueryParamsAndBodyParams());
        for (Project project : projectService.listForCurrentUser()) {
            result.addAll(algoAnnotationService.list(project, propertyGroupToShow));
        }
        return responseSuccess(result);
    }


    @GetMapping("/algoannotation/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get algo annotation : {}", id);
        return algoAnnotationService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("AlgoAnnotation", id));
    }

    @GetMapping("/project/{idProject}/algoannotation/count.json")
    public ResponseEntity<String> countByProject(
            @PathVariable(value = "idProject") Long idProject,
            @RequestParam(value="startDate", required = false) Long startDate,
            @RequestParam(value="endDate", required = false) Long endDate
    ) {
        log.debug("REST request to count algo annotation by project");
        Project project= projectService.find(idProject)
                .orElseThrow(() -> new ObjectNotFoundException("Project", idProject));
        Date start = (startDate!=null? new Date(startDate) : null);
        Date end = (endDate!=null? new Date(endDate) : null);
        return responseSuccess(JsonObject.of("total", algoAnnotationService.countByProject(project, start, end)));
    }


    @PostMapping("/algoannotation.json")
    public ResponseEntity<String> add(
            @RequestBody JsonObject json,
            @RequestParam(required = false) Long minPoint,
            @RequestParam(required = false) Long maxPoint
    ) {
        log.debug("REST request to save algo annotation : " + json);
        json.putIfAbsent("minPoint", minPoint);
        json.putIfAbsent("maxPoint", maxPoint);
        return add(algoAnnotationService, json);
    }

    public CommandResponse addOne(ModelService service, JsonObject json) {
        if (json.isMissing("location")) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + json.get("location"));
        }
        RequestParams requestParam = retrieveRequestParam();
        json.putIfAbsent("minPoint", requestParam.get("minPoint"));
        json.putIfAbsent("maxPoint", requestParam.get("maxPoint"));
        return service.add(json);
    }


    @PutMapping("/algoannotation/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit algo annotation : " + id);
        //get annotation from DB
        AlgoAnnotation domain = (AlgoAnnotation)algoAnnotationService.retrieve(json);
        //update it thanks to JSON in request
        CommandResponse result = algoAnnotationService.update(domain,json);
        return responseSuccess(result);
    }

    @DeleteMapping("/algoannotation/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete an annotation : " + id);
        return delete(algoAnnotationService, JsonObject.of("id", id), null);
    }



    // TODO
//    @RestApiMethod(description="Download a report (pdf, xls,...) with software annotation data from a specific project")
//    @RestApiResponseObject(objectIdentifier =  "file")
//    @RestApiParams(params=[
//            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
//            @RestApiParam(name="terms", type="list", paramType = RestApiParamType.QUERY,description = "The annotation terms id (if empty: all terms)"),
//            @RestApiParam(name="users", type="list", paramType = RestApiParamType.QUERY,description = "The annotation users id (if empty: all users)"),
//            @RestApiParam(name="images", type="list", paramType = RestApiParamType.QUERY,description = "The annotation images id (if empty: all images)"),
//            @RestApiParam(name="afterThan", type="Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created before this date will not be returned"),
//            @RestApiParam(name="beforeThan", type="Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created after this date will not be returned"),
//            @RestApiParam(name="format", type="string", paramType = RestApiParamType.QUERY,description = "The report format (pdf, xls,...)"),
//            ])
//    def downloadDocumentByProject() {
//        Long afterThan = params.getLong('afterThan')
//        Long beforeThan = params.getLong('beforeThan')
//        reportService.createAnnotationDocuments(params.long('id'), params.terms, params.boolean("noTerm", false), params.boolean("multipleTerms", false),
//        params.users, params.images, afterThan, beforeThan, params.format, response, "ALGOANNOTATION")
//    }


    @RequestMapping(value = "/algoannotation/{id}/crop.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void crop(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean mask,
            @RequestParam(required = false) Boolean alphaMask,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality
    ) throws UnsupportedEncodingException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        AlgoAnnotation algoAnnotation = algoAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AlgoAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setMask(mask);
        cropParameter.setAlphaMask(alphaMask);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);

        responseByteArray(imageServerService.crop(algoAnnotation, cropParameter), format);
    }

    @RequestMapping(value = "/algoannotation/{id}/mask.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void cropMask(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality
    ) throws UnsupportedEncodingException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        AlgoAnnotation algoAnnotation = algoAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AlgoAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setMask(true);
        cropParameter.setAlphaMask(false);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);

        responseByteArray(imageServerService.crop(algoAnnotation, cropParameter), format);
    }

    @RequestMapping(value = "/algoannotation/{id}/alphamask.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void cropAlphaMask(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality
    ) throws UnsupportedEncodingException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        AlgoAnnotation algoAnnotation = algoAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AlgoAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setAlphaMask(true);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);

        responseByteArray(imageServerService.crop(algoAnnotation, cropParameter), format);
    }


    //TODO
//
//
//    @RestApiMethod(description="Add comment on an annotation to other user and send a mail to users")
//    @RestApiResponseObject(objectIdentifier = "empty")
//    @RestApiParams(params=[
//            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id"),
//            @RestApiParam(name="POST JSON: comment", type="string", paramType = RestApiParamType.QUERY,description = "The comment"),
//            @RestApiParam(name="POST JSON: sender", type="long", paramType = RestApiParamType.QUERY,description = "The user id who share the annotation"),
//            @RestApiParam(name="POST JSON: subject", type="string", paramType = RestApiParamType.QUERY,description = "The subject of the mail that will be send"),
//            @RestApiParam(name="POST JSON: from", type="string", paramType = RestApiParamType.QUERY,description = "The username of the user who send the mail"),
//            @RestApiParam(name="POST JSON: receivers", type="list", paramType = RestApiParamType.QUERY,description = "The list of user (id) to send the mail"),
//            @RestApiParam(name="POST JSON: emails", type="list", paramType = RestApiParamType.QUERY,required = false, description = "The list of emails to send the mail. Used (and mandatory) if receivers is null"),
//            @RestApiParam(name="POST JSON: annotationURL ", type="string", paramType = RestApiParamType.QUERY,description = "The URL of the annotation in the image viewer"),
//            @RestApiParam(name="POST JSON: shareAnnotationURL", type="string", paramType = RestApiParamType.QUERY,description = "The URL of the comment"),
//            ])
//    def addComment() {
//
//        AlgoAnnotation annotation = algoAnnotationService.read(params.getLong('annotation'))
//        def result = sharedAnnotationService.add(request.JSON, annotation, params)
//        if(result) {
//            responseResult(result)
//        }
//    }
//
//    /**
//     * Show a single comment for an annotation
//     */
//    //TODO : duplicate code from UserAnnotation
//    @RestApiMethod(description="Get a specific comment")
//    @RestApiParams(params=[
//            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id"),
//            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The comment id"),
//            ])
//    def showComment() {
//
//        AlgoAnnotation annotation = algoAnnotationService.read(params.long('annotation'))
//        if (!annotation) {
//            responseNotFound("Annotation", params.annotation)
//        }
//        def sharedAnnotation = SharedAnnotation.findById(params.long('id'))
//        if (sharedAnnotation) {
//            responseSuccess(sharedAnnotation)
//        } else {
//            responseNotFound("SharedAnnotation", params.id)
//        }
//    }
//
//    /**
//     * List all comments for an annotation
//     */
//    @RestApiMethod(description="Get all comments on annotation", listing=true)
//    @RestApiParams(params=[
//            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
//            ])
//    def listComments() {
//        AlgoAnnotation annotation = algoAnnotationService.read(params.long('annotation'))
//        if (annotation) {
//            responseSuccess(sharedAnnotationService.listComments(annotation))
//        } else {
//            responseNotFound("Annotation", params.id)
//        }
//    }
}
