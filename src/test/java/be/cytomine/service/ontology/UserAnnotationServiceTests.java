package be.cytomine.service.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.TestUtils;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AnnotationLight;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.AnnotationResult;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class UserAnnotationServiceTests {

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Test
    void get_userAnnotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotation).isEqualTo(userAnnotationService.get(userAnnotation.getId()));
    }

    @Test
    void get_unexisting_userAnnotation_return_null() {
        assertThat(userAnnotationService.get(0L)).isNull();
    }

    @Test
    void find_userAnnotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());
        assertThat(userAnnotation).isEqualTo(userAnnotationService.find(userAnnotation.getId()).get());
    }

    @Test
    void find_unexisting_userAnnotation_return_empty() {
        assertThat(userAnnotationService.find(0L)).isEmpty();
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
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        UserAnnotation a1 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("a"),user1,term1);
        UserAnnotation a2 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("b"),user1,term2);
        UserAnnotation a3 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("c"),user2,term1);
        UserAnnotation a4 = builder.given_a_user_annotation(sliceInstance, POLYGONES.get("d"),user2,term2);

        List<AnnotationResult> list;
        List<Long> ids;

        list = userAnnotationService.listIncluded(
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

        list = userAnnotationService.listIncluded(
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


        list = userAnnotationService.listIncluded(
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

        UserAnnotation a5 = builder.given_a_user_annotation(sliceInstance, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",builder.given_superadmin(),term2);

        list = userAnnotationService.listIncluded(
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
    void count_by_project() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        assertThat(userAnnotationService.countByProject(userAnnotation.getProject()))
                .isEqualTo(1);
        assertThat(userAnnotationService.countByProject(builder.given_a_project()))
                .isEqualTo(0);
    }

    @Test
    void count_by_project_with_date() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),-30),
                DateUtils.addDays(userAnnotation.getCreated(),30)))
                .isEqualTo(1);

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),-30),
                DateUtils.addDays(userAnnotation.getCreated(),-15)))
                .isEqualTo(0);

        assertThat(userAnnotationService.countByProject(
                userAnnotation.getProject(),
                DateUtils.addDays(userAnnotation.getCreated(),15),
                DateUtils.addDays(userAnnotation.getCreated(),30)))
                .isEqualTo(0);
    }

    @Test
    void list_all_lights() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        Optional<AnnotationLight> first = userAnnotationService.listLight()
                .stream().filter(x -> x.getId().equals(userAnnotation.getId())).findFirst();

        assertThat(first).isPresent();
        assertThat(first.get().getContainer()).isEqualTo(userAnnotation.getProject().getId());
    }

    @Test
    void add_valid_user_annotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_big_user_annotation_with_max_number_of_points() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/very_big_annotation.txt")));
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("maxPoint", 100);
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();
        assertThat(created.getLocation().getNumPoints()).isLessThanOrEqualTo(100);
    }

    @Test
    void add_too_small_user_annotation() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, " +
                "226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, " +
                "225.73582220103702 306.89723126347087))");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_multiline() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read(
                "LINESTRING( 181.05636403199998 324.87936288, 208.31216076799996 303.464094016)"
        ));
        JsonObject jsonObject = userAnnotation.toJsonObject();
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
        assertThat(((UserAnnotation)commandResponse.getObject()).getWktLocation())
                .isEqualTo("LINESTRING (181.05636403199998 324.87936288, 208.31216076799996 303.464094016)");
    }

    @Test
    void add_user_annotation_without_project() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.remove("project");
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200); // project is retrieve from image/slice

    }


    @Test
    void add_valid_user_annotation_with_terms() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        Term term1 = builder.given_a_term(userAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(userAnnotation.getProject().getOntology());

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = userAnnotationService.add(jsonObject);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(created);
        assertThat(created.getTerms()).hasSize(2);

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        assertThat(userAnnotationService.find(commandResponse.getObject().getId()).get().getTerms()).hasSize(2);
    }


    @Test
    void add_user_annotation_bad_geom() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POINT(BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_out_of_bounds() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        userAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON ((" +
                "-1 -1," +
                "-1 " + userAnnotation.getImage().getBaseImage().getHeight() + "," +
                userAnnotation.getImage().getBaseImage().getWidth()+5 + " "+userAnnotation.getImage().getBaseImage().getHeight()+","+
                userAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = userAnnotation.toJsonObject();
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation().toText())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        userAnnotation.getImage().getBaseImage().getWidth() + " "+userAnnotation.getImage().getBaseImage().getHeight()+", "+
                        userAnnotation.getImage().getBaseImage().getWidth() + " 0, " +
                        "0 0, " +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + "))");
    }

    @Test
    void add_user_annotation_empty_polygon() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POLYGON EMPTY");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
                userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_null_geometry() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.remove("location");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_slice_not_exists() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void add_user_annotation_slice_null_retrieve_reference_slice() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        CommandResponse commandResponse = userAnnotationService.add(jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200); //referenceSlice is taken
    }

    @Test
    void add_user_annotation_slice_null_and_image_not_exists_fail() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("slice", null);
        jsonObject.put("image", -1);
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }


    @Test
    void edit_valid_user_annotation_with_success() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        String oldLocation = "POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))";
        String newLocation = "POLYGON ((2107 2160, 2047 2074, 1983 2168, 2107 2160))";

        userAnnotation.setLocation(new WKTReader().read(oldLocation));
        builder.persistAndReturn(userAnnotation);
        CommandResponse commandResponse = userAnnotationService.update(userAnnotation,
                userAnnotation.toJsonObject().withChange(
                        "location", newLocation)
        );

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
        UserAnnotation edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getLocation().toText()).isEqualTo(newLocation);
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

        commandService.undo();

        edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(oldLocation);

        commandService.redo();

        edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
        AssertionsForClassTypes.assertThat(edited.getWktLocation()).isEqualTo(newLocation);

    }

    @Test
    void edit_user_annotation_out_of_bounds() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setLocation(new WKTReader().read("" +
                "POLYGON((" +
                "-1 -1," +
                "-1 " + userAnnotation.getImage().getBaseImage().getHeight() + "," +
                userAnnotation.getImage().getBaseImage().getWidth()+5 + " "+userAnnotation.getImage().getBaseImage().getHeight()+","+
                userAnnotation.getImage().getBaseImage().getWidth() + " 0," +
                "-1 -1))"));
        JsonObject jsonObject = userAnnotation.toJsonObject();
        CommandResponse commandResponse = userAnnotationService.update(userAnnotation, jsonObject);
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(((UserAnnotation)commandResponse.getObject()).getLocation())
                .isEqualTo("" +
                        "POLYGON ((" +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() + ", " +
                        userAnnotation.getImage().getBaseImage().getWidth() + " "+userAnnotation.getImage().getBaseImage().getHeight()+", "+
                        userAnnotation.getImage().getBaseImage().getWidth() + "0, " +
                        "0 0, " +
                        "0 " + userAnnotation.getImage().getBaseImage().getHeight() +"))");
    }


    @Test
    void edit_user_annotation_empty_polygon() throws ParseException {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("location", "POINT (BAD GEOMETRY)");
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            userAnnotationService.add(jsonObject);
        }) ;
    }

    @Test
    void delete_user_annotation_with_success() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();

        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());

        commandService.undo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());

        commandService.redo();

        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
    }


    @Test
    void delete_user_annotation_with_terms() {
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation();
        Term term1 = builder.given_a_term(userAnnotation.getProject().getOntology());
        Term term2 = builder.given_a_term(userAnnotation.getProject().getOntology());

        JsonObject jsonObject = userAnnotation.toJsonObject();
        jsonObject.put("term", List.of(term1.getId(), term2.getId()));

        CommandResponse commandResponse = userAnnotationService.add(jsonObject);

        commandResponse = userAnnotationService.delete((UserAnnotation)commandResponse.getObject(), null, null, true);

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());

    }


    //TODO: test repead when implemented








