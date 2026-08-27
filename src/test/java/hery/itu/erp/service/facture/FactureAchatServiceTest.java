package hery.itu.erp.service.facture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.config.PaymentProperties;
import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.service.login.LoginService;

class FactureAchatServiceTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private MockRestServiceServer server;
    private FactureAchatService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LoginService loginService = mock(LoginService.class);
        when(loginService.getSessionCookie()).thenReturn("sid=abc");
        ErpNextClient client = new ErpNextClient(new RestTemplateBuilder(customizer),
                new ErpNextProperties("http://erp.test"), loginService, new ObjectMapper());
        service = new FactureAchatService(client, new PaymentProperties("PE-.YYYY.-", "Banque - TC", "MGA"));
        server = customizer.getServer();
    }

    @Test
    void lePaiementUtiliseLeCompteConfigureEtLIdentifiantDuFournisseur() {
        server.expect(requestTo("http://erp.test/api/resource/Purchase%20Invoice/ACC-PINV-2025-00001"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"ACC-PINV-2025-00001\",\"supplier\":\"SUP-0001\","
                        + "\"supplier_name\":\"Fournisseur X\",\"company\":\"Orinasa SA\",\"outstanding_amount\":1000}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Payment%20Entry"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"doctype\":\"Payment Entry\",\"naming_series\":\"PE-.YYYY.-\",\"payment_type\":\"Pay\","
                        + "\"party_type\":\"Supplier\",\"party\":\"SUP-0001\",\"company\":\"Orinasa SA\",\"paid_amount\":1000.0,"
                        + "\"paid_from\":\"Banque - TC\",\"paid_from_account_currency\":\"MGA\","
                        + "\"references\":[{\"reference_doctype\":\"Purchase Invoice\",\"reference_name\":\"ACC-PINV-2025-00001\",\"allocated_amount\":1000.0}]}",
                        JsonCompareMode.LENIENT))
                .andRespond(withSuccess("{\"data\":{\"name\":\"PE-2025-00007\"}}", JSON));
        server.expect(requestTo("http://erp.test/api/resource/Payment%20Entry/PE-2025-00007"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"docstatus\":1}"))
                .andRespond(withSuccess("{\"data\":{\"name\":\"PE-2025-00007\",\"docstatus\":1}}", JSON));

        service.payerFacture("ACC-PINV-2025-00001", 1000);

        server.verify();
    }
}
