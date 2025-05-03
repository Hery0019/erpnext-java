package hery.itu.erp.service.fourniseur;

import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.FournisseurResponse;
import hery.itu.erp.service.login.LoginService;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
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
    
            // Appel pour obtenir les détails avec les items
            String detailsUrl = "http://erpnext.localhost:8001/api/resource/Supplier Quotation/" + devisName;
            ResponseEntity<Map> detailsResponse = restTemplate.exchange(detailsUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> fullDevisData = (Map<String, Object>) detailsResponse.getBody().get("data");
    
            Devis devis = new Devis();
            devis.setNumero(devisName);
            devis.setDate((String) devisData.get("title"));
            devis.setStatus((String) devisData.get("status"));
            devis.setCurrency((String) devisData.get("currency"));
    
            List<Map<String, Object>> items = (List<Map<String, Object>>) fullDevisData.get("items");
            List<ItemDevis> itemList = new ArrayList<>();
            double total = 0.0;
    
            if (items != null) {
                for (Map<String, Object> item : items) {
                    ItemDevis itemDevis = new ItemDevis();
                    itemDevis.setCode((String) item.get("item_code"));
                    itemDevis.setDescription((String) item.get("description"));
                    itemDevis.setQuantite(item.get("qty") != null ? ((Number) item.get("qty")).doubleValue() : 0.0);
                    itemDevis.setUnite((String) item.get("uom"));
                    itemDevis.setPrixUnitaire(item.get("rate") != null ? ((Number) item.get("rate")).doubleValue() : 0.0);
                    itemDevis.setMontant(item.get("amount") != null ? ((Number) item.get("amount")).doubleValue() : 0.0);
                    itemDevis.setEntrepot((String) item.get("warehouse"));
    
                    total += itemDevis.getMontant();
                    itemList.add(itemDevis);
                }
            }
    
            devis.setMontant(total);
            devis.setItems(itemList);
            devisList.add(devis);
        }
    
        return devisList;
    }

    public void modifierPrixItem(String devisId, String itemCode, double newRate) {
        try {
            String url = "http://erpnext.localhost:8001/api/resource/Supplier Quotation/" + devisId;
            
            // Récupérer d'abord le devis actuel
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", loginService.getSessionCookie());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> getEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, getEntity, Map.class);
            
            if (response.getBody() != null && response.getBody().get("data") != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    Object itemsObj = data.get("items");
                    if (!(itemsObj instanceof List)) {
                        throw new RuntimeException("'items' n'est pas une liste mais : " + (itemsObj == null ? "null" : itemsObj.getClass()));
                    }
                    List<?> itemsRaw = (List<?>) itemsObj;
                    List<Map<String, Object>> items = new java.util.ArrayList<>();
                    for (Object itemObj : itemsRaw) {
                        if (!(itemObj instanceof Map)) {
                            throw new RuntimeException("Un item n'est pas un objet JSON mais : " + itemObj.getClass());
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> item = (Map<String, Object>) itemObj;
                        items.add(item);
                    }
                    for (Map<String, Object> item : items) {
                        if (item.get("item_code").equals(itemCode)) {
                            item.put("rate", newRate);
                            double qty = ((Number) item.get("qty")).doubleValue();
                            item.put("amount", qty * newRate);
                            break;
                        }
                    }
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("items", items);
                    HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(updateData, headers);
                    restTemplate.exchange(url, HttpMethod.PUT, putEntity, Map.class);
                } else if (dataObj instanceof List) {
                    // Si 'data' est une liste de devis (rare mais possible)
                    List<?> dataList = (List<?>) dataObj;
                    boolean updated = false;
                    for (Object elem : dataList) {
                        if (elem instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) elem;
                            Object itemsObj = data.get("items");
                            if (!(itemsObj instanceof List)) continue;
                            List<?> itemsRaw = (List<?>) itemsObj;
                            List<Map<String, Object>> items = new java.util.ArrayList<>();
                            for (Object itemObj : itemsRaw) {
                                if (!(itemObj instanceof Map)) continue;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> item = (Map<String, Object>) itemObj;
                                items.add(item);
                            }
                            for (Map<String, Object> item : items) {
                                if (item.get("item_code").equals(itemCode)) {
                                    item.put("rate", newRate);
                                    double qty = ((Number) item.get("qty")).doubleValue();
                                    item.put("amount", qty * newRate);
                                    updated = true;
                                    break;
                                }
                            }
                            if (updated) {
                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("items", items);
                                HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(updateData, headers);
                                restTemplate.exchange(url, HttpMethod.PUT, putEntity, Map.class);
                                break;
                            }
                        }
                    }
                    if (!updated) {
                        throw new RuntimeException("Aucun devis ou item correspondant trouvé dans la liste retournée par l'API.");
                    }
                } else {
                    throw new RuntimeException("Format inattendu pour 'data': " + dataObj.getClass());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la modification du prix: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la modification du prix", e);
        }
    }
}
