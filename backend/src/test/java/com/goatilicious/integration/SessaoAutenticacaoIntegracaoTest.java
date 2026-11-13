package com.goatilicious.integration;

import com.goatilicious.services.UtilizadorService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoAutenticacaoIntegracaoTest {

    private static final String EMAIL = "sessao@goatilicious.test";
    private static final String SENHA = "senha-da-sessao";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilizadorService utilizadorService;

    @BeforeEach
    void criarConta() {
        utilizadorService.criarStaff("Staff Sessão", EMAIL, SENHA);
    }

    @Test
    void loginCriaSessaoHttpOnlyESemBasic() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();

        Cookie token = csrf.getResponse().getCookie("XSRF-TOKEN");
        String valor = token.getValue();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(token)
                        .header("X-XSRF-TOKEN", valor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("STAFF"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/me").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void logoutInvalidaSessao() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie token = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(post("/api/auth/logout")
                        .session(sessao)
                        .cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").session(sessao))
                .andExpect(status().isUnauthorized());
    }
}
