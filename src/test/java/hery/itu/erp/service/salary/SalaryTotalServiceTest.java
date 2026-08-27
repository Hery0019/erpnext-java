package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.model.salary.SalaryDetail;
import hery.itu.erp.model.salary.SalaryGroupedDTO;
import hery.itu.erp.service.login.LoginService;

class SalaryTotalServiceTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String SLIP_URL = "http://erp.test/api/resource/Salary%20Slip";
    private static final String DETAIL_URL = "http://erp.test/api/resource/Salary%20Detail";
    private static final String SLIP_ROW = "{\"name\":\"Sal Slip/HR-EMP-00001/00003\",\"employee\":\"HR-EMP-00001\","
            + "\"employee_name\":\"Ana\",\"start_date\":\"2025-03-01\",\"posting_date\":\"2025-04-01\","
            + "\"gross_pay\":1500.5,\"net_pay\":1200.25,\"total_deduction\":300.25}";

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
    void lesFichesDUnMoisEtLeursComposantsViennentDeDeuxRequetes() {
        server.expect(requestTo(Matchers.startsWith(SLIP_URL + "?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("[\"start_date\",\"between\",[\"2025-03-01\",\"2025-03-31\"]]")
                        .contains("[\"docstatus\",\"in\",[0,1]]"))
                .andRespond(withSuccess("{\"data\":[" + SLIP_ROW + "]}", JSON));
        server.expect(requestTo(Matchers.startsWith(DETAIL_URL + "?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("parent=Salary Slip")
                        .contains("[\"parent\",\"in\",[\"Sal Slip/HR-EMP-00001/00003\"]]"))
                .andRespond(withSuccess("{\"data\":["
                        + "{\"parent\":\"Sal Slip/HR-EMP-00001/00003\",\"parentfield\":\"earnings\",\"salary_component\":\"Basic\",\"amount\":1500.5},"
                        + "{\"parent\":\"Sal Slip/HR-EMP-00001/00003\",\"parentfield\":\"deductions\",\"salary_component\":\"Tax\",\"amount\":300.25}]}", JSON));

        List<SalaryDTO> slips = service.getDetailedSlips(2025, 3);

        assertThat(slips).hasSize(1);
        SalaryDTO slip = slips.get(0);
        assertThat(slip.getMonth()).isEqualTo("2025-03"); // période, pas la date de comptabilisation (avril)
        assertThat(slip.getPostingDate()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(slip.getEarnings()).extracting(SalaryDetail::getSalaryComponent).containsExactly("Basic");
        assertThat(slip.getDeductions()).extracting(SalaryDetail::getAmount).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("300.25"));
        server.verify(); // aucun GET par fiche
    }

    @Test
    void siLaTableEnfantEstRefuseeOnRetombeSurUnAppelParFiche() {
        server.expect(requestTo(Matchers.startsWith(SLIP_URL + "?")))
                .andRespond(withSuccess("{\"data\":[" + SLIP_ROW + "]}", JSON));
        server.expect(requestTo(Matchers.startsWith(DETAIL_URL + "?")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .body("{\"exception\":\"frappe.exceptions.PermissionError: Not permitted\"}").contentType(JSON));
        server.expect(requestTo(SLIP_URL + "/Sal%20Slip/HR-EMP-00001/00003"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00003\","
                        + "\"earnings\":[{\"salary_component\":\"Basic\",\"amount\":1500.5}],\"deductions\":[]}}", JSON));

        List<SalaryDTO> slips = service.getDetailedSlips(2025, 3);

        assertThat(slips.get(0).getEarnings()).extracting(SalaryDetail::getSalaryComponent).containsExactly("Basic");
        server.verify();
    }

    @Test
    void uneAnneeEntiereEstDemandeeEnUneSeulePeriode() {
        server.expect(requestTo(Matchers.startsWith(SLIP_URL + "?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("[\"start_date\",\"between\",[\"2025-01-01\",\"2025-12-31\"]]"))
                .andRespond(withSuccess("{\"data\":[]}", JSON));

        assertThat(service.getDetailedSlips(2025, null)).isEmpty();
        server.verify(); // pas de requête enfant sans fiche
    }

    @Test
    void leRegroupementParMoisCumuleTotauxEtComposants() {
        SalaryDTO janA = slip("2025-01", "1000", "100", "900", "Basic", "1000", "Tax", "100");
        SalaryDTO janB = slip("2025-01", "2000", "300", "1700", "Basic", "2000", "Tax", "300");
        SalaryDTO fev = slip("2025-02", "1000", "100", "900", "Basic", "1000", "Tax", "100");

        Map<String, SalaryGroupedDTO> grouped = service.groupByMonth(List.of(fev, janA, janB));

        assertThat(grouped.keySet()).containsExactly("2025-01", "2025-02");
        SalaryGroupedDTO jan = grouped.get("2025-01");
        assertThat(jan.getTotalGross()).isEqualByComparingTo("3000");
        assertThat(jan.getTotalDeduction()).isEqualByComparingTo("400");
        assertThat(jan.getTotalNet()).isEqualByComparingTo("2600");
        assertThat(jan.getEarningsTotal().get("Basic")).isEqualByComparingTo("3000");
        assertThat(jan.getDeductionsTotal().get("Tax")).isEqualByComparingTo("400");
    }

    @Test
    void leMoisDUnSlipDetailleEstCeluiDeSaPeriodePasDeSaDateDeComptabilisation() {
        server.expect(requestTo(SLIP_URL + "/Sal%20Slip/HR-EMP-00001/00001"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00001\",\"employee\":\"HR-EMP-00001\","
                        + "\"start_date\":\"2025-01-01\",\"end_date\":\"2025-01-31\",\"posting_date\":\"2025-02-01\","
                        + "\"gross_pay\":1500.5,\"net_pay\":1200.25,\"total_deduction\":300.25,"
                        + "\"earnings\":[{\"salary_component\":\"Basic\",\"amount\":1500.5}],"
                        + "\"deductions\":[{\"salary_component\":\"Tax\",\"amount\":300.25}]}}", JSON));

        SalaryDTO slip = service.getSalarySlipDetail("Sal Slip/HR-EMP-00001/00001");

        assertThat(slip.getMonth()).isEqualTo("2025-01");
        assertThat(slip.getGrossPay()).isEqualByComparingTo("1500.5");
        assertThat(slip.getEarnings()).hasSize(1);
        assertThat(slip.getDeductions().get(0).getAmount()).isEqualByComparingTo("300.25");
    }

    private static SalaryDTO slip(String month, String gross, String deduction, String net,
                                  String earningName, String earningAmount, String deductionName, String deductionAmount) {
        SalaryDTO s = new SalaryDTO();
        s.setMonth(month);
        s.setGrossPay(new BigDecimal(gross));
        s.setTotalDeduction(new BigDecimal(deduction));
        s.setNetPay(new BigDecimal(net));
        SalaryDetail e = new SalaryDetail();
        e.setSalaryComponent(earningName);
        e.setAmount(new BigDecimal(earningAmount));
        s.getEarnings().add(e);
        SalaryDetail d = new SalaryDetail();
        d.setSalaryComponent(deductionName);
        d.setAmount(new BigDecimal(deductionAmount));
        s.getDeductions().add(d);
        return s;
    }
}
