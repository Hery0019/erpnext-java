package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.model.salary.SalarySlip;
import hery.itu.erp.service.login.LoginService;

class SalarySlipServiceTest {

    private MockRestServiceServer server;
    private SalarySlipService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextClient client = new ErpNextClient(new RestTemplateBuilder(customizer),
                new ErpNextProperties("http://erp.test"), loginService, new ObjectMapper());
        service = new SalarySlipService(client);
        server = customizer.getServer();
    }

    @Test
    void lesFichesDUnEmployeViennentDUneSeuleRequete() {
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Slip?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("\"gross_pay\",\"net_pay\"")
                        .contains("filters=[[\"employee\",\"=\",\"HR-EMP-00001\"]]")
                        .contains("order_by=start_date asc"))
                .andRespond(withSuccess("{\"data\":["
                        + "{\"name\":\"Sal Slip/HR-EMP-00001/00001\",\"employee\":\"HR-EMP-00001\",\"employee_name\":\"Ana\",\"start_date\":\"2025-01-01\",\"end_date\":\"2025-01-31\",\"gross_pay\":1000,\"net_pay\":900,\"status\":\"Submitted\"},"
                        + "{\"name\":\"Sal Slip/HR-EMP-00001/00002\",\"employee\":\"HR-EMP-00001\",\"employee_name\":\"Ana\",\"start_date\":\"2025-02-01\",\"end_date\":\"2025-02-28\",\"gross_pay\":1000,\"net_pay\":900,\"status\":\"Submitted\"}]}",
                        MediaType.APPLICATION_JSON));

        List<SalarySlip> slips = service.getSalarySlipsByEmployee("HR-EMP-00001");

        assertThat(slips).extracting(SalarySlip::getName)
                .containsExactly("Sal Slip/HR-EMP-00001/00001", "Sal Slip/HR-EMP-00001/00002");
        assertThat(slips.get(0).getGross_pay()).isEqualTo(1000.0);
        assertThat(slips.get(0).getEmployee_name()).isEqualTo("Ana");
        server.verify(); // pas de GET par fiche
    }
}
