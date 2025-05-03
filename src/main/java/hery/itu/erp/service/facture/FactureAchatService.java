
package hery.itu.erp.service.facture;

import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.service.login.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class FactureAchatService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginService loginService;

    public List<FactureAchat> getFacturesParFournisseur(String fournisseurNom) {
        String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice"
                + "?fields=[\"name\",\"supplier\",\"posting_date\",\"status\",\"grand_total\"]"
                + "&filters=[[\"supplier\",\"=\",\"" + fournisseurNom + "\"]]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        List<FactureAchat> factures = new ArrayList<>();

        for (Map<String, Object> item : data) {
            FactureAchat f = new FactureAchat();
            f.setName((String) item.get("name"));
            f.setSupplier((String) item.get("supplier"));
            f.setPostingDate((String) item.get("posting_date"));
            f.setStatus((String) item.get("status"));
            f.setGrandTotal(Double.parseDouble(item.get("grand_total").toString()));
            factures.add(f);
        }

        return factures;
    }

    public List<FactureAchat> getAllFactures() {
        String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice"
                + "?fields=[\"name\",\"supplier\",\"posting_date\",\"status\",\"grand_total\"]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        List<FactureAchat> factures = new ArrayList<>();

        for (Map<String, Object> item : data) {
            FactureAchat f = new FactureAchat();
            f.setName((String) item.get("name"));
            f.setSupplier((String) item.get("supplier"));
            f.setPostingDate((String) item.get("posting_date"));
            f.setStatus((String) item.get("status"));
            f.setGrandTotal(Double.parseDouble(item.get("grand_total").toString()));
            factures.add(f);
        }

        return factures;
    }

    public FactureAchat getFactureByName(String name) {
        String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice/" + name + "?fields=[\"name\",\"supplier\",\"posting_date\",\"status\",\"grand_total\"]";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        FactureAchat f = new FactureAchat();
        f.setName((String) data.get("name"));
        f.setSupplier((String) data.get("supplier"));
        f.setPostingDate((String) data.get("posting_date"));
        f.setStatus((String) data.get("status"));
        f.setGrandTotal(Double.parseDouble(data.get("grand_total").toString()));
        return f;
    }
}