//
//
//    def testAnnotationIncludeFilterAlgoAnnotation() {
//
//
//
//        checkIncluded(slice, image,a1,a2,a3,a4,user1,user2,term1,term2)
//
//        def result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "pdf",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "csv",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        println result
//        assert 200 == result.code
//    }
//
//
//    public static def checkIncluded(
//            SliceInstance slice,
//            ImageInstance image,
//            AnnotationDomain a1,
//            AnnotationDomain a2,
//            AnnotationDomain a3,
//            AnnotationDomain a4,
//            SecUser user1,
//            SecUser user2,
//            Term term1,
//            Term term2) {
//
//        //tatic def listIncluded(String geometry, Long idImage, Long idUser,List<Long> terms,String username, String password) {
//        def result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
//        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
//
//        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user2.id, [term1.id,term2.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        assert !AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
//        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
//
//        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user2.id, [term1.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        assert !AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
//        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
//
//        UserAnnotation a5 = BasicInstanceBuilder.getUserAnnotationNotExist(slice, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",User.findByUsername(Infos.SUPERADMINLOGIN),term2)
//
//        result = AnnotationDomainAPI.listIncluded(a5, image.id, user1.id, [term1.id,term2.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
//        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
//        assert !AnnotationDomainAPI.containsInJSONList(a5.id,result.data)
//    }
//



