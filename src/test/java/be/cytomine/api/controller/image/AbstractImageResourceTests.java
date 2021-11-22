package be.cytomine.api.controller.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class AbstractImageResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAbstractImageControllerMockMvc;

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
        AbstractImage image1 = builder.given_an_abstract_image();
        AbstractImage image2 = builder.given_an_abstract_image();
        AbstractImage image3 = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json")) // default sorting must be created desc
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("sort", "id").param("order", "asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(image1.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image3.getId()));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("sort", "id").param("order", "desc"))
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

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("sort", "width").param("order", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image3.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(image1.getId()));
    }



    @Test
    @Transactional
    public void list_abstract_image_with_pagination() throws Exception {
        AbstractImage image1 = builder.given_an_abstract_image();
        AbstractImage image2 = builder.given_an_abstract_image();
        AbstractImage image3 = builder.given_an_abstract_image();

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "0").param("max", "0"))
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


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "0").param("max", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image3.getId()))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "1").param("max", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "1").param("max", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2)))) // default sorting must be created desc
                .andExpect(jsonPath("$.collection[0].id").value(image2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(image1.getId()))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "0").param("max", "500"))
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


        restAbstractImageControllerMockMvc.perform(get("/api/abstractimage.json").param("offset", "500").param("max", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(0)))) // default sorting must be created desc
                .andExpect(jsonPath("$.offset").value(500))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
    
}
