package hery.itu.erp.service.fourniseur;

import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.FournisseurResponse;
import hery.itu.erp.service.login.LoginService;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import hery.itu.erp.model.Devis;
import java.util.List;
import hery.itu.erp.model.ItemDevis;

@Service
public class FournisseurService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final LoginService loginService;

    public FournisseurService(LoginService loginService) {
        this.loginService = loginService;
    }

    public List<Fournisseur> getFournisseurs() {
        String url = "http://erpnext.localhost:8001/api/resource/Supplier";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie()); // Utilise le cookie stocké

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<FournisseurResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FournisseurResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur appel fournisseur : " + response.getStatusCode());
        }
    }

    public List<Devis> getDevisParFournisseur(String fournisseurNom) {
        String url = "http://erpnext.localhost:8001/api/resource/Supplier Quotation"
            + "?fields=[\"name\",\"title\",\"status\",\"currency\"]"
            + "&filters=[[\"supplier\",\"=\",\"" + fournisseurNom + "\"]]";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        
        List<Devis> devisList = new ArrayList<>();
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
    
        for (Map<String, Object> devisData : data) {
            String devisName = (String) devisData.get("name");
    
            // Récupérer les détails du devis, incluant les items
            String detailsUrl = "http://erpnext.localhost:8001/api/resource/Supplier Quotation/" + devisName;
            ResponseEntity<Map> detailsResponse = restTemplate.exchange(detailsUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> fullDevisData = (Map<String, Object>) detailsResponse.getBody().get("data");
    
            Devis devis = new Devis();
            devis.setNumero(devisName);
            devis.setDate((String) devisData.get("title"));
            devis.setStatus((String) devisData.get("status"));
            devis.setCurrency((String) devisData.get("currency"));
    
            // Extraire les items et leur prix
            List<Map<String, Object>> items = (List<Map<String, Object>>) fullDevisData.get("items");
            List<ItemDevis> itemList = new ArrayList<>();
            double total = 0.0;
    
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String itemCode = (String) item.get("item_code");
                    String description = (String) item.get("description");
                    Double rate = item.get("rate") != null ? ((Number) item.get("rate")).doubleValue() : 0.0;
    
                    ItemDevis itemDevis = new ItemDevis();
                    itemDevis.setCode(itemCode);
                    itemDevis.setDescription(description);
                    itemDevis.setPrix(rate);
                    itemList.add(itemDevis);
    
                    total += rate;
                }
            }
    
            devis.setMontant(total); // vous pouvez aussi définir .setItems(itemList)
            devisList.add(devis);
        }
    
        return devisList;
    }
    
}
