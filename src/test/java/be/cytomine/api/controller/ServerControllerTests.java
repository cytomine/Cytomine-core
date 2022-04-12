package be.cytomine.api.controller;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.dto.DatedCytomineDomain;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.utils.JsonObject;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static be.cytomine.security.jwt.TokenType.SHORT_TERM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.jmx.export.naming.IdentityNamingStrategy.TYPE_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class ServerControllerTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restConfigurationControllerMockMvc;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @Autowired
    private TokenProvider tokenProvider;

    private JwtParser jwtParser;

    @Autowired
    private ApplicationConfiguration applicationConfiguration;

    @BeforeEach
    public void before() {
        byte[] keyBytes;
        String secret = applicationConfiguration.getAuthentication().getJwt().getSecret();
        keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        lastConnectionRepository.deleteAll();
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth() throws Exception {
        ResultActions resultActions = restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.shortTermToken").exists())
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));

        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
        assertThat(lastConnection).hasSize(1);
        assertThat(lastConnection.get(0).getProject()).isNull();
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth_with_get() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/server/ping.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.shortTermToken").exists())
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));

        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
        assertThat(lastConnection).hasSize(1);
        assertThat(lastConnection.get(0).getProject()).isNull();
    }


    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth_with_short_term_token() throws Exception {
        ResultActions resultActions = restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.shortTermToken").exists())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));
        JsonObject response = JsonObject.toJsonObject(resultActions.andReturn().getResponse().getContentAsString());
        String jwt = response.getJSONAttrStr("shortTermToken");
        assertThat(jwt).isNotNull();
        assertThat(tokenProvider.validateToken(jwt)).isTrue();

        for (Map.Entry<String, Object> stringObjectEntry : jwtParser.parseClaimsJws(jwt).getBody().entrySet()) {
            System.out.println(stringObjectEntry.getKey() + "=" + stringObjectEntry.getValue());
        }
        assertThat(jwtParser.parseClaimsJws(jwt).getBody().get(TYPE_KEY)).isEqualTo(SHORT_TERM.toString());
        Date date = new Date(Long.parseLong(jwtParser.parseClaimsJws(jwt).getBody().get("exp").toString())*1000);
        assertThat(date).isBetween(DateUtils.addSeconds(new Date(), 250), DateUtils.addSeconds(new Date(), 350));
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_with_project() throws Exception {
        Project project = builder.given_a_project();
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": "+project.getId()+"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));

        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
        assertThat(lastConnection).hasSize(1);
        assertThat(lastConnection.get(0).getProject()).isEqualTo(project.getId());

    }

    @Test
    @Transactional
    public void ping_as_unauth() throws Exception {
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").doesNotExist());

        assertThat(lastConnectionRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void check_status() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/status.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath());
    }

}
