package hery.itu.erp.service.salary;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.login.LoginService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalaryStructAssService {
    private final RestTemplate restTemplate;
    private final LoginService loginService;
    private final String baseUrl = "http://erpnext.localhost:8000";

    public SalaryStructAssService(RestTemplateBuilder builder, LoginService loginService) {
        this.restTemplate = builder.build();
        this.loginService = loginService;
    }

    public Map createSalaryStructAss(byte[] file1Bytes, String companyName) throws Exception {
        String url = baseUrl + "/api/method/erpnext.api.importSeparer.import_salary_structures_and_assignments";
    
        // Encoder le fichier en base64
        String content1 = Base64.getEncoder().encodeToString(file1Bytes);
    
        // Préparer le body avec file1 ET company_name
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("file1", content1);
        body.add("company_name", companyName); // ✅ ajouté
    
        // Préparer les headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", loginService.getSessionCookie());
    
        // Construire la requête
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    
        // Appeler l'API
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    
        System.out.println("Réponse: " + response.getBody());
        return response.getBody();
    }
    

    /**
     * Génère plusieurs SalaryStructAss de month en month depuis fromDate jusqu'à toDate (inclus).
     */
    // public List<Boolean> generateSalaryStructAss(byte[] file1Bytes, LocalDate toDate) {
    //     List<Boolean> results = new ArrayList<>();
    //     LocalDate currentDate = salaryStructAss.getFrom_date();

    //     while (!currentDate.isAfter(toDate)) {
    //         boolean success = createSalaryStructAss(file1Bytes);
    //         results.add(success);

    //         // Avancer d'un mois
    //         currentDate = currentDate.plusMonths(1);
    //     }

    //     return results;
    // }
}

