package be.cytomine.api.controller.ontology;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.ontology.TrackService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestTrackController extends RestCytomineController {

    private final TrackService trackService;

    private final ImageInstanceRepository imageInstanceRepository;

    private final ProjectRepository projectRepository;

    @GetMapping("/project/{id}/track.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list tracks for project {}", id);
        return projectRepository.findById(id)
                .map( project -> responseSuccess(trackService.list(project)))
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
    }

    @GetMapping("/imageinstance/{id}/track.json")
    public ResponseEntity<String> listByImage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list tracks for imageinstance {}", id);
        return imageInstanceRepository.findById(id)
                .map( imageInstance -> responseSuccess(trackService.list(imageInstance)))
                .orElseThrow(() -> new ObjectNotFoundException("imageInstance", id));
    }


    // Not url mapping?
//    @GetMapping("/project/{idProject}/userannotation/count.json")
//    public ResponseEntity<String> countByProject(
//            @PathVariable(value = "idProject") Long idProject,
//            @RequestParam(value="startDate", required = false) Long startDate,
//            @RequestParam(value="endDate", required = false) Long endDate
//    ) {
//        log.debug("REST request to count user annotation by user/project");
//        Project project= projectService.find(idProject)
//                .orElseThrow(() -> new ObjectNotFoundException("Project", idProject));
//        Date start = (startDate!=null? new Date(startDate) : null);
//        Date end = (endDate!=null? new Date(endDate) : null);
//        return responseSuccess(JsonObject.of("total", userAnnotationService.countByProject(project, start, end)));
//    }
//


    @GetMapping("/track/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Track : {}", id);
        return trackService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Track", id));
    }


    /**
     * Add a new track
     * @param json JSON with Track data
     * @return Response map with .code = http response code and .data.track = new created Track
     */
    @PostMapping("/track.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save Track : " + json);
        return add(trackService, json);
    }

    /**
     * Update a track
     * @param id Track id
     * @param json JSON with the new Track data
     * @return Response map with .code = http response code and .data.newTrack = new created Track and  .data.oldTrack = old track value
     */
    @PutMapping("/track/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit Track : " + id);
        return update(trackService, json);
    }

    /**
     * Delete a track
     * @param id Track id
     * @return Response map with .code = http response code and .data.track = deleted track value
     */
    @DeleteMapping("/track/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete Track : " + id);
        return delete(trackService, JsonObject.of("id", id), null);
    }

}
