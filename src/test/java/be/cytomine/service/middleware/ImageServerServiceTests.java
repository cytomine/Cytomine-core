package be.cytomine.service.middleware;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.AbstractAuthorizationTest;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.*;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.vividsolutions.jts.io.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImageServerServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageServerService imageServerService;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {
        }
    }

    @Test
    void get_imageServer_with_success() {
        ImageServer imageServer = builder.given_an_image_server();
        assertThat(imageServer).isEqualTo(imageServerService.get(imageServer.getId()));
    }

    @Test
    void get_unexisting_imageServer_return_null() {
        assertThat(imageServerService.get(0L)).isNull();
    }

    @Test
    void find_imageServer_with_success() {
        ImageServer imageServer = builder.given_an_image_server();
        assertThat(imageServerService.find(imageServer.getId()).isPresent());
        assertThat(imageServer).isEqualTo(imageServerService.find(imageServer.getId()).get());
    }

    @Test
    void find_unexisting_imageServer_return_empty() {
        assertThat(imageServerService.find(0L)).isEmpty();
    }


    @Test
    void list_light_imageServer() {
        ImageServer imageServer = builder.given_an_image_server();
        assertThat(imageServerService.list().stream().anyMatch(item -> item.getId().equals(imageServer.getId()))).isTrue();
    }

    @Test
    void retrieve_storage_spaces() throws IOException {
        ImageServer imageServer = builder.given_an_image_server();
        imageServer.setUrl("http://localhost:8888");
        imageServer = builder.persistAndReturn(imageServer);


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/storage/size.json"))
                .willReturn(
                        aResponse().withBody("" + "{\"used\":193396892,\"available\":445132860,\"usedP\":0.302878435,\"hostname\":\"b52416f53249\",\"mount\":\"/data/images\",\"ip\":null}")
                )
        );

        StorageStats response = imageServerService.storageSpace(imageServer);
        assertThat(response).isNotNull();
        assertThat(response.getUsed()).isEqualTo(193396892);
        assertThat(response.getHostname()).isEqualTo("b52416f53249");

    }

    //http://localhost-ims/image/download?fif=%2Fdata%2Fimages%2F58%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs

    @Test
    void retrieve_download_uri() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        assertThat(imageServerService.downloadUri(image))
                .isEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");

        assertThat(imageServerService.downloadUri(image.getUploadedFile()))
                .isEqualTo("http://localhost:8888/image/download?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs");

    }

    @Test
    void extract_properties_from_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");


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

        assertThat(imageServerService.properties(image).size()).isEqualTo(18);
        assertThat(imageServerService.properties(image).get("File.BitsPerSample")).isEqualTo("8");

    }

    @Test
    void get_associated_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/associated.json?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs"))
                .willReturn(
                        aResponse().withBody("[\"macro\",\"thumbnail\",\"label\"]")
                )
        );
        assertThat(imageServerService.associated(image).size()).isEqualTo(3);

    }

    @Test
    void get_label_abstract_image_macro() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");
        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo("/image/nested.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512&label=macro"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setMaxSize(512);
        labelParameter.setLabel("macro");
        labelParameter.setFormat("png");
        assertThat(imageServerService.label(image, labelParameter)).isEqualTo(mockResponse);
    }

    @Test
    void get_thumb_for_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=256"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        ImageParameter imageParameter = new ImageParameter();
        imageParameter.setMaxSize(256);
        imageParameter.setFormat("png");
        assertThat(imageServerService.thumb(slice, imageParameter)).isEqualTo(mockResponse);


        byte[] mockResponse2 = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo("/slice/thumb.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&maxSize=512"))
                .willReturn(
                        aResponse().withBody(mockResponse2)
                )
        );

        imageParameter.setMaxSize(512);
        imageParameter.setFormat("png");
        assertThat(imageServerService.thumb(slice, imageParameter)).isEqualTo(mockResponse2);
    }


    @Test
    void get_crop_for_abstract_image() throws IOException, ParseException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=1&topLeftY=50&width=49&height=49&location=POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        CropParameter cropParameter = new CropParameter();
        cropParameter.setLocation("POLYGON((1 1,50 10,50 50,10 50,1 1))");
        cropParameter.setFormat("png");
        byte[] crop = imageServerService.crop(slice, cropParameter);
        //List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(crop).isEqualTo(mockResponse);

    }


    @Test
    void get_window_for_abstract_image() throws UnsupportedEncodingException, ParseException {

        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("openslide/mrxs");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        configureFor("localhost", 8888);
        String url = "/slice/crop.png?fif=%2Fdata%2Fimages%2F" + builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop";
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(10);
        windowParameter.setY(20);
        windowParameter.setW(30);
        windowParameter.setH(40);
        windowParameter.setFormat("png");
        byte[] crop = imageServerService.window(slice, windowParameter);
        //List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        assertThat(crop).isEqualTo(mockResponse);

        windowParameter.setFormat("jpg");
        assertThat(imageServerService.windowUrl(slice, windowParameter))
                .isEqualTo("http://localhost:8888/slice/crop.jpg?fif=%2Fdata%2Fimages%2F"+ builder.given_superadmin().getId() + "%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs&topLeftX=10&topLeftY=220676&width=30&height=40&imageWidth=109240&imageHeight=220696&type=crop");
    }

}
