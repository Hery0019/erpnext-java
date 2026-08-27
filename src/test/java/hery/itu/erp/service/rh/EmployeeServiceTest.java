package hery.itu.erp.service.rh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.service.login.LoginService;

class EmployeeServiceTest {

    private MockRestServiceServer server;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextClient client = new ErpNextClient(new RestTemplateBuilder(customizer),
                new ErpNextProperties("http://erp.test"), loginService, new ObjectMapper());
        service = new EmployeeService(client);
        server = customizer.getServer();
    }

    @Test
    void leFiltrePasseDansLUrlEncodeEtSansRechargerChaqueEmploye() throws Exception {
        String hostile = "a\"],[\"name\",\"!=\",\"";
        String expectedFilters = new ObjectMapper().writeValueAsString(List.of(
                List.of("first_name", "like", "%" + hostile + "%"),
                List.of("date_of_joining", ">=", "2024-01-01")));

        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Employee?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getURI().getQuery()).contains("filters=" + expectedFilters))
                .andRespond(withSuccess(
                        "{\"data\":[{\"name\":\"HR-EMP-00001\",\"first_name\":\"Ana\",\"employee_name\":\"Ana Bé\",\"company\":\"Orinasa SA\"}]}",
                        MediaType.APPLICATION_JSON));

        List<Employee> employees = service.filterEmployees(hostile, null, "", null, "2024-01-01", null, null);

        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getName()).isEqualTo("HR-EMP-00001");
        assertThat(employees.get(0).getFirst_name()).isEqualTo("Ana");
        assertThat(employees.get(0).getEmployee_name()).isEqualTo("Ana Bé");
        server.verify(); // une seule requête : plus de GET par employé
    }
}
