package hery.itu.erp.service.salary;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.model.salary.SalarySlip;
import hery.itu.erp.model.salary.SalarySlipListResponse;
import hery.itu.erp.service.login.LoginService;

@Service
public class SalarySlipService {

    private final RestTemplate restTemplate;
    private final LoginService loginService;

    private final String BASE_URL = "http://erpnext.localhost:8000/api/resource";

    public SalarySlipService(RestTemplate restTemplate, LoginService loginService) {
        this.restTemplate = restTemplate;
        this.loginService = loginService;
    }

    /**
     * ✅ Retourne la liste des salary slip names pour un employé
     */
    public List<String> getSalarySlipNamesByEmployee(String employeeId) {
        String url = BASE_URL + "/Salary Slip?fields=[\"name\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]&limit_page_length=100";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SalarySlipListResponse> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            SalarySlipListResponse.class
        );

        // Extraire les noms seulement
        return response.getBody().getData().stream()
            .map(SalarySlip::getName)
            .toList();
    }

    /**
     * ✅ Retourne un objet SalarySlip complet à partir du nom
     */
    

    public SalarySlip getSalarySlipDetail(String salarySlipName) {
        String url = BASE_URL + "/Salary Slip/" + salarySlipName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

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
                SalarySlip slip = new SalarySlip();
                slip.setName(data.path("name").asText());
                slip.setEmployee(data.path("employee").asText());
                slip.setEmployee_name(data.path("employee_name").asText());
                slip.setStart_date(data.path("start_date").asText());
                slip.setEnd_date(data.path("end_date").asText());
                slip.setGross_pay(data.path("gross_pay").asDouble());
                slip.setNet_pay(data.path("net_pay").asDouble());
                slip.setPosting_date(data.path("posting_date").asText());
                slip.setStatus(data.path("status").asText());
                slip.setSalary_structure(data.path("salary_structure").asText());
                slip.setCompany(data.path("company").asText());
                return slip;
            }
        }

        return null; // Si non trouvé ou erreur
    }

    /**
     * ✅ Nouvelle méthode combinée : retourne tous les SalarySlip détaillés d’un employé
     */
    public List<SalarySlip> getSalarySlipsByEmployee(String employeeId) {
        List<String> names = getSalarySlipNamesByEmployee(employeeId);
    
        return names.stream()
            .map(name -> {
                SalarySlip original = getSalarySlipDetail(name);
                SalarySlip copy = new SalarySlip();
                
                copy.setName(original.getName());
                copy.setEmployee(original.getEmployee());
                copy.setEmployee_name(original.getEmployee_name());
                copy.setStart_date(original.getStart_date());
                copy.setEnd_date(original.getEnd_date());
                copy.setGross_pay(original.getGross_pay());
                copy.setNet_pay(original.getNet_pay());
                copy.setPosting_date(original.getPosting_date());
                copy.setStatus(original.getStatus());
                copy.setSalary_structure(original.getSalary_structure());
                copy.setCompany(original.getCompany());
    
                return copy;
            })
            .toList();
    }
    
}
