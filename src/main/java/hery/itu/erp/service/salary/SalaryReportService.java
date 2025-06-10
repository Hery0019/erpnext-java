package hery.itu.erp.service.salary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import hery.itu.erp.service.login.LoginService;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class SalaryReportService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginService loginService;

    // Appel brut du rapport depuis ERPNext
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

    // Récupère les noms formatés (snake_case) des Salary Components
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

    public List<Map<String, Object>> getMonthlySalarySummaryByYear(int year, String company) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> componentNames = getFormattedSalaryComponentNames();

        for (int month = 1; month <= 12; month++) {
            LocalDate fromDate = LocalDate.of(year, month, 1);
            LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());

            Map<String, Object> reportData = getSalaryReport(fromDate, toDate, company);

            Object messageObj = reportData.get("message");
            if (!(messageObj instanceof Map<?, ?> message)) continue;

            Object resultObj = message.get("result");
            if (!(resultObj instanceof List<?> resultList)) continue;

            // Initialiser les totaux à 0 pour chaque composant
            Map<String, Object> monthlySummary = new LinkedHashMap<>();
            monthlySummary.put("mois", Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH));

            for (String comp : componentNames) {
                monthlySummary.put(comp, 0.0);
            }

            for (Object rowObj : resultList) {
                if (!(rowObj instanceof Map<?, ?> row)) continue;

                for (String comp : componentNames) {
                    Object valObj = row.get(comp);
                    if (valObj instanceof Number val) {
                        Double current = (Double) monthlySummary.get(comp);
                        monthlySummary.put(comp, current + val.doubleValue());
                    }
                }
            }

            result.add(monthlySummary);
        }

        return result;
    }

    
}
