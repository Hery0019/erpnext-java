package hery.itu.erp.service.salary;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.salary.SalarySlip;

@Service
public class SalarySlipService {

    private static final String SALARY_SLIP = "Salary Slip";

    private final ErpNextClient client;

    public SalarySlipService(ErpNextClient client) {
        this.client = client;
    }

    /** Noms de tous les Salary Slip d'un employé, par période croissante. */
    public List<String> getSalarySlipNamesByEmployee(String employeeId) {
        return client.list(SALARY_SLIP)
                .fields("name")
                .filters(Filters.where("employee", "=", employeeId))
                .orderBy("start_date asc")
                .fetchAll().stream()
                .map(node -> node.path("name").asText())
                .toList();
    }

    /**
     * @throws hery.itu.erp.erpnext.ErpNextNotFoundException si le slip n'existe pas
     */
    public SalarySlip getSalarySlipDetail(String salarySlipName) {
        return toSalarySlip(client.getDoc(SALARY_SLIP, salarySlipName));
    }

    /** Slips détaillés d'un employé (un appel par slip ; voir 3.4 / 5.1). */
    public List<SalarySlip> getSalarySlipsByEmployee(String employeeId) {
        return getSalarySlipNamesByEmployee(employeeId).stream()
                .map(this::getSalarySlipDetail)
                .toList();
    }

    private static SalarySlip toSalarySlip(JsonNode data) {
        SalarySlip slip = new SalarySlip();
        slip.setName(data.path("name").asText(null));
        slip.setEmployee(data.path("employee").asText(null));
        slip.setEmployee_name(data.path("employee_name").asText(null));
        slip.setStart_date(data.path("start_date").asText(null));
        slip.setEnd_date(data.path("end_date").asText(null));
        slip.setGross_pay(data.path("gross_pay").asDouble(0.0));
        slip.setNet_pay(data.path("net_pay").asDouble(0.0));
        slip.setPosting_date(data.path("posting_date").asText(null));
        slip.setStatus(data.path("status").asText(null));
        slip.setSalary_structure(data.path("salary_structure").asText(null));
        slip.setCompany(data.path("company").asText(null));
        return slip;
    }
}
