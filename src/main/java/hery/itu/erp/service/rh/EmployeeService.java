package hery.itu.erp.service.rh;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.rh.Employee;

@Service
public class EmployeeService {

    private static final String EMPLOYEE = "Employee";
    private static final List<String> FIELDS = List.of(
            "name", "first_name", "middle_name", "last_name", "employee_name",
            "date_of_birth", "date_of_joining", "status", "gender", "company");

    private final ErpNextClient client;

    public EmployeeService(ErpNextClient client) {
        this.client = client;
    }

    public List<Employee> getImportantEmployees() {
        return client.list(EMPLOYEE)
                .fields(FIELDS)
                .orderBy("employee_name asc")
                .fetchAll().stream()
                .map(this::toEmployee)
                .toList();
    }

    public List<Employee> filterEmployees(String firstName, String lastName, String gender, String status,
                                          String dateStart, String dateEnd, String company) {
        Filters filters = Filters.none();
        if (hasText(firstName)) {
            filters.like("first_name", "%" + firstName + "%");
        }
        if (hasText(lastName)) {
            filters.like("last_name", "%" + lastName + "%");
        }
        if (hasText(gender)) {
            filters.eq("gender", gender);
        }
        if (hasText(status)) {
            filters.eq("status", status);
        }
        if (hasText(company)) {
            filters.like("company", "%" + company + "%");
        }
        if (hasText(dateStart)) {
            filters.gte("date_of_joining", dateStart);
        }
        if (hasText(dateEnd)) {
            filters.lte("date_of_joining", dateEnd);
        }

        return client.list(EMPLOYEE)
                .fields(FIELDS)
                .filters(filters)
                .orderBy("employee_name asc")
                .fetchAll().stream()
                .map(this::toEmployee)
                .toList();
    }

    public Employee getEmployeeByName(String employeeName) {
        return client.getDoc(EMPLOYEE, employeeName, Employee.class);
    }

    /**
     * @throws hery.itu.erp.erpnext.ErpNextException si ERPNext refuse la création
     */
    public void createEmployee(Employee employee) {
        client.insert(EMPLOYEE, employee);
    }

    public void deleteEmploye(String employeeId) {
        client.delete(EMPLOYEE, employeeId);
    }

    private Employee toEmployee(JsonNode node) {
        return client.convert(node, Employee.class);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
