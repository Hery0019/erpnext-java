package hery.itu.erp.service.salary;

import com.fasterxml.jackson.databind.ObjectMapper;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.login.LoginService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SalaryStructAssService {

    private final RestTemplate restTemplate;
    private final LoginService loginService;
    private final String baseUrl = "http://erpnext.localhost:8000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SalaryStructAssService(RestTemplateBuilder builder, LoginService loginService) {
        this.restTemplate = builder.build();
        this.loginService = loginService;
    }

    public String createSalaryStructureAssignmentAndSubmit(SalaryStructAss salaryStructAss) throws Exception {
        String cookie = loginService.getSessionCookie();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        // ✅ Créer l'Assignment
        Map<String, Object> data = new HashMap<>();
        data.put("employee", salaryStructAss.getEmployee());
        data.put("salary_structure", salaryStructAss.getSalary_structure());
        data.put("company", salaryStructAss.getCompany());
        data.put("currency", salaryStructAss.getCurrency());

        if (salaryStructAss.getBase() != null) {
            data.put("base", salaryStructAss.getBase());
        }
        data.put("from_date", salaryStructAss.getFrom_date());
        if (salaryStructAss.getTo_date() != null) {
            data.put("to_date", salaryStructAss.getTo_date());
        }

        String payload = objectMapper.writeValueAsString(data);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Structure Assignment",
                HttpMethod.POST,
                request,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la création : " + response.getBody());
        }

        String name = objectMapper.readTree(response.getBody()).path("data").path("name").asText();
        if (name == null || name.isEmpty()) {
            throw new Exception("Impossible de récupérer le name de l'Assignment");
        }

        // ✅ Vérifier qu'il est créé
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> check = restTemplate.exchange(
                    baseUrl + "/api/resource/Salary Structure Assignment/" + name,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (check.getStatusCode().is2xxSuccessful()) break;
            Thread.sleep(300);
        }

        // ✅ Soumettre via run_method=submit
        ResponseEntity<String> submitResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Structure Assignment/" + name + "?run_method=submit",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!submitResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission : " + submitResponse.getBody());
        }

        return name;
    }

    public String createSalarySlipAndSubmit(SalaryStructAss salaryStructAss) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        // Vérifier que l'Assignment existe
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> check = restTemplate.exchange(
                    baseUrl + "/api/resource/Salary Structure Assignment?filters=" +
                            "[[\"Salary Structure Assignment\",\"employee\",\"=\",\"" + salaryStructAss.getEmployee() + "\"]]",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (check.getBody().contains("data")) break;
            Thread.sleep(300);
        }

        // ✅ Créer le Salary Slip
        Map<String, Object> slipData = new HashMap<>();
        slipData.put("employee", salaryStructAss.getEmployee());
        slipData.put("salary_structure", salaryStructAss.getSalary_structure());
        slipData.put("company", salaryStructAss.getCompany());
        slipData.put("start_date", salaryStructAss.getFrom_date());
        slipData.put("end_date", salaryStructAss.getTo_date() != null ? salaryStructAss.getTo_date() : salaryStructAss.getFrom_date());
        slipData.put("posting_date", salaryStructAss.getPosting_date());
        slipData.put("payroll_frequency", "Monthly");

        String slipJson = objectMapper.writeValueAsString(slipData);
        HttpEntity<String> slipRequest = new HttpEntity<>(slipJson, headers);

        ResponseEntity<String> slipResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip",
                HttpMethod.POST,
                slipRequest,
                String.class);

        if (!slipResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la création du Salary Slip : " + slipResponse.getBody());
        }

        // ✅ Soumettre le Salary Slip créé
        String slipName = objectMapper.readTree(slipResponse.getBody()).path("data").path("name").asText();
        if (slipName == null || slipName.isEmpty()) {
            throw new Exception("Impossible de récupérer le name du Salary Slip");
        }

        ResponseEntity<String> slipSubmitResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip/" + slipName + "?run_method=submit",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!slipSubmitResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission du Salary Slip : " + slipSubmitResponse.getBody());
        }

        return slipName;
    }

    public Map<String, Object> createAssignmentAndSlip(SalaryStructAss salaryStructAss) throws Exception {
        String assignmentName = createSalaryStructureAssignmentAndSubmit(salaryStructAss);
        String slipName = createSalarySlipAndSubmit(salaryStructAss);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", assignmentName);
        result.put("slip", slipName);
        return result;
    }
}
