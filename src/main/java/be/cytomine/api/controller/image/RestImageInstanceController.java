package be.cytomine.api.controller.image;

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

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.api.controller.utils.RequestParams;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.InvalidRequestException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.dto.*;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.ImageSearchExtension;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.StringUtils;
import com.vividsolutions.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageInstanceController extends RestCytomineController {

    private final ProjectService projectService;

    private final ImageInstanceService imageInstanceService;

    private final ImageServerService imageServerService;

    private final SecUserService secUserService;
    
    private final SliceCoordinatesService sliceCoordinatesService;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;


    @GetMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);

        return imageInstanceService.find(id)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
    }

    @GetMapping("/user/{id}/imageinstance.json")
    public ResponseEntity<String> listByUser(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance by user {}", id);
        SecUser secUser = secUserService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SecUser", id));
        RequestParams requestParams = retrievePageableParameters();
        return responseSuccess(imageInstanceService.list(secUser, retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder(), requestParams.getOffset(), requestParams.getMax()));
    }

    @GetMapping("/user/{id}/imageinstance/light.json")
    public ResponseEntity<String> listLightByUser(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance light by user {}", id);
        SecUser secUser = currentUserService.getCurrentUser();
        if (id != 0) {
            secUser = secUserService.find(id)
                    .orElseThrow(() -> new ObjectNotFoundException("SecUser", id));
        }
        return responseSuccess(imageInstanceService.listLight(secUser));
    }

    @GetMapping("/project/{id}/imageinstance.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id,
            @RequestParam(value = "light", defaultValue = "false", required = false) Boolean light,
            @RequestParam(value = "tree", defaultValue = "false", required = false) Boolean tree,
            @RequestParam(value = "withLastActivity", defaultValue = "false", required = false) Boolean withLastActivity,
            @RequestParam(value = "sort", defaultValue = "created", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "desc", required = false) String order,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Integer offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Integer max

    ) {
        log.debug("REST request to list images for project : {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        RequestParams requestParams = retrievePageableParameters();
        if (light) {
            return responseSuccess(imageInstanceService.listLight(project), securityACLService.isFilterRequired(project));
        } else if (tree) {
            return responseSuccess(imageInstanceService.listTree(project, requestParams.getOffset(), requestParams.getMax()), securityACLService.isFilterRequired(project));
        } else if (withLastActivity) {
            ImageSearchExtension imageSearchExtension = new ImageSearchExtension();
            imageSearchExtension.setWithLastActivity(withLastActivity);
            return responseSuccess(imageInstanceService.listExtended(project, imageSearchExtension, retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder(), requestParams.getOffset(), requestParams.getMax()), securityACLService.isFilterRequired(project));
        } else {
            return responseSuccess(imageInstanceService.list(project, retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder(), requestParams.getOffset(), requestParams.getMax(), false), securityACLService.isFilterRequired(project));
        }
    }

    @GetMapping("/imageinstance/{id}/next.json")
    public ResponseEntity<String> next(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        return imageInstanceService.next(imageInstance)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseGet(() -> responseSuccess(new JsonObject()));
    }

    @GetMapping("/imageinstance/{id}/previous.json")
    public ResponseEntity<String> previous(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        return imageInstanceService.previous(imageInstance)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseGet(() -> responseSuccess(new JsonObject()));
    }

    @PostMapping("/imageinstance.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save imageinstance : " + json);
        return add(imageInstanceService, json);
    }

    @PutMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit imageinstance : " + id);
        return update(imageInstanceService, json);
    }

    @DeleteMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete imageinstance : " + id);
        return delete(imageInstanceService, JsonObject.of("id", id), null);
    }

    //    // TODO:MIGRATION GET params vs POST params!
    @RequestMapping(value = "/imageinstance/{id}/thumb.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void thumb(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "512", required = false) Integer maxSize,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits

    ) throws IOException {
        log.debug("REST request get imageinstance {} thumb {}", id, format);
        ImageParameter thumbParameter = new ImageParameter();
        thumbParameter.setFormat(format);
        thumbParameter.setMaxSize(maxSize);
        thumbParameter.setColormap(colormap);
        thumbParameter.setInverse(inverse);
        thumbParameter.setContrast(contrast);
        thumbParameter.setGamma(gamma);
        thumbParameter.setMaxBits(bits!=null && bits.equals("max"));
        thumbParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        responseImage(imageServerService.thumb(imageInstance, thumbParameter, etag));
    }


    @RequestMapping(value = "/imageinstance/{id}/preview.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void preview(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "1024", required = false) Integer maxSize,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits

    ) throws IOException {
        log.debug("REST request get imageInstance {} preview {}", id, format);
        ImageParameter previewParameter = new ImageParameter();
        previewParameter.setFormat(format);
        previewParameter.setMaxSize(maxSize);
        previewParameter.setColormap(colormap);
        previewParameter.setInverse(inverse);
        previewParameter.setContrast(contrast);
        previewParameter.setGamma(gamma);
        previewParameter.setMaxBits(bits!=null &&  bits.equals("max"));
        previewParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);

        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        responseImage(imageServerService.thumb(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), previewParameter, etag));
    }


    @GetMapping("/imageinstance/{id}/associated.json")
    public ResponseEntity<String> associated(@PathVariable Long id) throws IOException {
        log.debug("REST request to get available associated images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.associated(imageInstance));
    }



    @GetMapping("/imageinstance/{id}/histogram.json")
    public ResponseEntity<String> histogram(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get histogram images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.imageHistogram(imageInstance.getBaseImage(), nBins));
    }

    @GetMapping("/imageinstance/{id}/histogram/bounds.json")
    public ResponseEntity<String> histogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get bounds images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.imageHistogramBounds(imageInstance.getBaseImage()));
    }

    @GetMapping("/imageinstance/{id}/channelhistogram.json")
    public ResponseEntity<String> channelHistograms(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get channelhistogram images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.channelHistograms(imageInstance.getBaseImage(), nBins));
    }


    @GetMapping("/imageinstance/{id}/channelhistogram/bounds.json")
    public ResponseEntity<String> channelHistogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get channelHistogramBounds images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.channelHistogramBounds(imageInstance.getBaseImage()));
    }






    @RequestMapping(value = "/imageinstance/{id}/associated/{label}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void label(
            @PathVariable Long id,
            @PathVariable String label,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize) throws IOException {
        log.debug("REST request to get associated image of a abstract image");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        if(securityACLService.isFilterRequired(imageInstance.getProject())){
            throw new ForbiddenException("You don't have the right to read or modify this resource! "  + imageInstance.getClass().toString() + " " + id);
        }
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setFormat(format);
        labelParameter.setLabel(label);
        labelParameter.setMaxSize(maxSize);
        String etag = getRequestETag();
        responseImage(imageServerService.label(imageInstance, labelParameter, etag));
    }
    //
    @RequestMapping(value = "/imageinstance/{id}/crop.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void crop(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            //@RequestParam(required = false) String boundaries,
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
    ) throws IOException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
