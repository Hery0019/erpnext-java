package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
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
import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.service.login.LoginService;

class SalaryTotalServiceTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private MockRestServiceServer server;
    private SalaryTotalService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextClient client = new ErpNextClient(new RestTemplateBuilder(customizer),
                new ErpNextProperties("http://erp.test"), loginService, new ObjectMapper());
        service = new SalaryTotalService(client);
        server = customizer.getServer();
    }

    @Test
    void leMoisDUnSlipDetailleEstCeluiDeSaPeriodePasDeSaDateDeComptabilisation() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip/Sal%20Slip/HR-EMP-00001/00001"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00001\",\"employee\":\"HR-EMP-00001\","
                        + "\"start_date\":\"2025-01-01\",\"end_date\":\"2025-01-31\",\"posting_date\":\"2025-02-01\","
                        + "\"gross_pay\":1500.5,\"net_pay\":1200.25,\"total_deduction\":300.25,"
                        + "\"earnings\":[{\"salary_component\":\"Basic\",\"amount\":1500.5}],"
                        + "\"deductions\":[{\"salary_component\":\"Tax\",\"amount\":300.25}]}}", JSON));

        SalaryDTO slip = service.getSalarySlipDetail("Sal Slip/HR-EMP-00001/00001");

        assertThat(slip.getMonth()).isEqualTo("2025-01");
        assertThat(slip.getPostingDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(slip.getGrossPay()).isEqualByComparingTo("1500.5");
        assertThat(slip.getEarnings()).hasSize(1);
        assertThat(slip.getDeductions().get(0).getAmount()).isEqualByComparingTo("300.25");
    }

    @Test
    void laListeUtiliseLaMemeRegleDeMois() {
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Slip?")))
                .andRespond(withSuccess("{\"data\":[{\"name\":\"Sal Slip/HR-EMP-00001/00001\",\"employee\":\"HR-EMP-00001\","
                        + "\"start_date\":\"2025-01-01\",\"posting_date\":\"2025-02-01\",\"gross_pay\":1500.5,\"net_pay\":1200.25,\"total_deduction\":300.25}]}", JSON));

        List<SalaryDTO> all = service.getAllSalary();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getMonth()).isEqualTo("2025-01");
    }
}
