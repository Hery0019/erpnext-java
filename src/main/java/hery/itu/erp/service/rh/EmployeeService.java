package hery.itu.erp.service.rh;

import java.util.*;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;

import hery.itu.erp.model.rh.EmployeeResponse;
import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.service.login.LoginService;

@Service
public class EmployeeService {
    private final RestTemplate restTemplate;
    private final LoginService loginService;
    private final String baseUrl = "http://erpnext.localhost:8000";

    public EmployeeService(RestTemplateBuilder builder, LoginService loginService) {
        this.restTemplate = builder.build();
        this.loginService = loginService;
    }

    public List<Employee> getImportantEmployees() {
        String url = baseUrl + "/api/resource/Employee?fields=[\"first_name\",\"middle_name\",\"date_of_birth\",\"date_of_joining\",\"status\",\"name\",\"gender\",\"company\",\"employee_name\"]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie()); 
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<EmployeeResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                EmployeeResponse.class
        );

        return response.getBody().getData();
    }

    public List<Employee> filterEmployees(String firstName, String lastName, String gender, String status,
                                      String dateStart, String dateEnd, String company) {

        // URL avec les champs spécifiques en query string
        String url = "http://erpnext.localhost:8000/api/resource/Employee" +
                "?fields=[\"first_name\",\"last_name\",\"date_of_birth\",\"date_of_joining\"," +
                "\"status\",\"name\",\"gender\",\"company\"]" +
                "&limit_page_length=1000";

        List<List<String>> filters = new ArrayList<>();

        if (firstName != null && !firstName.isEmpty()) {
            filters.add(Arrays.asList("first_name", "like", "%" + firstName + "%"));
        }
        if (lastName != null && !lastName.isEmpty()) {
            filters.add(Arrays.asList("last_name", "like", "%" + lastName + "%"));
        }
        if (gender != null && !gender.isEmpty()) {
            filters.add(Arrays.asList("gender", "=", gender));
        }
        if (status != null && !status.isEmpty()) {
            filters.add(Arrays.asList("status", "=", status));
        }
        if (company != null && !company.isEmpty()) {
            filters.add(Arrays.asList("company", "like", "%" + company + "%"));
        }

        if (dateStart != null && !dateStart.isEmpty()) {
            filters.add(Arrays.asList("date_of_joining", ">=", dateStart));
        }
        if (dateEnd != null && !dateEnd.isEmpty()) {
            filters.add(Arrays.asList("date_of_joining", "<=", dateEnd));
        }

        // Mettre les filtres dans le corps de la requête
        Map<String, Object> params = new HashMap<>();
        params.put("filters", filters);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        // Requête HTTP GET avec champs dans l'URL, filtres dans le corps
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        List<Employee> employees = new ArrayList<>();

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode dataArray = response.getBody().get("data");
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode emp : dataArray) {
                    Employee e = getEmployeeByName(emp.path("name").asText());
                    employees.add(e);
                }
            }
        }
        return employees;
    }

    public Employee getEmployeeByName(String employeeName) {
        String url = "http://erpnext.localhost:8000/api/resource/Employee/" + employeeName;
    
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());  // Authentification ERPNext
        HttpEntity<Void> entity = new HttpEntity<>(headers);
    
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );
    
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode data = response.getBody().get("data");
            if (data != null) {
                Employee emp = new Employee();
                emp.setName(data.path("name").asText());
                emp.setFirst_name(data.path("first_name").asText());
                emp.setLast_name(data.path("last_name").asText());
                emp.setDate_of_birth(data.path("date_of_birth").asText());
                emp.setDate_of_joining(data.path("date_of_joining").asText());
                emp.setGender(data.path("gender").asText());
                emp.setStatus(data.path("status").asText());
                emp.setCompany(data.path("company").asText());
                // Ajoutez ici d'autres champs si votre classe Employee en a
    
                return emp;
            }
        }
    
        return null; // En cas d'erreur ou de non-trouvé
    }
    

    public boolean createEmployee(Employee employee) {
        RestTemplate restTemplate = new RestTemplate();

        String EMPLOYEE_API_URL = "http://erpnext.localhost:8000/api/resource/Employee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", loginService.getSessionCookie());  // session auth

        HttpEntity<Employee> request = new HttpEntity<>(employee, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(EMPLOYEE_API_URL, request, String.class);

        return response.getStatusCode() == HttpStatus.OK;
    }

    public void deleteEmploye(String employeeId) {
        String sessionCookie = loginService.getSessionCookie();
        String url = "http://erpnext.localhost:8000/api/resource/Employee/" + employeeId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", sessionCookie);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }


}

