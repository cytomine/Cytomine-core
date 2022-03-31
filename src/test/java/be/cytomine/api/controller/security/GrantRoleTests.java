package be.cytomine.api.controller.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin")
public class GrantRoleTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restGrandRoleControllerMockMvc;

    protected MockHttpSession session;
//
//    protected MockHttpServletRequest request;

    @Test
    @WithMockUser(username = "admin")
    public void open_close_admin_session_as_admin() throws Exception {
        startSession();

        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                .session(session))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(false))
                .andExpect(jsonPath("$.userByNow").value(true));


        restGrandRoleControllerMockMvc.perform(get("/session/admin/open.json")
                        .session(session))
                .andDo(print())
                .andExpect(status().isOk());


        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                        .session(session))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(true))
                .andExpect(jsonPath("$.userByNow").value(true));


        restGrandRoleControllerMockMvc.perform(get("/session/admin/close.json")
                        .session(session))
                .andDo(print())
                .andExpect(status().isOk());

        restGrandRoleControllerMockMvc.perform(get("/session/admin/info.json")
                        .session(session))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminByNow").value(false))
                .andExpect(jsonPath("$.userByNow").value(true));


        endSession();
    }

    protected void startSession() {
        session = new MockHttpSession();
    }

    protected void endSession() {
        session.clearAttributes();
        session = null;
    }


}
