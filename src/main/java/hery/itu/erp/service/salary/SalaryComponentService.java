package hery.itu.erp.service.salary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hery.itu.erp.service.login.LoginService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SalaryComponentService {

    private final RestTemplate restTemplate;
    private final LoginService loginService;
    private final String baseUrl = "http://erpnext.localhost:8000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SalaryComponentService(RestTemplateBuilder builder, LoginService loginService) {
        this.restTemplate = builder.build();
        this.loginService = loginService;
    }

    /**
     * Récupère la liste des noms des Salary Component.
     *
     * @return liste de noms (String)
     * @throws Exception en cas d'erreur HTTP ou JSON
     */
    public List<String> getAllSalaryComponentNames() throws Exception {
        String cookie = loginService.getSessionCookie();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Component?fields=[\"name\"]";

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération des Salary Components : " + response.getBody());
        }

        List<String> componentNames = new ArrayList<>();
        JsonNode arr = objectMapper.readTree(response.getBody()).path("data");
        if (arr.isArray()) {
            for (JsonNode node : arr) {
                componentNames.add(node.path("name").asText());
            }
        }

        return componentNames;
    }
}
