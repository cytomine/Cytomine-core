package be.cytomine.service.image;

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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.dto.*;
import be.cytomine.service.middleware.ImageServerService;
import com.github.tomakehurst.wiremock.WireMockServer;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImagePropertiesServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    ImagePropertiesService imagePropertiesService;

    @Autowired
    PropertyRepository propertyRepository;

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
    void clear_properties_from_abstract_image() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        Property property = builder.given_a_property(image, "cytomine.width", "value1");
        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isPresent();
        imagePropertiesService.clear(image);
        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isEmpty();

    }

    @Test
    void clear_properties_from_abstract_image_does_not_affect_other_property() throws IOException {
        AbstractImage image = builder.given_an_abstract_image();
        Property property = builder.given_a_property(image, "special", "value1");
        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "special")).isPresent();
        imagePropertiesService.clear(image);
        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "special")).isPresent();

    }

    @Test
    void parser_for_property() {
        ImagePropertiesValue width = ImagePropertiesService.keys().get(0);
        assertThat(width.parser.apply(123)).isEqualTo(123);
    }


    @Test
    void extract_populated_properties_to_abstract_image() throws IOException, IllegalAccessException {
        AbstractImage image = builder.given_an_abstract_image();
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

        imagePropertiesService.populate(image);

        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width")).isPresent();
        assertThat(propertyRepository.findByDomainIdentAndKey(image.getId(), "cytomine.width").get().getValue()).isEqualTo("854");


        assertThat(imageServerService.properties(image).size()).isEqualTo(18);
        imagePropertiesService.extractUseful(image);

        assertThat(image.getWidth()).isEqualTo(854);
        assertThat(image.getHeight()).isEqualTo(1724);
        assertThat(image.getColorspace()).isEqualTo("empty");
    }

}
