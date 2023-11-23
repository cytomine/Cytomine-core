package be.cytomine.domain;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.*;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.dto.AreaDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static be.cytomine.domain.social.PersistentUserPosition.getJtsPolygon;
import static be.cytomine.service.social.ProjectConnectionService.DATABASE_NAME;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class MongoDBDomainTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    PersistentConnectionRepository persistentConnectionRepository;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    MongoClient mongoClient;

    SimpleDateFormat mongoDBFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void cleanDB() {
        persistentProjectConnectionRepository.deleteAll();
        persistentConnectionRepository.deleteAll();
        persistentImageConsultationRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
        lastUserPositionRepository.deleteAll();
    }

    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void last_connection_indexes() throws ParseException, JsonProcessingException {
        ListIndexesIterable<Document> indexes = retrieveIndex("lastConnection");
        //> db.lastConnection.getIndexes()
        //[
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"_id" : 1
        //		},
        //		"name" : "_id_",
        //		"ns" : "cytomine.lastConnection"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"date" : 2
        //		},
        //		"name" : "date_2",
        //		"ns" : "cytomine.lastConnection",
        //		"expireAfterSeconds" : 300
        //	}
        //]
        Document indexId = null;
        Document indexUserDate = null;
        for (Document index : indexes) {
            if (index.get("name").equals("_id_")) {
                indexId = index;
            }
            if (index.get("name").equals("date_2")) {
                indexUserDate = index;
            }
        }
        assertThat(indexes).hasSize(2);
        assertThat(indexId).isNotNull();
        assertThat(((Document)indexId.get("key")).get("_id")).isEqualTo(1);
        assertThat(indexUserDate).isNotNull();
        assertThat(((Document)indexUserDate.get("key")).get("date")).isEqualTo(1); // 1 or 2
        assertThat(indexUserDate.get("expireAfterSeconds")).isEqualTo(300L);
    }
    
    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void last_connection_domain() throws ParseException, JsonProcessingException {
        // EXpected:
//        { "_id" : NumberLong(60657), "date" : ISODate("2022-02-02T07:30:23.384Z"), "created" : ISODate("2022-02-02T07:30:23.385Z"), "user" : NumberLong(58), "version" : NumberLong(0) }

        LastConnection lastConnection = new LastConnection();
        lastConnection.setId(60657L);
        lastConnection.setProject(null);
        lastConnection.setUser(58L);
        lastConnection.setCreated(mongoDBFormat.parse("2022-02-02T07:30:23.384Z"));
        lastConnection.setDate(mongoDBFormat.parse("2022-02-02T07:30:23.384Z"));
        lastConnection = lastConnectionRepository.insert(lastConnection);

        Document document = retrieveDocument("lastConnection", lastConnection.getId());

        // Result from grails version
        //{
        //	"_id": 60657,
        //	"date": {
        //		"$date": "2022-02-02T07:30:23.384Z"
        //	},
        //	"created": {
        //		"$date": "2022-02-02T07:30:23.385Z"
        //	},
        //	"user": 58,
        //	"version": 0
        //}

        String expectedResults =
                "{\n" +
                        "\t\"_id\": 60657,\n" +
                        "\t\"date\": {\n" +
                        "\t\t\"$date\": \"2022-02-02T06:30:23.384Z\"\n" + //UTC
                        "\t},\n" +
                        "\t\"created\": {\n" +
                        "\t\t\"$date\": \"2022-02-02T06:30:23.384Z\"\n" + //UTC
                        "\t},\n" +
                        "\t\"user\": 58,\n" +
                        "\t\"version\": 0\n" +
                        "}";


        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2022-02-02T06:30:23.384Z", "2022-02-02T07:30:23.384Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));

    }


    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void last_user_position_index() throws ParseException, JsonProcessingException {
        // Expected:
        //> db.lastUserPosition.getIndexes()
        //[
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"_id" : 1
        //		},
        //		"name" : "_id_",
        //		"ns" : "cytomine.lastUserPosition"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"user" : 1,
        //			"image" : 1,
        //			"slice" : 1,
        //			"created" : -1
        //		},
        //		"name" : "user_1_image_1_slice_1_created_-1",
        //		"ns" : "cytomine.lastUserPosition"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"location" : "2d",
        //			"image" : 1,
        //			"slice" : 1
        //		},
        //		"name" : "location_2d_image_1_slice_1",
        //		"ns" : "cytomine.lastUserPosition",
        //		"min" : -2147483648,
        //		"max" : 2147483647
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"created" : 1
        //		},
        //		"name" : "created_1",
        //		"ns" : "cytomine.lastUserPosition",
        //		"expireAfterSeconds" : 60
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"image" : 1
        //		},
        //		"name" : "image_1",
        //		"ns" : "cytomine.lastUserPosition"
        //	}
        //]
        ListIndexesIterable<Document> indexes = retrieveIndex("lastUserPosition");
        Document indexId = null;
        Document indexUserImageSliceCreated = null;
        Document locationImageSlice = null;
        Document created = null;
        Document image = null;
        for (Document index : indexes) {
            if (index.get("name").equals("_id_")) {
                indexId = index;
            }
            if (index.get("name").equals("user_1_image_1_slice_1_created_-1")) {
                indexUserImageSliceCreated = index;
            }

            if (index.get("name").equals("location_2d_image_1_slice_1")) {
                locationImageSlice = index;
            }

            if (index.get("name").equals("created_1")) {
                created = index;
            }

            if (index.get("name").equals("image_1")) {
                image = index;
            }
        }
        assertThat(indexes).hasSize(5);

        assertThat(indexId).isNotNull();
        assertThat(((Document)indexId.get("key")).get("_id")).isEqualTo(1);

        assertThat(indexUserImageSliceCreated).isNotNull();
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("user")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("image")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("slice")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("created")).isEqualTo(-1);

        assertThat(locationImageSlice).isNotNull();
        assertThat(((Document)locationImageSlice.get("key")).get("location")).isEqualTo("2d");
        assertThat(((Document)locationImageSlice.get("key")).get("image")).isEqualTo(1);
        assertThat(((Document)locationImageSlice.get("key")).get("slice")).isEqualTo(1);

        assertThat(created).isNotNull();
        assertThat(((Document)created.get("key")).get("created")).isEqualTo(1);
        assertThat(created.get("expireAfterSeconds")).isEqualTo(60L);

        assertThat(image).isNotNull();
        assertThat(((Document)image.get("key")).get("image")).isEqualTo(1);
    }

    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void last_user_position_domain() throws ParseException, JsonProcessingException {
        // Expected:
        //> db.lastUserPosition.find({}).sort({_id:-1}).limit(1);
        //        { "_id" : NumberLong(60911),
        //        "broadcast" : false,
        //        "created" : ISODate("2022-02-02T07:40:46.710Z"),
        //        "image" : NumberLong(29240),
        //        "imageName" : "CMU-1-Small-Region (1).svs",
        //        "location" : [ [ -109, 2548 ], [ 683, 2548 ], [ 683, 2028 ], [ -109, 2028 ] ],
        //        "project" : NumberLong(22782),
        //        "rotation" : "0.0",
        //        "slice" : NumberLong(29241),
        //        "user" : NumberLong(58),
        //        "zoom" : 5 }
        //

        LastUserPosition lastPosition = new LastUserPosition();
        lastPosition.setId(60911L);
        lastPosition.setBroadcast(false);
        lastPosition.setCreated(mongoDBFormat.parse("2022-02-02T07:40:46.710Z"));
        lastPosition.setImage(29240L);
        lastPosition.setImageName("CMU-1-Small-Region (1).svs");
        lastPosition.setLocation(new AreaDTO(
                new be.cytomine.service.dto.Point(-109d,2548d),
                new be.cytomine.service.dto.Point(683d,2548d),
                new be.cytomine.service.dto.Point(683d,2028d),
                new be.cytomine.service.dto.Point(-109d,2028d)
        ).toMongodbLocation().getCoordinates());
        lastPosition.setProject(22782L);
        lastPosition.setRotation(0d);
        lastPosition.setSlice(29241L);
        lastPosition.setUser(58L);
        lastPosition.setZoom(5);

        lastPosition = lastUserPositionRepository.insert(lastPosition);

        Document document = retrieveDocument("lastUserPosition", lastPosition.getId());

        //        {
        //	"_id": 60911,
        //	"broadcast": false,
        //	"created": {
        //		"$date": "2022-02-02T07:40:46.71Z"
        //	},
        //	"image": 29240,
        //	"imageName": "CMU-1-Small-Region (1).svs",
        //	"location": [
        //		[-109.0, 2548.0],
        //		[683.0, 2548.0],
        //		[683.0, 2028.0],
        //		[-109.0, 2028.0]
        //	],
        //	"project": 22782,
        //	"rotation": "0.0",
        //	"slice": 29241,
        //	"user": 58,
        //	"zoom": 5
        //}

        String expectedResults = "{\n" +
                "\"_id\": 60911,\n" +
                "\"broadcast\": false,\n" +
                "\"created\": {\n" +
                "\"$date\": \"2022-02-02T06:40:46.71Z\"\n" +//UTC
                "},\n" +
                "\"image\": 29240,\n" +
                "\"imageName\": \"CMU-1-Small-Region (1).svs\",\n" +
                "\"location\": [\n" +
                "[-109.0, 2548.0],\n" +
                "[683.0, 2548.0],\n" +
                "[683.0, 2028.0],\n" +
                "[-109.0, 2028.0]\n" +
                "],\n" +
                "\"project\": 22782,\n" +
                "\"rotation\": 0.0,\n" + // ???????????
                "\"slice\": 29241,\n" +
                "\"user\": 58,\n" +
                "\"zoom\": 5\n" +
                "}";

        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2022-02-02T06:40:46.71Z", "2022-02-02T07:40:46.71Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));
        // fails because in grails version, rotation is a string
    }

    @Test
    void peristent_connection_indexes() {

        ListIndexesIterable<Document> indexes = retrieveIndex("persistentConnection");
        // Expected
        //> db.persistentConnection.getIndexes()
        //                [
        //                {
        //                        "v" : 1,
        //                "key" : {
        //            "_id" : 1
        //        },
        //        "name" : "_id_",
        //                "ns" : "cytomine.persistentConnection"
        //	},
        //        {
        //            "v" : 1,
        //                "key" : {
        //            "user" : 1,
        //                    "created" : -1
        //        },
        //            "name" : "user_1_created_-1",
        //                "ns" : "cytomine.persistentConnection"
        //        }
        //]

        Document indexId = null;
        Document indexUserCreated = null;
        for (Document index : indexes) {
            if (index.get("name").equals("_id_")) {
                indexId = index;
            }
            if (index.get("name").equals("user_1_created_-1")) {
                indexUserCreated = index;
            }
        }
        assertThat(indexes).hasSize(2);
        assertThat(indexId).isNotNull();
        assertThat(((Document)indexId.get("key")).get("_id")).isEqualTo(1);
        assertThat(indexUserCreated).isNotNull();
        assertThat(((Document)indexUserCreated.get("key")).get("user")).isEqualTo(1);
        assertThat(((Document)indexUserCreated.get("key")).get("created")).isEqualTo(-1);
    }

    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void persistent_connection_domain() throws ParseException, JsonProcessingException {

        // Result from Mongodb with the Cytomine Grails version

//        > db.persistentConnection.findOne()
//        {
//                 "_id" : NumberLong(198),
//                "created" : ISODate("2021-09-15T14:21:41.204Z"),
//                "session" : "2D68BD4BE734DC4EFF20F9FB0EE5F3F7",
//                "user" : NumberLong(58),
//                "version" : NumberLong(0)
//        }

        PersistentConnection connection = new PersistentConnection();
        connection.setId(3073L);
        connection.setCreated(mongoDBFormat.parse("2021-09-22T09:06:32.472Z"));
        connection.setSession("B7850470EED8CD7570E05C50FD5F02F6");
        connection.setProject(null);

        connection = persistentConnectionRepository.insert(connection);

        Document document = retrieveDocument("persistentConnection", connection.getId());

        // check index
        // {
        //   "_id":3073,
        //   "created":{
        //      "$date":"2021-09-22T07:06:32.472Z"
        //   },
        //   "session":"B7850470EED8CD7570E05C50FD5F02F6"
        //}

        String expectedResults =
                "{\n" +
                        "   \"_id\":3073,\n" +
                        "   \"created\":{\n" +
                        "      \"$date\":\"2021-09-22T07:06:32.472Z\"\n" + //UTC
                        "   },\n" +
                        "   \"session\":\"B7850470EED8CD7570E05C50FD5F02F6\"\n" +
                        "}";


        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2021-09-22T07:06:32.472Z", "2021-09-22T09:06:32.472Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));
    }


    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void persistent_project_connection_domain() throws ParseException, JsonProcessingException {

        // Result from Mongodb with the Cytomine Grails version

//        > db.persistentProjectConnection.findOne()
//        {
//                 "_id" : NumberLong(3073),
//                "browser" : "firefox",
//                "browserVersion" : "92.0.0",
//                "created" : ISODate("2021-09-22T09:06:32.472Z"),
//                "os" : "Linux",
//                "project" : NumberLong(3063),
//                "session" : "B7850470EED8CD7570E05C50FD5F02F6",
//                "user" : NumberLong(58),
//                "countCreatedAnnotations" : 0,
//                "countViewedImages" : 0,
//                "time" : NumberLong(139164)
//        }

        PersistentProjectConnection connection = new PersistentProjectConnection();
        connection.setId(3073L);
        connection.setBrowser("firefox");
        connection.setBrowserVersion("92.0.0");
        connection.setCreated(mongoDBFormat.parse("2021-09-22T09:06:32.472Z"));
        connection.setOs("Linux");
        connection.setProject(3063L);
        connection.setSession("B7850470EED8CD7570E05C50FD5F02F6");
        connection.setUser(58L);
        connection.setCountCreatedAnnotations(0);
        connection.setCountViewedImages(0);
        connection.setTime(139164L);

        connection = persistentProjectConnectionRepository.insert(connection);

        Document document = retrieveDocument("persistentProjectConnection", connection.getId());

        // {"_id": 3073, "browser": "firefox", "browserVersion": "92.0.0", "created": {"$date": "2021-09-22T09:06:32.472Z"}, "os": "Linux", "project": 3063, "session": "B7850470EED8CD7570E05C50FD5F02F6", "user": 58, "countCreatedAnnotations": 0, "countViewedImages": 0, "time": 139164}

        String expectedResults =
                "{\n" +
                        "   \"_id\":3073,\n" +
                        "   \"browser\":\"firefox\",\n" +
                        "   \"browserVersion\":\"92.0.0\",\n" +
                        "   \"created\":{\n" +
                        "      \"$date\":\"2021-09-22T07:06:32.472Z\"\n" + //utc
                        "   },\n" +
                        "   \"os\":\"Linux\",\n" +
                        "   \"project\":3063,\n" +
                        "   \"session\":\"B7850470EED8CD7570E05C50FD5F02F6\",\n" +
                        "   \"user\":58,\n" +
                        "   \"countCreatedAnnotations\":0,\n" +
                        "   \"countViewedImages\":0,\n" +
                        "   \"time\":139164\n" +
                        "}";

        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2021-09-22T07:06:32.472Z","2021-09-22T09:06:32.472Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));
        // TODO: test index
    }


    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void persistent_image_consultation_domain() throws ParseException, JsonProcessingException {

        // Result from Mongodb with the Cytomine Grails version

//> db.persistentImageConsultation.findOne()
//        {
//            "_id" : NumberLong(3975),
//                "created" : ISODate("2021-09-23T08:55:02.602Z"),
//                "image" : NumberLong(3962),
//                "imageName" : "CMU-1-Small-Region (1).svs",
//                "imageThumb" : "http://localhost-core/api/imageinstance/3962/thumb.png?maxSize=256",
//                "mode" : "view",
//                "project" : NumberLong(3063),
//                "projectConnection" : NumberLong(3974),
//                "session" : "B6AC04394B9D9F746A15E511C5DC243B",
//                "user" : NumberLong(58),
//                "countCreatedAnnotations" : 0,
//                "time" : NumberLong(12149)
//        }


        PersistentImageConsultation consultation = new PersistentImageConsultation();
        consultation.setId(3975L);
        consultation.setUser(58L);
        consultation.setImage(3962L);
        consultation.setProject(3063L);
        consultation.setSession("B6AC04394B9D9F746A15E511C5DC243B");
        consultation.setProjectConnection(3974L);
        consultation.setCreated(mongoDBFormat.parse("2021-09-23T08:55:02.602Z"));
        consultation.setSession("B6AC04394B9D9F746A15E511C5DC243B");
        consultation.setMode("view");
        consultation.setImageName("CMU-1-Small-Region (1).svs");
        consultation.setImageThumb("http://localhost-core/api/imageinstance/3962/thumb.png?maxSize=256");
        consultation.setTime(12149L);
        consultation.setCountCreatedAnnotations(0);

        consultation = persistentImageConsultationRepository.insert(consultation);

        Document document = retrieveDocument("persistentImageConsultation", consultation.getId());

     // "_id": 3975, "created": {"$date": "2021-09-23T08:55:02.602Z"}, "image": 3962, "imageName": "CMU-1-Small-Region (1).svs", "imageThumb": "http://localhost-core/api/imageinstance/3962/thumb.png?maxSize=256", "mode": "view", "project": 3063, "projectConnection": 3974, "session": "B6AC04394B9D9F746A15E511C5DC243B", "user": 58, "countCreatedAnnotations": 0, "time": 12149}

        String expectedResults =
                "{\n" +
                        "\"_id\": 3975,\n" +
                        "\"created\": {\n" +
                        "\"$date\": \"2021-09-23T06:55:02.602Z\"\n" + //UTC
                        "},\n" +
                        "\"image\": 3962,\n" +
                        "\"imageName\": \"CMU-1-Small-Region (1).svs\",\n" +
                        "\"imageThumb\": \"http://localhost-core/api/imageinstance/3962/thumb.png?maxSize=256\",\n" +
                        "\"mode\": \"view\",\n" +
                        "\"project\": 3063,\n" +
                        "\"projectConnection\": 3974,\n" +
                        "\"session\": \"B6AC04394B9D9F746A15E511C5DC243B\",\n" +
                        "\"user\": 58,\n" +
                        "\"countCreatedAnnotations\": 0,\n" +
                        "\"time\": 12149\n" +
                        "}";

        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2021-09-23T06:55:02.602Z", "2021-09-23T08:55:02.602Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));
        // TODO: issue with Date: created seems to have issue with UTC

        // TODO: test index
    }

    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void persistent_user_position_index() throws ParseException, JsonProcessingException {

        //Test index
        //> db.persistentUserPosition.getIndexes()
        //[
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"_id" : 1
        //		},
        //		"name" : "_id_",
        //		"ns" : "cytomine.persistentUserPosition"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"user" : 1,
        //			"image" : 1,
        //			"slice" : 1,
        //			"created" : -1
        //		},
        //		"name" : "user_1_image_1_slice_1_created_-1",
        //		"ns" : "cytomine.persistentUserPosition"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"location" : "2d",
        //			"image" : 1,
        //			"slice" : 1
        //		},
        //		"name" : "location_2d_image_1_slice_1",
        //		"ns" : "cytomine.persistentUserPosition",
        //		"min" : -2147483648,
        //		"max" : 2147483647
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"image" : 1
        //		},
        //		"name" : "image_1",
        //		"ns" : "cytomine.persistentUserPosition"
        //	}
        //]
        ListIndexesIterable<Document> indexes = retrieveIndex("persistentUserPosition");
        Document indexId = null;
        Document indexUserImageSliceCreated = null;
        Document locationImageSlice = null;
        Document image = null;
        for (Document index : indexes) {
            if (index.get("name").equals("_id_")) {
                indexId = index;
            }
            if (index.get("name").equals("user_1_image_1_slice_1_created_-1")) {
                indexUserImageSliceCreated = index;
            }

            if (index.get("name").equals("location_2d_image_1_slice_1")) {
                locationImageSlice = index;
            }

            if (index.get("name").equals("image_1")) {
                image = index;
            }
        }
        assertThat(indexes).hasSize(4);

        assertThat(indexId).isNotNull();
        assertThat(((Document)indexId.get("key")).get("_id")).isEqualTo(1);

        assertThat(indexUserImageSliceCreated).isNotNull();
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("user")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("image")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("slice")).isEqualTo(1);
        assertThat(((Document)indexUserImageSliceCreated.get("key")).get("created")).isEqualTo(-1);

        assertThat(locationImageSlice).isNotNull();
        assertThat(((Document)locationImageSlice.get("key")).get("location")).isEqualTo("2d");
        assertThat(((Document)locationImageSlice.get("key")).get("image")).isEqualTo(1);
        assertThat(((Document)locationImageSlice.get("key")).get("slice")).isEqualTo(1);

        assertThat(image).isNotNull();
        assertThat(((Document)image.get("key")).get("image")).isEqualTo(1);
    }

    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void persistent_user_position_domain() throws ParseException, JsonProcessingException {
        // Result from Mongodb with the Cytomine Grails version

        //> db.persistentUserPosition.findOne()
        //        {
        //            "_id" : NumberLong(3977),
        //                "broadcast" : false,
        //                "created" : ISODate("2021-09-23T08:55:03.608Z"),
        //                "image" : NumberLong(3962),
        //                "imageName" : "CMU-1-Small-Region (1).svs",
        //                "location" : [
        //		[
        //            -3338,3128
        //		],
        //		[
        //            5558,3128
        //		],
        //		[
        //            5558,-160
        //		],
        //		[
        //            -3338,-160
        //		]
        //	],
        //            "project" : NumberLong(3063),
        //                "rotation" : "0.0",
        //                "session" : "B6AC04394B9D9F746A15E511C5DC243B",
        //                "slice" : NumberLong(3963),
        //                "user" : NumberLong(58),
        //                "zoom" : 2
        //        }
        ImageInstance imageInstance = new ImageInstance();
        imageInstance.setId(3962L);
        Project project = new Project();
        project.setId(3063L);
        SliceInstance sliceInstance = new SliceInstance();
        sliceInstance.setId(3963L);
        User user = new User();
        user.setId(58L);


        PersistentUserPosition lastPosition = new PersistentUserPosition();
        lastPosition.setId(3977L);
        lastPosition.setBroadcast(false);
        lastPosition.setCreated(mongoDBFormat.parse("2021-09-23T08:55:03.608Z"));
        lastPosition.setImage(imageInstance.getId());
        lastPosition.setImageName("CMU-1-Small-Region (1).svs");
        lastPosition.setLocation(new AreaDTO(
                new be.cytomine.service.dto.Point(-3338d,3128d),
                new be.cytomine.service.dto.Point(5558d,3128d),
                new be.cytomine.service.dto.Point(5558d,-160d),
                new be.cytomine.service.dto.Point(-3338d,-160d)
        ).toMongodbLocation().getCoordinates());
        lastPosition.setProject(project.getId());
        lastPosition.setRotation(0d);
        lastPosition.setSession("B6AC04394B9D9F746A15E511C5DC243B");
        lastPosition.setSlice(sliceInstance.getId());
        lastPosition.setUser(user.getId());
        lastPosition.setZoom(2);

        lastPosition = persistentUserPositionRepository.insert(lastPosition);

        Document document = retrieveDocument("persistentUserPosition", lastPosition.getId());

        MongoCollection<Document> persistentProjectConnectionFromGrails = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentUserPosition");
        ArrayList<Document> results = persistentProjectConnectionFromGrails.find(eq("_id", 3977L))
                .into(new ArrayList<>());
        for (Document result : results) {
            System.out.println(result.toJson());
        }

        String expectedResults =
                "{\n" +
                        "\"_id\": 3977,\n" +
                        "\"broadcast\": false,\n" +
                        "\"created\": {\n" +
                        "\"$date\": \"2021-09-23T06:55:03.608Z\"\n" + // //UTC
                        "},\n" +
                        "\"image\": 3962,\n" +
                        "\"imageName\": \"CMU-1-Small-Region (1).svs\",\n" +
                        "\"location\": [\n" +
                        "[-3338.0, 3128.0],\n" +
                        "[5558.0, 3128.0],\n" +
                        "[5558.0, -160.0],\n" +
                        "[-3338.0, -160.0]\n" +
                        "],\n" +
                        "\"project\": 3063,\n" +
                        "\"rotation\": 0.0,\n" +
                        "\"session\": \"B6AC04394B9D9F746A15E511C5DC243B\",\n" +
                        "\"slice\": 3963,\n" +
                        "\"user\": 58,\n" +
                        "\"zoom\": 2\n" +
                        "}";

        String expectedResultsAnotherTimeZone = expectedResults.replaceAll("2021-09-23T06:55:03.608Z", "2021-09-23T08:55:03.608Z");

        assertThat(objectMapper.readTree(document.toJson())).isIn(objectMapper.readTree(expectedResults), objectMapper.readTree(expectedResultsAnotherTimeZone));
       //Fails because rotation is a string
    }


    /**
     * Check that the format of MongoDB is the same as
     */
    @Test
    void annotation_action_indexes() throws ParseException, JsonProcessingException {
        ListIndexesIterable<Document> indexes = retrieveIndex("annotationAction");
        //[
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"_id" : 1
        //		},
        //		"name" : "_id_",
        //		"ns" : "cytomine.annotationAction"
        //	},
        //	{
        //		"v" : 1,
        //		"key" : {
        //			"user" : 1,
        //			"image" : 1,
        //			"created" : -1
        //		},
        //		"name" : "user_1_image_1_created_-1",
        //		"ns" : "cytomine.annotationAction"
        //	}
        //]
        Document indexId = null;
        Document indexUserDate = null;
        for (Document index : indexes) {
            if (index.get("name").equals("_id_")) {
                indexId = index;
            }
            if (index.get("name").equals("user_1_image_1_created_-1")) {
                indexUserDate = index;
            }
        }
        assertThat(indexes).hasSize(2);
        assertThat(indexId).isNotNull();
        assertThat(((Document)indexId.get("key")).get("_id")).isEqualTo(1);
        assertThat(indexUserDate).isNotNull();
        assertThat(((Document)indexUserDate.get("key")).get("user")).isEqualTo(1);
        assertThat(((Document)indexUserDate.get("key")).get("image")).isEqualTo(1);
        assertThat(((Document)indexUserDate.get("key")).get("created")).isEqualTo(-1);
    }


    private Document retrieveDocument(String collectionName, Long id) {
        MongoCollection<Document> persistentProjectConnection = mongoClient.getDatabase(DATABASE_NAME).getCollection(collectionName);

        List<Document> results = persistentProjectConnection.find(eq("_id", id))
                .into(new ArrayList<>());
        System.out.println("****************************");
        assertThat(results).hasSize(1);
        Document document = results.get(0);
        return document;
    }

    private ListIndexesIterable<Document> retrieveIndex(String collectionName) {
        MongoCollection<Document> persistentProjectConnection = mongoClient.getDatabase(DATABASE_NAME).getCollection(collectionName);
        return persistentProjectConnection.listIndexes();
    }


    // how to retrieve doc from grails version

    //        MongoClient mongoClient = MongoClients.create();
//        MongoCollection<Document> persistentProjectConnectionFromGrails = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentProjectConnection");
//        results = persistentProjectConnectionFromGrails.find(eq("_id", 3073L))
//                .into(new ArrayList<>());
//        for (Document result : results) {
//            System.out.println(result.toJson());
//        }
}
