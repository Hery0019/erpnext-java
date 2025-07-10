package hery.itu.erp.service.salary;

import java.net.URI;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.model.salary.SalaryDetail;
import hery.itu.erp.service.login.LoginService;

@Service
public class SalaryTotalService {

    private final RestTemplate restTemplate;
    private final LoginService loginService;

    private final String baseUrl = "http://erpnext.localhost:8000";

    public SalaryTotalService(RestTemplate restTemplate, LoginService loginService) {
        this.restTemplate = restTemplate;
        this.loginService = loginService;
    }

    public List<SalaryDTO> getSalarySlipsByMonth(int year, int month) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        YearMonth yearMonth = YearMonth.of(year, month);
        String startDate = yearMonth.atDay(1).toString();
        String endDate = yearMonth.atEndOfMonth().toString();

        String filters = String.format("[[\"posting_date\",\"between\",[\"%s\",\"%s\"]]]", startDate, endDate);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/resource/Salary Slip")
                .queryParam("filters", filters)
                .queryParam("limit_page_length", 1000)
                .build()
                .encode()
                .toUri();

        System.out.println("Appel API par mois : " + uri);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                Map.class);

        List<SalaryDTO> salaries = new ArrayList<>();
        if (response.getBody() != null && response.getBody().get("data") != null) {
            List<Map<String, Object>> salarySlips = (List<Map<String, Object>>) response.getBody().get("data");
            for (Map<String, Object> slipData : salarySlips) {
                String slipName = (String) slipData.get("name");
                SalaryDTO slip = getSalarySlipDetail(slipName);
                if (slip != null) {
                    salaries.add(slip);
                }
            }
        }

        return salaries;
    }

    public SalaryDTO getSalarySlipDetail(String slipName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/api/resource/Salary Slip/" + slipName + "?limit_page_length=1000";
        System.out.println("Appel API : " + url);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class);

        JsonNode data = response.getBody().path("data");
        if (data.isMissingNode()) {
            System.out.println("Aucune donnée trouvée pour " + slipName);
            return null;
        }

        SalaryDTO slip = new SalaryDTO();
        slip.setSlipName(data.path("name").asText(null));
        slip.setEmployeeId(data.path("employee").asText(null));
        slip.setEmployeeName(data.path("employee_name").asText(null));
        slip.setCompany(data.path("company").asText(null));
        if (data.has("posting_date")) {
            LocalDate start = LocalDate.parse(data.path("posting_date").asText(), DateTimeFormatter.ISO_DATE);
            slip.setMonth(start.getYear() + "-" + String.format("%02d", start.getMonthValue()));
            slip.setPostingDate(start);
        }
        slip.setStatus(data.path("status").asText(null));
        slip.setCurrency(data.path("currency").asText(null));
        slip.setGrossPay(BigDecimal.valueOf(data.path("gross_pay").asDouble(0.0)));
        slip.setNetPay(BigDecimal.valueOf(data.path("net_pay").asDouble(0.0)));
        slip.setTotalDeduction(BigDecimal.valueOf(data.path("total_deduction").asDouble(0.0)));
        slip.setMonthToDate(BigDecimal.valueOf(data.path("month_to_date").asDouble(0.0)));
        slip.setYearToDate(BigDecimal.valueOf(data.path("year_to_date").asDouble(0.0)));
        slip.setTotalInWords(data.path("total_in_words").asText(null));

        if (data.has("earnings") && data.get("earnings").isArray()) {
            for (JsonNode earningNode : data.get("earnings")) {
                SalaryDetail earning = new SalaryDetail();
                earning.setSalaryComponent(earningNode.path("salary_component").asText(null));
                earning.setAmount(BigDecimal.valueOf(earningNode.path("amount").asDouble(0.0)));
                earning.setType(SalaryDetail.Type.EARNING);
                slip.getEarnings().add(earning);
            }
        }

        if (data.has("deductions") && data.get("deductions").isArray()) {
            for (JsonNode deductionNode : data.get("deductions")) {
                SalaryDetail deduction = new SalaryDetail();
                deduction.setSalaryComponent(deductionNode.path("salary_component").asText(null));
                deduction.setAmount(BigDecimal.valueOf(deductionNode.path("amount").asDouble(0.0)));
                deduction.setType(SalaryDetail.Type.DEDUCTION);
                slip.getDeductions().add(deduction);
            }
        }

        return slip;
    }

    public List<SalaryDTO> getAllSalary() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = baseUrl
                + "/api/resource/Salary Slip?"
                + "fields=[\"name\",\"employee\",\"employee_name\",\"posting_date\",\"start_date\",\"gross_pay\",\"net_pay\",\"total_deduction\",\"total_earnings\",\"status\"]"
                + "&limit_page_length=1000";

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class);

        List<SalaryDTO> salaries = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            if (data != null) {
                DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE;
                for (Map<String, Object> item : data) {
                    SalaryDTO slip = new SalaryDTO();
                    slip.setSlipName((String) item.get("name"));
                    slip.setEmployeeId((String) item.get("employee"));
                    slip.setEmployeeName((String) item.get("employee_name"));

                    if (item.get("posting_date") != null) {
                        LocalDate dt = LocalDate.parse(item.get("posting_date").toString(), isoFormatter);
                        slip.setPostingDate(dt);
                    }

                    if (item.get("start_date") != null) {
                        LocalDate start = LocalDate.parse(item.get("start_date").toString(), isoFormatter);
                        slip.setMonth(start.getYear() + "-" + String.format("%02d", start.getMonthValue()));
                    }

                    if (item.get("gross_pay") != null) {
                        slip.setGrossPay(new BigDecimal(item.get("gross_pay").toString()));
                    }
                    if (item.get("net_pay") != null) {
                        slip.setNetPay(new BigDecimal(item.get("net_pay").toString()));
                    }
                    if (item.get("total_deduction") != null) {
                        slip.setTotalDeduction(new BigDecimal(item.get("total_deduction").toString()));
                    }

                    slip.setStatus((String) item.get("status"));

                    salaries.add(slip);
                }
            }
        }

        return salaries;
    }

}
