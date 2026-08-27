package hery.itu.erp.service.salary;

import java.util.List;

import org.springframework.stereotype.Service;

import hery.itu.erp.erpnext.ErpNextClient;

@Service
public class SalaryComponentService {

    private static final String SALARY_COMPONENT = "Salary Component";

    private final ErpNextClient client;

    public SalaryComponentService(ErpNextClient client) {
        this.client = client;
    }

    /** Noms de tous les Salary Component, triés. */
    public List<String> getAllSalaryComponentNames() {
        return client.list(SALARY_COMPONENT)
                .fields("name")
                .orderBy("name asc")
                .fetchAll().stream()
                .map(node -> node.path("name").asText())
                .toList();
    }
}
