package hery.itu.erp.service.salary;

import com.fasterxml.jackson.databind.ObjectMapper;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.login.LoginService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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

        Map<String, Object> data = new HashMap<>();
        data.put("employee", salaryStructAss.getEmployee());
        data.put("salary_structure", salaryStructAss.getSalary_structure());
        data.put("company", salaryStructAss.getCompany());
        data.put("currency", salaryStructAss.getCurrency());
        data.put("base", salaryStructAss.getBase().toString());
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

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> check = restTemplate.exchange(
                    baseUrl + "/api/resource/Salary Structure Assignment/" + name,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (check.getStatusCode().is2xxSuccessful()) break;
            Thread.sleep(300);
        }

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

        // ✅ Générer suffix incrémental
        String prefix = "Sal Slip/" + salaryStructAss.getEmployee() + "/";
        int suffix = getNextSlipSuffix(salaryStructAss.getEmployee());
        String slipName = String.format("%s%05d", prefix, suffix);

        Map<String, Object> slipData = new HashMap<>();
        slipData.put("name", slipName);
        slipData.put("employee", salaryStructAss.getEmployee());
        slipData.put("salary_structure", salaryStructAss.getSalary_structure());
        slipData.put("company", salaryStructAss.getCompany());
        slipData.put("start_date", salaryStructAss.getFrom_date());
        slipData.put("end_date", salaryStructAss.getTo_date() != null ? salaryStructAss.getTo_date() : salaryStructAss.getFrom_date());
        slipData.put("posting_date", salaryStructAss.getPosting_date());
        slipData.put("payroll_frequency", "Monthly");
        slipData.put("base", salaryStructAss.getBase());

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

    private int getNextSlipSuffix(String employee) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Slip?fields=[\"name\"]"
                + "&filters=[[\"Salary Slip\",\"employee\",\"=\",\"" + employee + "\"]]"
                + "&order_by=creation desc&limit=1";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String lastName = null;
        if (response.getStatusCode().is2xxSuccessful()) {
            var arr = objectMapper.readTree(response.getBody()).path("data");
            if (arr.isArray() && arr.size() > 0) {
                lastName = arr.get(0).path("name").asText();
            }
        }

        if (lastName != null && lastName.contains("/")) {
            String[] parts = lastName.split("/");
            try {
                return Integer.parseInt(parts[2]) + 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public Map<String, Object> createAssignmentAndSlip(SalaryStructAss salaryStructAss) throws Exception {
        if (salaryStructAss.getBase() == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase != null) {
                salaryStructAss.setBase(BigDecimal.valueOf(lastBase));
            } else {
                throw new Exception("Base introuvable et non fournie !");
            }
        }

        String assignmentName = createSalaryStructureAssignmentAndSubmit(salaryStructAss);
        String slipName = createSalarySlipAndSubmit(salaryStructAss);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", assignmentName);
        result.put("slip", slipName);
        return result;
    }

    public List<String> generateSalary(SalaryStructAss salaryStructAss, LocalDate startDate, LocalDate endDate) throws Exception {
        List<String> generatedSlips = new ArrayList<>();

        BigDecimal base = salaryStructAss.getBase();
        System.out.println("Base fournie par l'employee " + salaryStructAss.getEmployee() + " : " + base);
        if (base == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase != null) {
                base = BigDecimal.valueOf(lastBase);
                System.out.println("Base null donc recherche du dernier base");
                System.out.println("Dernier base trouvée pour l'employé " + salaryStructAss.getEmployee() + " : " + base);
            } else {
                throw new Exception("Base introuvable pour l'employé : " + salaryStructAss.getEmployee());
            }
        }


        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate limit = endDate.withDayOfMonth(1);

        while (!current.isAfter(limit)) {
            LocalDate slipStart = current;
            LocalDate slipEnd = current.with(TemporalAdjusters.lastDayOfMonth());

            SalaryStructAss slipAss = new SalaryStructAss();
            slipAss.setEmployee(salaryStructAss.getEmployee());
            slipAss.setSalary_structure(salaryStructAss.getSalary_structure());
            slipAss.setCompany(salaryStructAss.getCompany());
            slipAss.setCurrency(salaryStructAss.getCurrency());
            slipAss.setBase(base);
            System.out.println("Base dans l'objet slipAss : " + slipAss.getBase());
            slipAss.setFrom_date(slipStart.toString());
            slipAss.setTo_date(slipEnd.toString());
            slipAss.setPosting_date(slipEnd.toString());

            Map<String, Object> result = createAssignmentAndSlip(slipAss);
            generatedSlips.add(result.get("slip").toString());

            current = current.plusMonths(1);
        }

        return generatedSlips;
    }

    private Double getLastSalaryBase(String employee) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);
    
        // ✅ Lire le dernier Salary Structure Assignment soumis
        String url = baseUrl + "/api/resource/Salary Structure Assignment?filters="
                + "[[\"Salary Structure Assignment\",\"employee\",\"=\",\"" + employee + "\"],"
                + "[\"Salary Structure Assignment\",\"docstatus\",\"=\",1]]"
                + "&fields=[\"name\",\"base\",\"from_date\"]"
                + "&order_by=from_date desc&limit=1";
    
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    
        var arr = objectMapper.readTree(response.getBody()).path("data");
        if (arr.isArray() && arr.size() > 0) {
            return arr.get(0).path("base").asDouble();
        }
    
        return null; // pas trouvé
    }
    
}
