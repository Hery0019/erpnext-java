package hery.itu.erp.erpnext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.service.login.LoginService;

class ErpNextClientTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private MockRestServiceServer server;
    private ErpNextClient client;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextProperties properties = new ErpNextProperties("http://erp.test/", Duration.ofSeconds(1), Duration.ofSeconds(1), 2);
        client = new ErpNextClient(new RestTemplateBuilder(customizer), properties, loginService, new ObjectMapper());
        server = customizer.getServer();
    }

    /** Compare la query string décodée (java.net.URI décode les %xx). */
    private static RequestMatcher decodedQuery(String expected) {
        return request -> assertThat(request.getURI().getQuery()).isEqualTo(expected);
    }

    @Test
    void listeEncodeDoctypeChampsFiltresEtEnvoieLeCookie() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://erp.test/api/resource/Salary%20Slip?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Cookie", "sid=abc"))
                .andExpect(decodedQuery("fields=[\"name\",\"employee\"]"
                        + "&filters=[[\"employee\",\"=\",\"HR-EMP-00001\"],[\"docstatus\",\"in\",[0,1]]]"
                        + "&order_by=creation desc&limit_start=0&limit_page_length=1"))
                .andRespond(withSuccess("{\"data\":[{\"name\":\"Sal Slip/HR-EMP-00001/00001\"}]}", JSON));

        List<JsonNode> rows = client.list("Salary Slip")
                .fields("name", "employee")
                .filters(Filters.where("employee", "=", "HR-EMP-00001").in("docstatus", List.of(0, 1)))
                .orderBy("creation desc")
                .limit(1)
                .fetch();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).path("name").asText()).isEqualTo("Sal Slip/HR-EMP-00001/00001");
        server.verify();
    }

    @Test
    void uneValeurDeFiltreHostileResteUneSimpleChaine() throws Exception {
        String hostile = "x\"],[\"name\",\"!=\",\"";
        String expectedFilters = new ObjectMapper().writeValueAsString(List.of(List.of("supplier", "=", hostile)));

        server.expect(decodedQuery("fields=[\"name\"]&filters=" + expectedFilters + "&limit_start=0&limit_page_length=2"))
                .andRespond(withSuccess("{\"data\":[]}", JSON));

        assertThat(client.list("Purchase Order").filters(Filters.where("supplier", "=", hostile)).fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetchAllParcourtToutesLesPages() {
        server.expect(decodedQuery("fields=[\"name\"]&limit_start=0&limit_page_length=2"))
                .andRespond(withSuccess("{\"data\":[{\"name\":\"a\"},{\"name\":\"b\"}]}", JSON));
        server.expect(decodedQuery("fields=[\"name\"]&limit_start=2&limit_page_length=2"))
                .andRespond(withSuccess("{\"data\":[{\"name\":\"c\"}]}", JSON));

        List<JsonNode> all = client.list("Supplier").fetchAll();

        assertThat(all).extracting(n -> n.path("name").asText()).containsExactly("a", "b", "c");
        server.verify();
    }

    @Test
    void listChildRowsEnvoieLeParentEtDecoupeEnLotsDe100() throws Exception {
        List<String> names = java.util.stream.IntStream.rangeClosed(1, 150).mapToObj(i -> "S-" + i).toList();
        ObjectMapper mapper = new ObjectMapper();
        String firstBatch = mapper.writeValueAsString(names.subList(0, 100));
        String secondBatch = mapper.writeValueAsString(names.subList(100, 150));

        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://erp.test/api/resource/Salary%20Detail?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("fields=[\"parent\",\"amount\"]")
                        .contains("filters=[[\"parent\",\"in\"," + firstBatch + "],[\"salary_component\",\"=\",\"Basic\"]]")
                        .contains("order_by=parent asc")
                        .contains("parent=Salary Slip"))
                .andRespond(withSuccess("{\"data\":[{\"parent\":\"S-1\",\"amount\":10}]}", JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://erp.test/api/resource/Salary%20Detail?")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("filters=[[\"parent\",\"in\"," + secondBatch + "]"))
                .andRespond(withSuccess("{\"data\":[]}", JSON));

        List<JsonNode> rows = client.listChildRows("Salary Detail", "Salary Slip", List.of("parent", "amount"),
                names, Filters.where("salary_component", "=", "Basic"), "parent asc");

        assertThat(rows).hasSize(1);
        server.verify();
    }

    @Test
    void getDocEncodeLesEspacesMaisConserveLesSlashDuNom() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Slip/Sal%20Slip/HR-EMP-00001/00001"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"Sal Slip/HR-EMP-00001/00001\",\"gross_pay\":1234.5}}", JSON));

        JsonNode doc = client.getDoc("Salary Slip", "Sal Slip/HR-EMP-00001/00001");

        assertThat(doc.path("gross_pay").decimalValue()).isEqualByComparingTo("1234.5");
    }

    @Test
    void insertEnvoieDuJsonEtRenvoieLeDocumentCree() {
        server.expect(requestTo("http://erp.test/api/resource/Employee"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith("application/json")))
                .andExpect(content().json("{\"first_name\":\"Ana\"}"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"HR-EMP-00042\"}}", JSON));

        JsonNode created = client.insert("Employee", Map.of("first_name", "Ana"));

        assertThat(created.path("name").asText()).isEqualTo("HR-EMP-00042");
    }

    @Test
    void runDocMethodPosteSurRunMethod() {
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment/HR-SSA-2025-00001?run_method=submit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"message\":\"ok\"}", JSON));

        assertThat(client.runDocMethod("Salary Structure Assignment", "HR-SSA-2025-00001", "submit")
                .path("message").asText()).isEqualTo("ok");
    }

    @Test
    void erreur404DevientNotFound() {
        server.expect(ExpectedCount.twice(), requestTo("http://erp.test/api/resource/Employee/X"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"exception\":\"frappe.exceptions.DoesNotExistError: Employee X not found\"}").contentType(JSON));

        assertThatThrownBy(() -> client.getDoc("Employee", "X"))
                .isInstanceOf(ErpNextNotFoundException.class)
                .hasMessageContaining("Employee X not found");
        assertThat(client.findDoc("Employee", "X")).isEmpty();
    }

    @Test
    void erreur403DevientForbidden() {
        server.expect(requestTo("http://erp.test/api/resource/Employee/X"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"message\":\"Not permitted\"}").contentType(JSON));

        assertThatThrownBy(() -> client.getDoc("Employee", "X"))
                .isInstanceOf(ErpNextForbiddenException.class)
                .hasMessageContaining("Not permitted");
    }

    @Test
    void erreur417AvecServerMessagesDevientValidationAvecLeMessageFrappe() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String serverMessages = mapper.writeValueAsString(List.of(
                "{\"message\": \"<b>Base</b> est obligatoire\", \"title\": \"Message\"}"));
        String body = mapper.writeValueAsString(Map.of(
                "exception", "frappe.exceptions.MandatoryError: [Salary Structure Assignment]: base",
                "_server_messages", serverMessages));
        server.expect(requestTo("http://erp.test/api/resource/Salary%20Structure%20Assignment"))
                .andRespond(withStatus(HttpStatus.EXPECTATION_FAILED).body(body).contentType(JSON));

        assertThatThrownBy(() -> client.insert("Salary Structure Assignment", Map.of()))
                .isInstanceOf(ErpNextValidationException.class)
                .satisfies(e -> assertThat(((ErpNextException) e).getErpNextMessage()).isEqualTo("Base est obligatoire"));
    }

    @Test
    void erreurReseauDevientUnavailable() {
        server.expect(requestTo("http://erp.test/api/resource/Employee/X"))
                .andRespond(request -> { throw new IOException("Connection refused"); });

        assertThatThrownBy(() -> client.getDoc("Employee", "X"))
                .isInstanceOf(ErpNextUnavailableException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    void erreur502HtmlDevientUnavailableSansPlanter() {
        server.expect(requestTo("http://erp.test/api/resource/Employee/X"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html><body><h1>502 Bad Gateway</h1></body></html>").contentType(MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.getDoc("Employee", "X"))
                .isInstanceOf(ErpNextUnavailableException.class)
                .hasMessageContaining("502 Bad Gateway");
    }
}
