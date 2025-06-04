package hery.itu.erp.service.salary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import hery.itu.erp.model.salary.SalaryReport;
import hery.itu.erp.service.login.LoginService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalaryReportService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginService loginService; // pour le cookie de session ERPNext

    

    // Tu laisses cette méthode telle quelle (c’est ton appel HTTP)
    public Map<String, Object> getSalaryReport(LocalDate fromDate, LocalDate toDate, String company) {
        String url = "http://erpnext.localhost:8000/api/method/frappe.desk.query_report.run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", loginService.getSessionCookie());

        Map<String, Object> filters = Map.of(
            "report_name", "Salary Register",
            "filters", Map.of(
                "from_date", fromDate.toString(),
                "to_date", toDate.toString(),
                "company", company
            )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(filters, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getBody();
    }

    public List<String> getFormattedSalaryComponentNames() {
        String url = "http://erpnext.localhost:8000/api/resource/Salary Component?fields=[\"name\"]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie()); 

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        List<String> formattedNames = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK) {
            List<Map<String, String>> data = (List<Map<String, String>>) response.getBody().get("data");

            for (Map<String, String> item : data) {
                String originalName = item.get("name");
                String formatted = originalName.toLowerCase().replace(" ", "_");
                formattedNames.add(formatted);
            }
        }

        return formattedNames;
    }


}
