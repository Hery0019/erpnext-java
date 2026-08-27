package hery.itu.erp.service.salary;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;

@Service
public class SalaryReportService {

    /** Rapport ERPNext « Salary Register » (personnalisé côté serveur : filtres component/signe/combien). */
    private static final String REPORT_NAME = "Salary Register";
    private static final String QUERY_REPORT_RUN = "frappe.desk.query_report.run";

    private final ErpNextClient client;

    public SalaryReportService(ErpNextClient client) {
        this.client = client;
    }

    /** Réponse brute du rapport ({@code message.result}, {@code message.columns}…). */
    public Map<String, Object> getSalaryReport(LocalDate fromDate, LocalDate toDate, String company,
                                               String employee, String salaryComponent, String signe, double combien) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("from_date", fromDate.toString());
        filters.put("to_date", toDate.toString());
        filters.put("company", company);
        if (!employee.isEmpty()) filters.put("employee", employee);
        if (!salaryComponent.isEmpty()) filters.put("component", salaryComponent);
        if (!signe.isEmpty()) filters.put("signe", signe);
        if (combien > 0) filters.put("combien", combien);

        JsonNode response = client.callMethod(QUERY_REPORT_RUN, Map.of(
                "report_name", REPORT_NAME,
                "filters", filters));
        return client.convert(response, new TypeReference<Map<String, Object>>() { });
    }

    /** Noms des Salary Component au format snake_case (clés des colonnes du rapport). */
    public List<String> getFormattedSalaryComponentNames() {
        return client.list("Salary Component")
                .fields("name")
                .orderBy("name asc")
                .fetchAll().stream()
                .map(node -> node.path("name").asText().toLowerCase().replace(" ", "_"))
                .toList();
    }
}
