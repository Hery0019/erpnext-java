package hery.itu.erp.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hery.itu.erp.service.login.LoginService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoginService loginService;

    @Test
    void pageDeLoginPublique() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void anonymeRedirigeVersLogin() throws Exception {
        mvc.perform(get("/employes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/"));
    }

    @Test
    void suppressionAnonymeRefusee() throws Exception {
        mvc.perform(get("/employes/delete/HR-EMP-00001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/"));
    }

    @Test
    void mauvaisIdentifiantsRedirigeVersErreur() throws Exception {
        when(loginService.loginToErpNext(anyString(), anyString())).thenReturn(Optional.empty());

        mvc.perform(post("/login").param("username", "hery").param("password", "x").with(csrf()))
                .andExpect(redirectedUrl("/?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void bonsIdentifiantsOuvrentUneSessionUtilisateur() throws Exception {
        when(loginService.loginToErpNext("hery", "ok")).thenReturn(Optional.of("sid=abc"));

        mvc.perform(post("/login").param("username", "hery").param("password", "ok").with(csrf()))
                .andExpect(redirectedUrl("/employes"))
                .andExpect(authenticated().withUsername("hery"));
    }

    @Test
    void postSansJetonCsrfRefuse() throws Exception {
        mvc.perform(post("/login").param("username", "hery").param("password", "ok"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAuthentifieSansCsrfRefuse() throws Exception {
        mvc.perform(post("/modification-groupe/execute").with(user("hery")))
                .andExpect(status().isForbidden());
    }
}
