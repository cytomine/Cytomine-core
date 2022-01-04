package be.cytomine.api.controller.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.api.ApiExceptionHandler;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.dto.WindowParameter;
import be.cytomine.service.image.ImagePropertiesService;
import be.cytomine.utils.JsonObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AbstractImageResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAbstractImageControllerMockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

//    @Autowired
//    private RestAbstractImageController restAbstractImageController;

    private static WireMockServer wireMockServer = new WireMockServer(8888);


//    @BeforeEach
//    public void initMock() {
//        if(restAbstractImageControllerMockMvc==null) {
//            restAbstractImageControllerMockMvc = MockMvcBuilders.standaloneSetup(restAbstractImageController)
//                    .setControllerAdvice(new ApiExceptionHandler())
//                    .build();
//        }
//    }



    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {}
    }

    @Test
    @Transactional
    public void list_abstract_image_by_width() throws Exception {
        AbstractImage img500Width = builder.given_an_abstract_image();
        img500Width.setWidth(500);
        img500Width = builder.persistAndReturn(img500Width);
        AbstractImage img501Width = builder.given_an_abstract_image();
        img501Width.setWidth(501);
        img501Width = builder.persistAndReturn(img501Width);


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]", "501"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[gte]", "500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]", String.valueOf(Integer.MAX_VALUE)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[gte]","501"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[lte]","500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[equals]","500"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("width[equals]","100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=="+img500Width.getId()+")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=="+img501Width.getId()+")]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_abstract_image_by_projects() throws Exception {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        AbstractImage imageInProject1 = builder.given_an_abstract_image();
        AbstractImage imageInProject2 = builder.given_an_abstract_image();
        AbstractImage imageInProject1And2 = builder.given_an_abstract_image();

        builder.given_an_image_instance(imageInProject1, project1);
        builder.given_an_image_instance(imageInProject2, project2);
        builder.given_an_image_instance(imageInProject1And2, project1);
        builder.given_an_image_instance(imageInProject1And2, project2);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(project1.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(project2.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")]").exists());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("project", String.valueOf(anotherProject.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject2.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageInProject1And2.getId() + ")]").doesNotExist());

    }

    @Test
    @Transactional
    public void list_abstract_image_with_ordering() throws Exception {
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        AbstractImage image1 = builder.given_an_abstract_image();
        image1.setWidth(width);
        AbstractImage image2 = builder.given_an_abstract_image();
        image2.setWidth(width);
        AbstractImage image3 = builder.given_an_abstract_image();
        image3.setWidth(width);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")// default sorting must be created desc
                        .param("width[equals]", String.valueOf(width)))

                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "id")
                        .param("order", "asc")
                        .param("width[equals]", String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(image1.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image3.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "id")
                        .param("order", "desc")
                        .param("width[equals]", String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));

        image1.setWidth(100);
        builder.persistAndReturn(image1);
        image2.setWidth(200);
        builder.persistAndReturn(image1);
        image3.setWidth(150);
        builder.persistAndReturn(image1);

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("sort", "width")
                        .param("order", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> map = JsonObject.toMap(mvcResult.getResponse().getContentAsString());
        List<Map<String, Object>> objectList = (List<Map<String, Object>>)map.get("collection");
        List<Long> ids = objectList.stream().map(x -> Long.valueOf((int)x.get("id"))).collect(Collectors.toList());

        assertThat(ids.indexOf(image2.getId())).isLessThan(ids.indexOf(image3.getId()));
        assertThat(ids.indexOf(image3.getId())).isLessThan(ids.indexOf(image1.getId()));


    }



    @Test
    @Transactional
    public void list_abstract_image_with_pagination() throws Exception {
        int width = Math.abs(new Random().nextInt());
        // we add width filter to get only the image set defined in this test
        AbstractImage image1 = builder.given_an_abstract_image();
        image1.setWidth(width);
        AbstractImage image2 = builder.given_an_abstract_image();
        image2.setWidth(width);
        AbstractImage image3 = builder.given_an_abstract_image();
        image3.setWidth(width);

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "0")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "1")
                        .param("max", "1")
                        .param("width[equals]",  String.valueOf(width))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "1")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "0")
                        .param("max", "500")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")
                        .param("offset", "500")
                        .param("max", "0")
                        .param("width[equals]", String.valueOf(width)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
                .andExpect(jsonPath("$.offset").value(500))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }


    @Test
    @Transactional
    public void get_an_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.AbstractImage"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.preview").value("http://localhost:8080/api/abstractimage/"+image.getId()+"/thumb.png?maxSize=1024"))
                .andExpect(jsonPath("$.thumb").value("http://localhost:8080/api/abstractimage/"+image.getId()+"/thumb.png?maxSize=512"))
                .andExpect(jsonPath("$.macroURL").value("http://localhost:8080/api/abstractimage/"+image.getId()+"/associated/macro.png?maxWidth=512"))
                .andExpect(jsonPath("$.path").value("/data/images/"+builder.given_superadmin().getId()+"/1636379100999/CMU-2/CMU-2.mrxs"))
                .andExpect(jsonPath("$.contentType").value("openslide/mrxs"))
                .andExpect(jsonPath("$.height").value(220696))
                .andExpect(jsonPath("$.width").value(109240))
                .andExpect(jsonPath("$.physicalSizeY").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeX").hasJsonPath())
                .andExpect(jsonPath("$.physicalSizeZ").hasJsonPath())
                .andExpect(jsonPath("$.fps").hasJsonPath())
                .andExpect(jsonPath("$.zoom").hasJsonPath())
                .andExpect(jsonPath("$.filename").hasJsonPath())
                .andExpect(jsonPath("$.dimensions").hasJsonPath());
    }


    @Test
    @Transactional
    public void get_an_abstract_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    @Transactional
    public void get_an_abstract_image_from_uploaded_file() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/uploadedfile/{id}/abstractimage.json", image.getUploadedFile().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()));
    }


    @Test
    @Transactional
    public void add_valid_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_a_not_persisted_abstract_image();
        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(abstractImage.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        JsonObject jsonObject = abstractImage.toJsonObject();
        jsonObject.put("width", 999);
        restAbstractImageControllerMockMvc.perform(put("/api/abstractimage/{id}.json", abstractImage.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditAbstractImageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists())
                .andExpect(jsonPath("$.abstractimage.width").value(999));


    }


    @Test
    @Transactional
    public void delete_abstract_image() throws Exception {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        restAbstractImageControllerMockMvc.perform(delete("/api/abstractimage/{id}.json", abstractImage.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.abstractimageID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteAbstractImageCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.abstractimage.id").exists());


    }


    @Test
    @Transactional
    public void list_unused_abstract_image() throws Exception {
        AbstractImage imageWithImageInstance = builder.given_an_image_instance().getBaseImage();
        AbstractImage imageWithoutImageInstance = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/unused.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageWithImageInstance.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id==" + imageWithoutImageInstance.getId() + ")]").exists());

    }

    @Test
    @Transactional
    public void get_abstract_image_uploader() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/user.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(builder.given_superadmin().getId()));
    }

    @Test
    @Transactional
    public void get_abstract_image_uploader_for_abstract_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/user.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void get_abstract_image_thumb() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0,0,0);
        slice.setUploadedFile(image.getUploadedFile());
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512"))
                .willReturn(
                        aResponse().withBody(new byte[]{0,1,2,3})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/thumb.png?maxSize=512", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3});
    }

    @Test
    @Transactional
    public void get_abstract_image_thumb_if_image_not_exist() throws Exception {
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/thumb.png", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    public void get_abstract_image_preview() throws Exception {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0,0,0);
        slice.setUploadedFile(image.getUploadedFile());
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=1024"))
                .willReturn(
                        aResponse().withBody(new byte[]{0,1,2,3})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/preview.png?maxSize=1024", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3});
    }


    @Test
    @Transactional
    public void get_abstract_image_associeted_label() throws Exception {
        AbstractImage image = given_test_abstract_image();
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/associated.json?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs"))
                .willReturn(
                        aResponse().withBody("[\"macro\",\"thumbnail\",\"label\"]")
                )
        );
        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/associated.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection", containsInAnyOrder("label","macro","thumbnail")));
    }


    @Test
    @Transactional
    public void get_abstract_image_associeted_label_macro() throws Exception {
        AbstractImage image = given_test_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");
        configureFor("localhost", 8888);

        stubFor(get(urlEqualTo("/image/nested.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId()+ "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512&label=macro"))
                .willReturn(
                        aResponse().withBody(new byte[]{0,1,2,3,4})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/associated/{label}.png", image.getId(), "macro")
                        .param("maxSize", "512"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{0,1,2,3,4});
    }


    @Test
    @Transactional
    public void get_abstract_image_crop() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);


        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(new byte[]{99})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/crop.png", image.getId())
                        .param("location", "POLYGON((1 1,50 10,50 50,10 50,1 1))"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{99});
    }

    @Test
    @Transactional
    public void get_abstract_image_window() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);
        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(new byte[]{123})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/window-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{123});


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/window_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/slice/crop.jpg?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop"))
                .andExpect(status().isOk());

    }


    @Test
    @Transactional
    public void get_abstract_image_camera() throws Exception {
        AbstractImage image = given_test_abstract_image();

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);
        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(new byte[]{123})
                )
        );

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/camera-10-20-30-40.png", image.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getContentAsByteArray()).isEqualTo(new byte[]{123});


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/camera_url-10-20-30-40.jpg", image.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.url").value("http://localhost:8888/slice/crop.jpg?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop"))
                .andExpect(status().isOk());

    }

    private AbstractImage given_test_abstract_image() {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");
        return image;
    }

    @Test
    public void download_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();

        MvcResult mvcResult = restAbstractImageControllerMockMvc.perform(get("/api/abstractimage/{id}/download", image.getId()))
                .andDo(print()).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        assertThat(mvcResult.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F"+builder.given_superadmin().getId()+"%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");


    }


    @Test
    public void clear_properties_from_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();
        builder.given_a_property(image, "cytomine.width", "value1");
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isPresent();
        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/clear.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isEmpty();

    }


    @Test
    public void extract_populated_properties_to_abstract_image() throws Exception {
        AbstractImage image = given_test_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");


        image.setWidth(1);
        image.setPhysicalSizeX(2d);
        image.setColorspace("empty");

        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/properties.json?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs"))
                .willReturn(
                        aResponse().withBody("{\"File.BitsPerSample\":\"8\",\"File.ColorComponents\":\"3\",\"" +
                                "File.Comment\":\"Intel(R) JPEG Library, version 1,5,4,36\",\"File.EncodingProcess\":\"Baseline DCT, Huffman coding\"," +
                                "\"File.ImageHeight\":\"1724\",\"File.ImageWidth\":\"854\",\"File.YCbCrSubSampling\":\"YCbCr4:2:2 (2 1)\"," +
                                "\"JFIF.JFIFVersion\":\"1.01\",\"JFIF.ResolutionUnit\":\"None\",\"JFIF.XResolution\":\"1\",\"JFIF.YResolution\":\"1\"," +
                                "\"cytomine.mimeType\":\"image/jpeg\",\"cytomine.extension\":\"mrxs\",\"cytomine.format\":\"JPEGFormat\",\"cytomine.width\":854," +
                                "\"cytomine.height\":1724,\"cytomine.bitPerSample\":8,\"cytomine.samplePerPixel\":3}")
                )
        );

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/populate.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isPresent();
        AssertionsForClassTypes.assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width").get().getValue()).isEqualTo("854");

        restAbstractImageControllerMockMvc.perform(post("/api/abstractimage/{id}/properties/extract.json", image.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(image.getWidth()).isEqualTo(854);
        assertThat(image.getHeight()).isEqualTo(1724);
        assertThat(image.getColorspace()).isEqualTo("empty");
    }

}
