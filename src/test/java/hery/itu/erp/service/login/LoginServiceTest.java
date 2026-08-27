package hery.itu.erp.service.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.security.ErpNextUser;

class LoginServiceTest {

    private MockRestServiceServer server;
    private LoginService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        service = new LoginService(new RestTemplateBuilder(customizer), new ErpNextProperties("http://erp.test/"));
        server = customizer.getServer();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginAccepteRetourneLeCookieSid() {
        server.expect(requestTo("http://erp.test/api/method/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("usr=hery&pwd=secret"))
                .andRespond(withSuccess("{\"message\":\"Logged In\"}", MediaType.APPLICATION_JSON)
                        .header("Set-Cookie", "sid=abc123; Path=/; HttpOnly"));

        assertThat(service.loginToErpNext("hery", "secret")).contains("sid=abc123");
        server.verify();
    }

    @Test
    void loginRefuseRetourneVide() {
        server.expect(requestTo("http://erp.test/api/method/login")).andRespond(withUnauthorizedRequest());

        assertThat(service.loginToErpNext("hery", "wrong")).isEmpty();
    }

    @Test
    void reponseSansCookieRetourneVide() {
        server.expect(requestTo("http://erp.test/api/method/login"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON).header("Set-Cookie", "sid=Guest; Path=/"));

        assertThat(service.loginToErpNext("hery", "secret")).isEmpty();
    }

    @Test
    void erpNextInjoignableLeveUneExceptionDistincte() {
        server.expect(requestTo("http://erp.test/api/method/login"))
                .andRespond(request -> { throw new IOException("Connection refused"); });

        assertThatThrownBy(() -> service.loginToErpNext("hery", "secret"))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("injoignable");
    }

    @Test
    void getSessionCookieLitLePrincipalDeLUtilisateurCourant() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(new ErpNextUser("hery", "sid=xyz"), null, List.of()));

        assertThat(service.getSessionCookie()).isEqualTo("sid=xyz");
    }

    @Test
    void getSessionCookieSansUtilisateurEchoue() {
        assertThatThrownBy(service::getSessionCookie)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
