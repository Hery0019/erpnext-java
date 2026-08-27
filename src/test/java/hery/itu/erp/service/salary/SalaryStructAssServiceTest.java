package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

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
import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.login.LoginService;

class SalaryStructAssServiceTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private MockRestServiceServer server;
    private SalaryStructAssService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextClient client = new ErpNextClient(new RestTemplateBuilder(customizer),
                new ErpNextProperties("http://erp.test"), loginService, new ObjectMapper());
        service = new SalaryStructAssService(client);
        server = customizer.getServer();
    }

    @Test
    void getAssignmentByIdMappeLeDocumentAvecUneBaseExacte() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment/HR-SSA-2025-00001"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"HR-SSA-2025-00001\",\"employee\":\"HR-EMP-00001\","
                        + "\"salary_structure\":\"Standard\",\"base\":1234.56,\"docstatus\":1,\"champ_inconnu\":1}}", JSON));

        SalaryStructAss ass = service.getAssignmentById("HR-SSA-2025-00001");

        assertThat(ass.getName()).isEqualTo("HR-SSA-2025-00001");
        assertThat(ass.getEmployee()).isEqualTo("HR-EMP-00001");
        assertThat(ass.getBase()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(ass.getDocstatus()).isEqualTo(1);
    }

    @Test
    void creationDuSsaUtiliseLeNomRenvoyeParErpNextPourLeSoumettre() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"employee\":\"HR-EMP-00001\",\"salary_structure\":\"Standard\","
                        + "\"company\":\"Orinasa SA\",\"currency\":\"MGA\",\"base\":\"1500\",\"from_date\":\"2025-03-01\",\"to_date\":\"2025-03-31\"}"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"HR-SSA-2025-00007\"}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment/HR-SSA-2025-00007?run_method=submit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", JSON));

        SalaryStructAss ass = new SalaryStructAss();
        ass.setEmployee("HR-EMP-00001");
        ass.setSalary_structure("Standard");
        ass.setCompany("Orinasa SA");
        ass.setCurrency("MGA");
        ass.setBase(new BigDecimal("1500"));
        ass.setFrom_date("2025-03-01");
        ass.setTo_date("2025-03-31");

        assertThat(service.createSalaryStructureAssignmentAndSubmit(ass)).isEqualTo("HR-SSA-2025-00007");
        server.verify();
    }

    @Test
    void leSlipEstSoumisSousLeNomAttribueParErpNext() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getBody().toString()).doesNotContain("\"name\""))
                .andExpect(content().json("{\"employee\":\"HR-EMP-00001\",\"start_date\":\"2025-03-01\","
                        + "\"end_date\":\"2025-03-31\",\"posting_date\":\"2025-03-31\",\"payroll_frequency\":\"Monthly\"}", false))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00009\"}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip/Sal%20Slip/HR-EMP-00001/00009?run_method=submit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", JSON));

        SalaryStructAss ass = assignment("HR-EMP-00001", "2025-03-01", "2025-03-31", "1500");

        assertThat(service.createSalarySlipAndSubmit(ass)).isEqualTo("Sal Slip/HR-EMP-00001/00009");
        server.verify();
    }

    @Test
    void lExistenceDUnSsaIgnoreLesDocumentsAnnulesEtCibleLEmploye() {
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Structure%20Assignment?")))
                .andExpect(request -> assertThat(request.getURI().getQuery()).contains(
                        "filters=[[\"employee\",\"=\",\"HR-EMP-00001\"],[\"from_date\",\"=\",\"2025-03-01\"],[\"docstatus\",\"in\",[0,1]]]"))
                .andRespond(withSuccess("{\"data\":[]}", JSON));

        assertThat(service.findAssignmentName("HR-EMP-00001", "2025-03-01")).isEmpty();
    }

    @Test
    void generationSurUnMoisSansExistantCreeSsaPuisSlipAvecLesNomsRenvoyes() {
        // 1. existence du slip (aucun) — les annulés sont exclus
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Slip?")))
                .andExpect(request -> assertThat(request.getURI().getQuery()).contains(
                        "[\"employee\",\"=\",\"HR-EMP-00001\"],[\"start_date\",\"=\",\"2025-03-01\"],[\"end_date\",\"=\",\"2025-03-31\"],[\"docstatus\",\"in\",[0,1]]"))
                .andRespond(withSuccess("{\"data\":[]}", JSON));
        // 2. existence du SSA (aucun)
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Structure%20Assignment?")))
                .andRespond(withSuccess("{\"data\":[]}", JSON));
        // 3. création + soumission du SSA
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"name\":\"HR-SSA-2025-00010\"}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment/HR-SSA-2025-00010?run_method=submit"))
                .andRespond(withSuccess("{}", JSON));
        // 4. création + soumission du slip
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00003\"}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip/Sal%20Slip/HR-EMP-00001/00003?run_method=submit"))
                .andRespond(withSuccess("{}", JSON));

        SalaryStructAss ass = assignment("HR-EMP-00001", null, null, "1500");
        java.util.List<String> slips = service.generateSalary(ass,
                java.time.LocalDate.of(2025, 3, 10), java.time.LocalDate.of(2025, 3, 20), null, null);

        assertThat(slips).containsExactly("Sal Slip/HR-EMP-00001/00003");
        server.verify();
    }

    private static SalaryStructAss assignment(String employee, String from, String to, String base) {
        SalaryStructAss ass = new SalaryStructAss();
        ass.setEmployee(employee);
        ass.setSalary_structure("Standard");
        ass.setCompany("Orinasa SA");
        ass.setCurrency("MGA");
        ass.setBase(new BigDecimal(base));
        ass.setFrom_date(from);
        ass.setTo_date(to);
        ass.setPosting_date(to);
        return ass;
    }

    @Test
    void rechercheDuSsaActifFiltreParEmployeEtDate() {
        server.expect(requestTo(Matchers.startsWith("http://erp.test/api/resource/Salary%20Structure%20Assignment?")))
                .andExpect(request -> assertThat(request.getURI().getQuery()).isEqualTo(
                        "fields=[\"name\",\"from_date\"]"
                        + "&filters=[[\"employee\",\"=\",\"HR-EMP-00001\"],[\"from_date\",\"<=\",\"2025-03-31\"]]"
                        + "&order_by=from_date desc&limit_start=0&limit_page_length=1"))
                .andRespond(withSuccess("{\"data\":[{\"name\":\"HR-SSA-2025-00003\",\"from_date\":\"2025-01-01\"}]}", JSON));

        String name = service.getSalaryStructureAssignmentByEmployeeAndDate(
                new SalaryFilterDTO("Sal Slip/HR-EMP-00001/00003", "HR-EMP-00001", "Basic", 1000.0, "2025-03-31"));

        assertThat(name).isEqualTo("HR-SSA-2025-00003");
    }
}
