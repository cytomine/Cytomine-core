package be.cytomine.service.ontology;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.TestUtils;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.security.UserJob;
import be.cytomine.dto.annotation.AnnotationResult;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.AlgoAnnotationListing;
import be.cytomine.repository.ontology.AlgoAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadminjob")
@Transactional
public class AlgoAnnotationServiceTests {

    @Autowired
    AlgoAnnotationService algoAnnotationService;

    @Autowired
    AlgoAnnotationRepository algoAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Test
    void get_algoAnnotation_with_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        assertThat(algoAnnotation).isEqualTo(algoAnnotationService.get(algoAnnotation.getId()));
    }

    @Test
    void get_unexisting_algoAnnotation_return_null() {
        assertThat(algoAnnotationService.get(0L)).isNull();
    }

    @Test
    void find_algoAnnotation_with_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        assertThat(algoAnnotationService.find(algoAnnotation.getId()).isPresent());
        assertThat(algoAnnotation).isEqualTo(algoAnnotationService.find(algoAnnotation.getId()).get());
    }

    @Test
    void find_unexisting_algoAnnotation_return_empty() {
        assertThat(algoAnnotationService.find(0L)).isEmpty();
    }

    @Test
    void count_by_project() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        assertThat(algoAnnotationService.countByProject(algoAnnotation.getProject()))
                .isEqualTo(1);
        assertThat(algoAnnotationService.countByProject(builder.given_a_project()))
                .isEqualTo(0);
    }

    @Test
    void count_by_project_with_date() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();

        assertThat(algoAnnotationService.countByProject(
                algoAnnotation.getProject(),
                DateUtils.addDays(algoAnnotation.getCreated(),-30),
                DateUtils.addDays(algoAnnotation.getCreated(),30)))
                .isEqualTo(1);

        assertThat(algoAnnotationService.countByProject(
                algoAnnotation.getProject(),
                DateUtils.addDays(algoAnnotation.getCreated(),-30),
                DateUtils.addDays(algoAnnotation.getCreated(),-15)))
                .isEqualTo(0);

        assertThat(algoAnnotationService.countByProject(
                algoAnnotation.getProject(),
                DateUtils.addDays(algoAnnotation.getCreated(),15),
                DateUtils.addDays(algoAnnotation.getCreated(),30)))
                .isEqualTo(0);
    }



    @Test
    void list_by_project() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        Optional<AnnotationResult> first = algoAnnotationService.list(algoAnnotation.getProject(), new ArrayList<>(AlgoAnnotationListing.availableColumnsDefault))
                .stream().filter(x -> ((AnnotationResult)x).get("id").equals(algoAnnotation.getId())).findFirst();
        assertThat(first).isPresent();
    }


    static Map<String, String> POLYGONES = Map.of(
            "a", "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))",
            "b", "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))",
            "c", "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))",
            "d", "POLYGON ((4 4,8 4, 8 7,4 7,4 4))",
            "e", "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
            //e intersect a,b and c
    );

    @Test
    void list_included() throws ParseException {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        UserJob user1 = builder.given_a_user_job();
        UserJob user2 = builder.given_a_user_job();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        AlgoAnnotation a1 = builder.given_a_algo_annotation(sliceInstance, POLYGONES.get("a"),user1,term1);
        AlgoAnnotation a2 = builder.given_a_algo_annotation(sliceInstance, POLYGONES.get("b"),user1,term2);
        AlgoAnnotation a3 = builder.given_a_algo_annotation(sliceInstance, POLYGONES.get("c"),user2,term1);
        AlgoAnnotation a4 = builder.given_a_algo_annotation(sliceInstance, POLYGONES.get("d"),user2,term2);

        List<AnnotationResult> list;
        List<Long> ids;

        list = algoAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user1,
                null,
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).doesNotContain(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        list = algoAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user2,
                List.of(term1.getId(), term2.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).doesNotContain(a1.getId());
        assertThat(ids).doesNotContain(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());


        list = algoAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user2,
                List.of(term1.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).doesNotContain(a1.getId());
        assertThat(ids).doesNotContain(a2.getId());
        assertThat(ids).contains(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());

        AlgoAnnotation a5 = builder.given_a_algo_annotation(sliceInstance, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",builder.given_superadmin_job(),term2);

        list = algoAnnotationService.listIncluded(
                sliceInstance.getImage(),
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
                user1,
                List.of(term1.getId(), term2.getId()),
                null,
                null
        );
        ids = list.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList());
        assertThat(ids).contains(a1.getId());
        assertThat(ids).contains(a2.getId());
        assertThat(ids).doesNotContain(a3.getId());
        assertThat(ids).doesNotContain(a4.getId());
        assertThat(ids).doesNotContain(a5.getId());
    }

    @Test
    void add_valid_algo_annotation_with_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        CommandResponse commandResponse = algoAnnotationService.add(algoAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        AlgoAnnotation created = algoAnnotationService.find(commandResponse.getObject().getId()).get();

        commandService.undo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_big_algo_annotation_with_max_number_of_points() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        algoAnnotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/very_big_annotation.txt")));
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("maxPoint", 100);
        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AlgoAnnotation created = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getLocation().getNumPoints()).isLessThanOrEqualTo(100);
    }

    @Test
    void add_too_small_algo_annotation() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, " +
                "226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, " +
                "225.73582220103702 306.89723126347087))");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_algo_annotation_multiline() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        algoAnnotation.setLocation(new WKTReader().read(
                "LINESTRING( 181.05636403199998 324.87936288, 208.31216076799996 303.464094016)"
        ));
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(((AlgoAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
        assertThat(((AlgoAnnotation)commandResponse.getObject()).getWktLocation())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
    }

    @Test
    void add_algo_annotation_without_project() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.remove("project");
        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200); // project is retrieve from image/slice

    }


    @Test
    void add_valid_algo_annotation_with_terms() {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        Term term1 = builder.given_a_term(algoAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(algoAnnotation.getProject().getOntology());

        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        AlgoAnnotation created = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(created);
        assertThat(created.terms()).hasSize(2);

        commandService.undo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        algoAnnotation = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(algoAnnotation);
        assertThat(algoAnnotation.terms()).hasSize(2);
    }


    @Test
    void add_algo_annotation_bad_geom() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("location", "POINT(BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_algo_annotation_out_of_bounds() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        algoAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON ((" +
                "-1 -1," +
                "-1 " + algoAnnotation.getImage().getBaseImage().getHeight() + "," +
                algoAnnotation.getImage().getBaseImage().getWidth()+5 + " "+algoAnnotation.getImage().getBaseImage().getHeight()+","+
                algoAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((AlgoAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + algoAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        algoAnnotation.getImage().getBaseImage().getWidth() + " "+algoAnnotation.getImage().getBaseImage().getHeight()+", "+
                        algoAnnotation.getImage().getBaseImage().getWidth() + " 0, " +
                        "0 0, " +
                        "0 " + algoAnnotation.getImage().getBaseImage().getHeight() + "))");
    }

    @Test
    void add_algo_annotation_empty_polygon() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON EMPTY");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
                algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_algo_annotation_null_geometry() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.remove("location");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_algo_annotation_slice_not_exists() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("slice", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_algo_annotation_slice_null_retrieve_reference_slice() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200); //referenceSlice is taken
    }

    @Test
    void add_algo_annotation_slice_null_and_image_not_exists_fail() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        jsonObject.put("image", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }


    @Test
    void edit_valid_algo_annotation_with_success() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        String oldLocation = "POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))";
        String newLocation = "POLYGON ((2107 2160, 2047 2074, 1983 2168, 2107 2160))";

        algoAnnotation.setLocation(new WKTReader().read(oldLocation));
        builder.persistAndReturn(algoAnnotation);
        CommandResponse commandResponse = algoAnnotationService.update(algoAnnotation,
                algoAnnotation.toJsonObject().withChange(
                        "location", newLocation)
        );

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(algoAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        AlgoAnnotation edited = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getLocation().toText()).isEqualTo(newLocation);
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

        commandService.undo();

        edited = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(oldLocation);

        commandService.redo();

        edited = algoAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

    }

    @Test
    void edit_algo_annotation_out_of_bounds() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        algoAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON((" +
                "-1 -1," +
                "-1 " + algoAnnotation.getImage().getBaseImage().getHeight() + "," +
                algoAnnotation.getImage().getBaseImage().getWidth()+5 + " "+algoAnnotation.getImage().getBaseImage().getHeight()+","+
                algoAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        CommandResponse commandResponse = algoAnnotationService.update(algoAnnotation, jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((AlgoAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + algoAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        algoAnnotation.getImage().getBaseImage().getWidth() + " "+algoAnnotation.getImage().getBaseImage().getHeight()+", "+
                        algoAnnotation.getImage().getBaseImage().getWidth() + " 0, " +
                        "0 0, " +
                        "0 " + algoAnnotation.getImage().getBaseImage().getHeight() +"))");
    }


    @Test
    void edit_algo_annotation_empty_polygon() throws ParseException {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("location", "POINT (BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            algoAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void delete_algo_annotation_with_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();

        CommandResponse commandResponse = algoAnnotationService.delete(algoAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(algoAnnotationService.find(algoAnnotation.getId()).isEmpty());

        commandService.undo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(algoAnnotation.getId()).isPresent());

        commandService.redo();

        AssertionsForClassTypes.assertThat(algoAnnotationService.find(algoAnnotation.getId()).isEmpty());
    }


    @Test
    void delete_algo_annotation_with_terms() {
        AlgoAnnotation algoAnnotation = builder.given_a_not_persisted_algo_annotation();
        Term term1 = builder.given_a_term(algoAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(algoAnnotation.getProject().getOntology());

        JsonObject jsonObject = algoAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = algoAnnotationService.add(jsonObject);

        commandResponse = algoAnnotationService.delete((AlgoAnnotation)commandResponse.getObject(), null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);

    }

    @Test
    void delete_algo_annotation_with_dependencies() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        algoAnnotationTerm.setAnnotation(algoAnnotation);

        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setParentClassName(algoAnnotation.getClass().getName());
        reviewedAnnotation.setParentIdent(algoAnnotation.getId());

        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();
        sharedAnnotation.setAnnotation(algoAnnotation);

        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        annotationTrack.setAnnotation(algoAnnotation);

        CommandResponse commandResponse = algoAnnotationService.delete(algoAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(algoAnnotationService.find(algoAnnotation.getId()).isEmpty());

        assertThat(entityManager.find(AlgoAnnotation.class, algoAnnotation.getId())).isNull();
        assertThat(entityManager.find(ReviewedAnnotation.class, reviewedAnnotation.getId())).isNull();
        assertThat(entityManager.find(SharedAnnotation.class, sharedAnnotation.getId())).isNull();
        assertThat(entityManager.find(AnnotationTrack.class, annotationTrack.getId())).isNull();
        assertThat(entityManager.find(AlgoAnnotationTerm.class, algoAnnotationTerm.getId())).isNull();

    }
}
