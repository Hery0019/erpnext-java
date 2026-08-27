package hery.itu.erp.service.salary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.model.salary.SalaryDetail;

@Service
public class SalaryTotalService {

    private static final String SALARY_SLIP = "Salary Slip";

    private final ErpNextClient client;

    public SalaryTotalService(ErpNextClient client) {
        this.client = client;
    }

    /** Slips détaillés dont la {@code posting_date} tombe dans le mois (un appel par slip ; voir 5.1). */
    public List<SalaryDTO> getSalarySlipsByMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return client.list(SALARY_SLIP)
                .fields("name")
                .filters(Filters.none().between("posting_date", yearMonth.atDay(1).toString(), yearMonth.atEndOfMonth().toString()))
                .orderBy("posting_date asc")
                .fetchAll().stream()
                .map(node -> getSalarySlipDetail(node.path("name").asText()))
                .toList();
    }

    /**
     * @throws hery.itu.erp.erpnext.ErpNextNotFoundException si le slip n'existe pas
     */
    public SalaryDTO getSalarySlipDetail(String slipName) {
        JsonNode data = client.getDoc(SALARY_SLIP, slipName);

        SalaryDTO slip = new SalaryDTO();
        slip.setSlipName(data.path("name").asText(null));
        slip.setEmployeeId(data.path("employee").asText(null));
        slip.setEmployeeName(data.path("employee_name").asText(null));
        slip.setCompany(data.path("company").asText(null));
        String postingDate = data.path("posting_date").asText(null);
        if (postingDate != null) {
            LocalDate date = LocalDate.parse(postingDate);
            slip.setMonth(toMonthKey(date));
            slip.setPostingDate(date);
        }
        slip.setStatus(data.path("status").asText(null));
        slip.setCurrency(data.path("currency").asText(null));
        slip.setGrossPay(data.path("gross_pay").decimalValue());
        slip.setNetPay(data.path("net_pay").decimalValue());
        slip.setTotalDeduction(data.path("total_deduction").decimalValue());
        slip.setMonthToDate(data.path("month_to_date").decimalValue());
        slip.setYearToDate(data.path("year_to_date").decimalValue());
        slip.setTotalInWords(data.path("total_in_words").asText(null));

        for (JsonNode earningNode : data.path("earnings")) {
            slip.getEarnings().add(toDetail(earningNode, SalaryDetail.Type.EARNING));
        }
        for (JsonNode deductionNode : data.path("deductions")) {
            slip.getDeductions().add(toDetail(deductionNode, SalaryDetail.Type.DEDUCTION));
        }
        return slip;
    }

    /** Tous les slips (sans le détail des composants), par période croissante. */
    public List<SalaryDTO> getAllSalary() {
        return client.list(SALARY_SLIP)
                .fields("name", "employee", "employee_name", "posting_date", "start_date",
                        "gross_pay", "net_pay", "total_deduction", "total_earnings", "status")
                .orderBy("start_date asc")
                .fetchAll().stream()
                .map(SalaryTotalService::toSummary)
                .toList();
    }

    private static SalaryDTO toSummary(JsonNode item) {
        SalaryDTO slip = new SalaryDTO();
        slip.setSlipName(item.path("name").asText(null));
        slip.setEmployeeId(item.path("employee").asText(null));
        slip.setEmployeeName(item.path("employee_name").asText(null));
        String postingDate = item.path("posting_date").asText(null);
        if (postingDate != null) {
            slip.setPostingDate(LocalDate.parse(postingDate));
        }
        String startDate = item.path("start_date").asText(null);
        if (startDate != null) {
            slip.setMonth(toMonthKey(LocalDate.parse(startDate)));
        }
        slip.setGrossPay(item.path("gross_pay").decimalValue());
        slip.setNetPay(item.path("net_pay").decimalValue());
        slip.setTotalDeduction(item.path("total_deduction").decimalValue());
        slip.setStatus(item.path("status").asText(null));
        return slip;
    }

    private static SalaryDetail toDetail(JsonNode node, SalaryDetail.Type type) {
        SalaryDetail detail = new SalaryDetail();
        detail.setSalaryComponent(node.path("salary_component").asText(null));
        detail.setAmount(node.path("amount").decimalValue());
        detail.setType(type);
        return detail;
    }

    private static String toMonthKey(LocalDate date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }
}