//
//
//
//    @Test
//    void list_all_userAnnotation_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        assertThat(userAnnotation).isIn(userAnnotationService.fin());
//    }
//
//
//
//    @Test
//    void list_userAnnotation_by_ontology_include_userAnnotation_from_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        assertThat(userAnnotation).isIn(userAnnotationService.list(userAnnotation.getOntology()));
//    }
//
//    @Test
//    void list_userAnnotation_by_ontology_do_not_include_userAnnotation_from_other_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        Ontology ontology = builder.given_an_ontology();
//        assertThat(userAnnotationService.list(ontology).size()).isEqualTo(0);
//    }
//
//    @Test
//    void list_userAnnotation_by_project_include_userAnnotation_from_project_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        Project project = builder.given_a_project_with_ontology(userAnnotation.getOntology());
//        assertThat(userAnnotation).isIn(userAnnotationService.list(project));
//    }
//
//    @Test
//    void list_userAnnotation_by_project_do_not_include_userAnnotation_from_other_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        Project project = builder.given_a_project_with_ontology(builder.given_an_ontology());
//        assertThat(userAnnotationService.list(project)).asList().isEmpty();
//    }
//
//    @Test
//    void list_userAnnotation_by_project_return_empty_result_if_project_has_no_ontology() {
//        Project project = builder.given_a_project_with_ontology(null);
//        assertThat(userAnnotationService.list(project)).asList().isEmpty();
//    }
//
//
//    @Test
//    void list_userAnnotation_ids_by_project_include_userAnnotation_from_project_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        Project project = builder.given_a_project_with_ontology(userAnnotation.getOntology());
//        assertThat(userAnnotation.getId()).isIn(userAnnotationService.getAllUserAnnotationId(project));
//    }
//
//    @Test
//    void list_userAnnotation_ids_by_project_do_not_include_userAnnotation_from_other_ontology() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        Project project = builder.given_a_project_with_ontology(builder.given_an_ontology());
//        assertThat(userAnnotationService.getAllUserAnnotationId(project)).asList().doesNotContain(userAnnotation.getId());
//    }
//
//    @Test
//    void list_userAnnotation_ids_by_project_return_empty_result_if_project_has_no_ontology() {
//        Project project = builder.given_a_project_with_ontology(null);
//        assertThat(userAnnotationService.getAllUserAnnotationId(project)).asList().isEmpty();
//    }
//
//    @Test
//    void add_valid_userAnnotation_with_success() {
//        Ontology ontology = builder.given_an_ontology();
//        UserAnnotation userAnnotation = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(ontology);
//
//        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());
//
//        assertThat(commandResponse).isNotNull();
//        assertThat(commandResponse.getStatus()).isEqualTo(200);
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
//        UserAnnotation created = userAnnotationService.find(commandResponse.getObject().getId()).get();
//        assertThat(created.getName()).isEqualTo(userAnnotation.getName());
//        assertThat(created.getOntology()).isEqualTo(userAnnotation.getOntology());
//    }
//
//    @Test
//    void add_userAnnotation_with_null_ontology_fail() {
//        UserAnnotation userAnnotation = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(null);
//        Assertions.assertThrows(WrongArgumentException.class, () -> {
//            userAnnotationService.add(userAnnotation.toJsonObject());
//        });
//    }
//
//    @Test
//    void add_userAnnotation_with_null_color_fail() {
//        UserAnnotation userAnnotation = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(builder.given_an_ontology());
//        userAnnotation.setColor(null);
//        Assertions.assertThrows(WrongArgumentException.class, () -> {
//            userAnnotationService.add(userAnnotation.toJsonObject());
//        });
//    }
//
//    @Test
//    void undo_redo_userAnnotation_creation_with_success() {
//        UserAnnotation userAnnotation = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(builder.given_an_ontology());
//        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
//        System.out.println("id = " + commandResponse.getObject().getId() + " name = " + userAnnotation.getName());
//
//        commandService.undo();
//
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();
//
//        commandService.redo();
//
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
//
//    }
//
//    @Test
//    void redo_userAnnotation_creation_fail_if_userAnnotation_already_exist() {
//        UserAnnotation userAnnotation = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(builder.given_an_ontology());
//        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
//        System.out.println("id = " + commandResponse.getObject().getId() + " name = " + userAnnotation.getName());
//
//        commandService.undo();
//
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isEmpty();
//
//        UserAnnotation userAnnotationWithSameName = BasicInstanceBuilder.given_a_not_persisted_userAnnotation(userAnnotation.getOntology());
//        userAnnotationWithSameName.setName(userAnnotation.getName());
//        builder.persistAndReturn(userAnnotationWithSameName);
//
//        // re-create a userAnnotation with a name that already exist in this ontology
//        Assertions.assertThrows(AlreadyExistException.class, () -> {
//            commandService.redo();
//        });
//    }
//
//    @Test
//    void edit_valid_userAnnotation_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//
//        CommandResponse commandResponse = userAnnotationService.update(userAnnotation, userAnnotation.toJsonObject().withChange("name", "NEW NAME").withChange("color", "NEW COLOR"));
//
//        assertThat(commandResponse).isNotNull();
//        assertThat(commandResponse.getStatus()).isEqualTo(200);
//        assertThat(userAnnotationService.find(commandResponse.getObject().getId())).isPresent();
//        UserAnnotation edited = userAnnotationService.find(commandResponse.getObject().getId()).get();
//        assertThat(edited.getName()).isEqualTo("NEW NAME");
//        assertThat(edited.getColor()).isEqualTo("NEW COLOR");
//    }
//
//    @Test
//    void undo_redo_userAnnotation_edition_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        userAnnotation.setName("OLD NAME");
//        userAnnotation = builder.persistAndReturn(userAnnotation);
//
//        userAnnotationService.update(userAnnotation, userAnnotation.toJsonObject().withChange("name", "NEW NAME"));
//
//        assertThat(userAnnotationRepository.getById(userAnnotation.getId()).getName()).isEqualTo("NEW NAME");
//
//        commandService.undo();
//
//        assertThat(userAnnotationRepository.getById(userAnnotation.getId()).getName()).isEqualTo("OLD NAME");
//
//        commandService.redo();
//
//        assertThat(userAnnotationRepository.getById(userAnnotation.getId()).getName()).isEqualTo("NEW NAME");
//
//    }
//
//    @Test
//    void delete_userAnnotation_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//
//        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);
//
//        assertThat(commandResponse).isNotNull();
//        assertThat(commandResponse.getStatus()).isEqualTo(200);
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//    }
//
//    @Test
//    void delete_userAnnotation_with_dependencies_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        RelationUserAnnotation relationUserAnnotation = builder.given_a_relation_userAnnotation(userAnnotation, builder.given_a_user_annotation(userAnnotation.getOntology()));
//
//        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, null, null, true);
//
//        assertThat(commandResponse).isNotNull();
//        assertThat(commandResponse.getStatus()).isEqualTo(200);
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//    }
//
//
//    @Test
//    void undo_redo_userAnnotation_deletion_with_success() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//
//        userAnnotationService.delete(userAnnotation, null, null, true);
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//
//        commandService.undo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());
//
//        commandService.redo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//    }
//
//    @Test
//    void undo_redo_userAnnotation_deletion_restore_dependencies() {
//        UserAnnotation userAnnotation = builder.given_a_user_annotation();
//        RelationUserAnnotation relationUserAnnotation = builder.given_a_relation_userAnnotation(userAnnotation, builder.given_a_user_annotation(userAnnotation.getOntology()));
//        CommandResponse commandResponse = userAnnotationService.delete(userAnnotation, transactionService.start(), null, true);
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//        assertThat(relationUserAnnotationRepository.findById(relationUserAnnotation.getId())).isEmpty();
//
//        commandService.undo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());
//        assertThat(relationUserAnnotationRepository.findById(relationUserAnnotation.getId())).isPresent();
//
//        commandService.redo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//        assertThat(relationUserAnnotationRepository.findById(relationUserAnnotation.getId())).isEmpty();
//
//        commandService.undo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isPresent());
//        assertThat(relationUserAnnotationRepository.findById(relationUserAnnotation.getId())).isPresent();
//
//        commandService.redo();
//
//        assertThat(userAnnotationService.find(userAnnotation.getId()).isEmpty());
//        assertThat(relationUserAnnotationRepository.findById(relationUserAnnotation.getId())).isEmpty();
//    }
}
