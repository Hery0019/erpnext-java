package hery.itu.erp.service.facture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hery.itu.erp.model.DetailsFacture;
import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.model.Item;
import hery.itu.erp.service.login.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class FactureAchatServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LoginService loginService;

    @InjectMocks
    private FactureAchatService factureAchatService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetDetailsFacture_success() throws Exception {
        // Arrange
        String factureNom = "FAC-0001";
        String sessionCookie = "sessionid=abc123";
        when(loginService.getSessionCookie()).thenReturn(sessionCookie);

        Map<String, Object> item = new HashMap<>();
        item.put("item_code", "ITEM001");
        item.put("item_name", "Produit Test");
        item.put("qty", 2);
        item.put("rate", 100.0);
        item.put("amount", 200.0);

        List<Map<String, Object>> items = Collections.singletonList(item);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "FAC-0001");
        data.put("supplier_name", "Fournisseur X");
        data.put("posting_date", "2024-05-01");
        data.put("status", "Paid");
        data.put("outstanding_amount", 0.0);
        data.put("items", items);
        data.put("grand_total", 200.0);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("data", data);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice/" + factureNom;
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Act
        DetailsFacture details = factureAchatService.getDetailsFacture(factureNom);

        // Assert
        assertNotNull(details);
        assertEquals("FAC-0001", details.getFacture().getName());
        assertEquals("Fournisseur X", details.getFacture().getSupplierName());
        assertEquals("Paid", details.getFacture().getStatus());
        assertEquals(200.0, Double.parseDouble(details.getGrandTotal()));
        assertFalse(details.getItems().isEmpty());
        assertEquals("ITEM001", details.getItems().get(0).getItem_code());
    }
}
