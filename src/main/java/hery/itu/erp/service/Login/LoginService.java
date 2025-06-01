package hery.itu.erp.service.login;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class LoginService {

    private final RestTemplate restTemplate = new RestTemplate();
    private String sessionCookie; // Stocke le cookie sid

    public boolean loginToErpNext(String username, String password) {
        String url = "http://erpnext.localhost:8000/api/method/login";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usr", username);
        form.add("pwd", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (cookies != null) {
                    for (String cookie : cookies) {
                        if (cookie.startsWith("sid=")) {
                            sessionCookie = cookie.split(";")[0]; // Ex: sid=abc123
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("Erreur lors de la connexion : " + e.getMessage());
            return false;
        }
    }

    public String getSessionCookie() {
        return sessionCookie;
    }
}
