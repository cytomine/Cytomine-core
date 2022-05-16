package be.cytomine.service.middleware;

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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
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


    @Test
    void retrieve_formats() throws IOException {
        ImageServer imageServer = builder.given_an_image_server();
        imageServer.setUrl("http://localhost:8888");
        imageServer = builder.persistAndReturn(imageServer);


        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/formats"))
                .willReturn(
                        aResponse().withBody(
                                """
                                {"items":[
                                    {"id":"BMP","name":"BMP","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"DICOM","name":"Dicom","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PYROMETIFF","name":"Pyramidal OME-TIFF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PLANARTIFF","name":"Planar TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"IMAGEJTIFF","name":"ImageJ TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"JPEG","name":"JPEG","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"JPEG2000","name":"JPEG2000","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"OMETIFF","name":"OME-TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PNG","name":"PNG","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PPM","name":"PPM","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"SIS","name":"Olympus SIS TIFF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"PYRTIFF","name":"Pyramidal TIFF","remarks":"","convertible":false,"readable":true,"writable":true,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"VIRTUALSTACK","name":"Virtual Stack","remarks":"","convertible":true,"readable":true,"writable":false,"importable":false,"plugin":"pims.formats.common"},
                                    {"id":"WEBP","name":"Web P","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims.formats.common"},
                                    {"id":"CZI","name":"Zeiss CZI","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"LIF","name":"Leica LIF","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"ND2","name":"Nikon ND2","remarks":"","convertible":true,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_bioformats"},
                                    {"id":"WSIDICOM","name":"WSI Dicom","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_dicom"},
                                    {"id":"ISYNTAX","name":"Philips ISyntax","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_isyntax"},
                                    {"id":"BIF","name":"Ventana BIF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"MRXS","name":"3D Histech MIRAX","remarks":"One .mrxs file and one directory with same name with .dat and .ini files, packed in an archive. ","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"NDPI","name":"Hamamatsu NDPI","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"PHILIPS","name":"Philips TIFF","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"SCN","name":"Leica SCN","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"SVS","name":"Leica Aperio SVS","remarks":"","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"},
                                    {"id":"VMS","name":"Hamamatsu VMS","remarks":"One .vms file, one .opt optimization file and several .jpg with same name, packed in an archive. ","convertible":false,"readable":true,"writable":false,"importable":true,"plugin":"pims_plugin_format_openslide"}
                                    ],"size":26}                                        
                                        """
                        )
                )
        );

        List<Map<String, Object>> formats = imageServerService.formats(imageServer);
        printLastRequest();
        System.out.println(formats.stream().map(x -> x.get("id")).collect(Collectors.toList()));

        assertThat(formats).isNotNull();
        assertThat(formats.size()).isEqualTo(26);
        assertThat(formats.stream().map(x -> x.get("id"))).contains("PNG", "CZI", "SVS");
        assertThat(formats.stream().filter(x -> x.get("id").equals("PNG")).findFirst().get()).containsAllEntriesOf(Map.of("id", "PNG", "name", "PNG", "plugin", "pims.formats.common"));
    }


    //http://localhost-ims/image/download?fif=%2Fdata%2Fimages%2F58%2F1636379100999%2FCMU-2%2FCMU-2.mrxs&mimeType=openslide%2Fmrxs

    @Test
    void retrieve_download_uri() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        assertThat(imageServerService.downloadUri(image))
                .isEqualTo("http://localhost:8888/file/" + image.getPath() + "/export");

        assertThat(imageServerService.downloadUri(image.getUploadedFile()))
                .isEqualTo("http://localhost:8888/file/" + image.getPath() + "/export");
    }

    @Test
    void extract_properties_from_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");


        configureFor("localhost", 8888); //       /image/upload1644425985928451/LUNG1_pyr.tif/info
        stubFor(get(urlEqualTo("/image/" + image.getPath() + "/info"))
                .willReturn(
                        aResponse().withBody(
                                """
                                  {"image":
                                      {
                                          "original_format":"PYRTIFF",
                                          "width":30720,
                                          "height":25600,
                                          "depth":1,
                                          "duration":1,
                                          "physical_size_x":100000.00617,
                                          "physical_size_y":100000.00617,
                                          "physical_size_z":null,
                                          "frame_rate":null,
                                          "n_channels":3,
                                          "n_concrete_channels":1,
                                          "n_samples":3,
                                          "n_planes":1,
                                          "are_rgb_planes":true,
                                          "n_distinct_channels":1,
                                          "acquired_at":null,
                                          "description":"",
                                          "pixel_type":"uint8",
                                          "significant_bits":8,"bits":8},
                                          "instrument":
                                              {"microscope":{"model":null},
                                               "objective":{"nominal_magnification":null,"calibrated_magnification":null}},
                                               "associated":[],
                                               "channels":[
                                                  {"index":0,"suggested_name":"R","emission_wavelength":null,"excitation_wavelength":null,"color":"#f00"},
                                                  {"index":1,"suggested_name":"G","emission_wavelength":null,"excitation_wavelength":null,"color":"#0f0"},
                                                  {"index":2,"suggested_name":"B","emission_wavelength":null,"excitation_wavelength":null,"color":"#00f"}
                                               ],
                                               "representations":[
                                                  {"role":"UPLOAD",
                                                   "file":
                                                      {"file_type":"SINGLE",
                                                      "filepath":"/data/images/upload1644425985928451/LUNG1_pyr.tif",
                                                      "stem":"LUNG1_pyr",
                                                      "extension":".tif",
                                                      "created_at":"2022-05-05T22:16:23.318839",
                                                      "size":126616954,"is_symbolic":false,"role":"UPLOAD"}
                                                      },
                                                      {"role":"ORIGINAL",
                                                      "file":
                                                          {"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/original.PYRTIFF","stem":"original","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"ORIGINAL"}
                                                          },
                                                          {"role":"SPATIAL","file":{"file_type":"SINGLE","filepath":"/data/images/upload1644425985928451/processed/visualisation.PYRTIFF","stem":"visualisation","extension":".PYRTIFF","created_at":"2022-05-05T22:16:23.318839","size":126616954,"is_symbolic":true,"role":"SPATIAL"},"pyramid":{"n_tiers":8,"tiers":[{"zoom":7,"level":0,"width":30720,"height":25600,"tile_width":256,"tile_height":256,"downsampling_factor":1.0,"n_tiles":12000,"n_tx":120,"n_ty":100},{"zoom":6,"level":1,"width":15360,"height":12800,"tile_width":256,"tile_height":256,"downsampling_factor":2.0,"n_tiles":3000,"n_tx":60,"n_ty":50},{"zoom":5,"level":2,"width":7680,"height":6400,"tile_width":256,"tile_height":256,"downsampling_factor":4.0,"n_tiles":750,"n_tx":30,"n_ty":25},{"zoom":4,"level":3,"width":3840,"height":3200,"tile_width":256,"tile_height":256,"downsampling_factor":8.0,"n_tiles":195,"n_tx":15,"n_ty":13},{"zoom":3,"level":4,"width":1920,"height":1600,"tile_width":256,"tile_height":256,"downsampling_factor":16.0,"n_tiles":56,"n_tx":8,"n_ty":7},{"zoom":2,"level":5,"width":960,"height":800,"tile_width":256,"tile_height":256,"downsampling_factor":32.0,"n_tiles":16,"n_tx":4,"n_ty":4},{"zoom":1,"level":6,"width":480,"height":400,"tile_width":256,"tile_height":256,"downsampling_factor":64.0,"n_tiles":4,"n_tx":2,"n_ty":2},{"zoom":0,"level":7,"width":240,"height":200,"tile_width":256,"tile_height":256,"downsampling_factor":128.0,"n_tiles":1,"n_tx":1,"n_ty":1}]}}]}
      
                                  """
                        )
                )
        );
        assertThat(((Map<String, Object>)imageServerService.properties(image).get("image")).size()).isEqualTo(20);
        printLastRequest();
        assertThat(((Map<String, Object>)imageServerService.properties(image).get("image")).get("width")).isEqualTo(30720);

    }

    @Test
    void get_associated_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/image/" + image.getPath() + "/info/associated"))
                .willReturn(
                        aResponse().withBody("{\"items\": [{\"name\":\"macro\"},{\"name\":\"thumbnail\"},{\"name\":\"label\"}], \"size\": 0}")
                )
        );
        int size = imageServerService.associated(image).size();
        printLastRequest();
        assertThat(size).isEqualTo(3);

    }

    @Test
    void get_label_abstract_image_macro() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");
        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        String url = "/image/" + image.getPath() + "/associated/macro?length=512";

        System.out.println(url);
        stubFor(get(urlEqualTo(url))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setMaxSize(512);
        labelParameter.setLabel("macro");
        labelParameter.setFormat("png");
        byte[] data = imageServerService.label(image, labelParameter, null).getContent();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);
    }

    @Test
    void get_thumb_for_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);

        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content


        stubFor(get(urlEqualTo("/image/" + image.getPath() + "/thumb?z_slices=0&timepoints=0&length=256"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );
        ImageParameter imageParameter = new ImageParameter();
        imageParameter.setMaxSize(256);
        imageParameter.setFormat("png");

        byte[] data = imageServerService.thumb(slice, imageParameter, null).getContent();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse);


        byte[] mockResponse2 = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        stubFor(get(urlEqualTo("/image/" + image.getPath() + "/thumb?z_slices=0&timepoints=0&length=512"))
                .willReturn(
                        aResponse().withBody(mockResponse2)
                )
        );

        imageParameter.setMaxSize(512);
        imageParameter.setFormat("png");
        data = imageServerService.thumb(slice, imageParameter, null).getContent();
        printLastRequest();
        assertThat(data).isEqualTo(mockResponse2);
    }


    @Test
    void get_crop_for_abstract_image() throws IOException, ParseException {
        AbstractImage image = builder.given_an_abstract_image();
        image.setWidth(109240);
        image.setHeight(220696);
        image.getUploadedFile().getImageServer().setBasePath("/data/images");
        image.getUploadedFile().getImageServer().setUrl("http://localhost:8888");
        image.getUploadedFile().setFilename("1636379100999/CMU-2/CMU-2.mrxs");
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());

        configureFor("localhost", 8888);
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        //String url = "/image/" + image.getPath() + "/annotation/drawing?context_factor=1.25&annotations=%7B%22geometry%22%3A%22POLYGON+%28%281+1%2C+50+10%2C+50+50%2C+10+50%2C+1+1%29%29%22%2C%22stroke_color%22%3Anull%2C%22stroke_width%22%3Anull%7D&level=0&z_slices=0&timepoints=0";
        String url = "/image/" + image.getPath() + "/annotation/drawing";
        String body = "{\"context_factor\":1.25,\"annotations\":{\"geometry\":\"POLYGON ((1 1, 50 10, 50 50, 10 50, 1 1))\",\"stroke_color\":null,\"stroke_width\":null},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(url)).withRequestBody(equalTo(
                body
                        ))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        CropParameter cropParameter = new CropParameter();
        cropParameter.setLocation("POLYGON((1 1,50 10,50 50,10 50,1 1))");
        cropParameter.setFormat("png");
        cropParameter.setDraw(true);
        cropParameter.setIncreaseArea(1.25);
        cropParameter.setComplete(true);
        //draw=true&complete=true&increaseArea=1.25

        byte[] crop = null;
        try {
            crop = imageServerService.crop(slice, cropParameter, null).getContent();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        printLastRequest();
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
        image.getUploadedFile().setContentType("MRXS");

        AbstractSlice slice = builder.given_an_abstract_slice(image, 0, 0, 0);
        slice.setUploadedFile(image.getUploadedFile());
        byte[] mockResponse = UUID.randomUUID().toString().getBytes(); // we don't care about the response content, we just check that core build a valid ims url and return the content

        //http://localhost-ims/image/1650442012355/2021-12-17-114138.jpg/window?region=[left:1, top:2, width:3, height:4]&level=0

        configureFor("localhost", 8888);
        String url = "/image/" + image.getPath() + "/window";
        String body = "{\"region\":{\"left\":10,\"top\":20,\"width\":30,\"height\":40},\"level\":0,\"z_slices\":0,\"timepoints\":0}";
        System.out.println(url);
        System.out.println(body);
        stubFor(post(urlEqualTo(url)).withRequestBody(equalTo(body))
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
        byte[] crop = imageServerService.window(slice, windowParameter, null).getContent();
        printLastRequest();
        assertThat(crop).isEqualTo(mockResponse);
    }

    private void printLastRequest() {
        List<LoggedRequest> all = wireMockServer.findAll(RequestPatternBuilder.allRequests());
        all.subList(Math.max(all.size() - 3, 0), all.size()).forEach(x -> System.out.println(x.getMethod() + " " + x.getAbsoluteUrl() + " " + x.getBodyAsString()));
    }
}