//        cropParameter.setBoundaries(boundaries);
        cropParameter.setMaxSize(maxSize);
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
        String etag = getRequestETag();
        responseImage(imageServerService.crop(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), cropParameter, etag));
    }

    @RequestMapping(value = "/imageinstance/{id}/window_url-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> windowUrl(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,
            @RequestParam(defaultValue = "false", required = false) Boolean withExterior
    ) throws UnsupportedEncodingException, ParseException {
        log.debug("REST request get imageInstance {} window url {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(withExterior);
        windowParameter.setFormat(format);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        String url = imageServerService.windowUrl(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter);
        return responseSuccess(JsonObject.of("url", url));
    }

    @RequestMapping(value = "/imageinstance/{id}/window-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void window(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,
            @RequestParam(defaultValue = "false", required = false) Boolean withExterior
    ) throws IOException, ParseException {
        log.debug("REST request get imageInstance {} window {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(withExterior);
        windowParameter.setFormat(format);

        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        responseImage(imageServerService.window(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter, etag));
    }

    @RequestMapping(value = "/imageinstance/{id}/camera_url-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> cameraUrl(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h
    ) throws UnsupportedEncodingException, ParseException {
        log.debug("REST request get imageInstance {} camera url {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(false);
        windowParameter.setFormat(format);
        // TODO : should we handle other window parameters?
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        String url = imageServerService.windowUrl(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter);
        return responseSuccess(JsonObject.of("url", url));
    }

    @RequestMapping(value = "/imageinstance/{id}/camera-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public void camera(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h
    ) throws IOException, ParseException {
        log.debug("REST request get imageInstance {} camera {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(false);
        windowParameter.setFormat(format);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        // TODO : should we handle other window parameters?
        String etag = getRequestETag();
        responseImage(imageServerService.window(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter, etag));
    }

    @GetMapping("/imageinstance/{id}/download")
    public RedirectView download(@PathVariable Long id) throws IOException {
        log.debug("REST request to download image instance");
        ImageInstance imageinstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        boolean canDownload = imageinstance.getProject().getAreImagesDownloadable();
        if(!canDownload) {
            // TODO: in abstract image, there is no check for that ?!?
            securityACLService.checkIsAdminContainer(imageinstance.getProject());
        }


        String url = imageServerService.downloadUri(imageinstance.getBaseImage());
        return new RedirectView(url);
    }

    @GetMapping("/imageinstance/{id}/sliceinstance/reference.json")
    public ResponseEntity<String> getReferenceSlice(
            @PathVariable Long id
    ) {
        log.debug("REST request get reference sliceinstance for imageinstance {}", id);
        SliceInstance sliceInstance = imageInstanceService.getReferenceSlice(id);
        if (sliceInstance != null) {
            return responseSuccess(sliceInstance);
        } else {
            return responseNotFound("SliceInstance", "ImageInstance", id);
        }
    }

    @GetMapping("/imageinstance/{id}/metadata.json")
    public ResponseEntity<String> metadata(
            @PathVariable Long id
    ) {
        log.debug("REST request get metadata for imageinstance {}", id);
        ImageInstance imageinstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        //responseSuccess(propertyService.list(imageinstance.getBaseImage()));
        //TODO
        throw new CytomineMethodNotYetImplementedException("");
    }
    
    // TODO

    @GetMapping("/project/{projectId}/bounds/imageinstance.json")
    public ResponseEntity<String> bounds(
            @PathVariable Long projectId
    ) {
        log.debug("REST request get bouds for imageinstance in project {}", projectId);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(JsonObject.toJsonString(imageInstanceService.computeBounds(project)));
    }


    /**
     * Clone an image instance and (optional) its metadata and all annotations in another project
     */
    @PostMapping("/imageinstance/{id}/clone.json")
    public ResponseEntity<String> cloneImage(
            @PathVariable Long id
    ) throws IOException, ClassNotFoundException {
        log.debug("REST request to clone image {}", id);
        JsonObject jsonObject = mergeQueryParamsAndBodyParams();
        ImageInstance imageinstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        Project project = projectService.find(jsonObject.getJSONAttrLong("idProject"))
                .orElseThrow(() -> new ObjectNotFoundException("Project", jsonObject.getJSONAttrLong("idProject")));

        List<Long> usersArray = new ArrayList<>();
        if (StringUtils.isNotBlank(jsonObject.getJSONAttrStr("layersArray"))) {
            usersArray = jsonObject.getJSONAttrListLong("layersArray", new ArrayList<>());
        }
        List<Map<String, Object>> annotationsTranfertMap = jsonObject.getJSONAttrListMap("annotationsTranfertMap", new ArrayList<>());

        CommandResponse commandResponse = imageInstanceService.cloneImage(
                imageinstance,
                project,
                jsonObject.getJSONAttrBoolean("areImageMetadataCopied", false),
                jsonObject.getJSONAttrBoolean("areAnnotationsCopied", false),
                jsonObject.getJSONAttrBoolean("areAnnotationsMetadataCopied", false),
                usersArray,
                jsonObject.getJSONAttrBoolean("areAllLayersCopiedOnCurrentUserLayer", false),
                jsonObject.getJSONAttrBoolean("areMissingLayersCopiedOnCurrentUserLayer", false),
                annotationsTranfertMap
        );

        return responseSuccess(commandResponse);
    }


//    protected void processElementsBeforeRendering(JsonObject jsonObject) {
//        boolean filterEnabled = false;
//        RequestParams requestParams = retrieveRequestParam();
//
//        if (requestParams.containsKey("project")) {
//            Project project = projectService.find(Long.parseLong(requestParams.get("id")))
//                    .orElseThrow(() -> new ObjectNotFoundException("Project",requestParams.get("id" )));
//            filterEnabled = project.getBlindMode();
//        } else if(requestParams.containsKey("id")) { //TODO!!! WTF??? && !(["windowUrl", "cameraUrl", "getReferenceSlice"].contains(params.action.GET))
////            project = ImageInstance.read(params.long("id"))?.project
////            if(project) filterEnabled = project.blindMode
//        }
//    }


    protected void filterOneElement(Map<String, Object> element) {
        element.put("instanceFilename", null);
        element.put("filename", null);
        element.put("originalFilename", null);
        element.put("path", null);
        element.put("fullPath", null);
    }

}
