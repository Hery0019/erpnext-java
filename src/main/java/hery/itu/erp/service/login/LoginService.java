package hery.itu.erp.service.login;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.security.ErpNextUser;

/**
 * Authentification auprès d'ERPNext. Ce service est sans état : le cookie de session
 * appartient à l'utilisateur courant (voir {@link ErpNextUser}) et non au serveur.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final RestTemplate restTemplate;
    private final ErpNextProperties properties;

    public LoginService(RestTemplateBuilder builder, ErpNextProperties properties) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(15))
                .build();
        this.properties = properties;
    }

    /**
     * Tente un login ERPNext.
     *
     * @return le cookie {@code sid=...} si les identifiants sont acceptés, vide sinon
     * @throws AuthenticationServiceException si ERPNext est injoignable
     */
    public Optional<String> loginToErpNext(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usr", username);
        form.add("pwd", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.baseUrl() + "/api/method/login", new HttpEntity<>(form, headers), String.class);

            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.startsWith("sid=") && !cookie.startsWith("sid=Guest")) {
                        return Optional.of(cookie.split(";")[0]);
                    }
                }
            }
            log.warn("Login ERPNext de '{}' : réponse {} sans cookie sid", username, response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            // 401 / 403 : identifiants refusés par ERPNext
            log.info("Login ERPNext refusé pour '{}' ({})", username, e.getStatusCode());
            return Optional.empty();
        } catch (RestClientException e) {
            throw new AuthenticationServiceException("ERPNext est injoignable : " + e.getMessage(), e);
        }
    }

    /**
     * Cookie de session ERPNext de l'utilisateur courant.
     *
     * @throws AuthenticationCredentialsNotFoundException si aucun utilisateur n'est authentifié
     */
    public String getSessionCookie() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ErpNextUser user) {
            return user.sidCookie();
        }
        throw new AuthenticationCredentialsNotFoundException("Aucune session ERPNext pour l'utilisateur courant");
    }
}
