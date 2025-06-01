package hery.itu.erp.service.fourniseur;

import hery.itu.erp.model.PurchaseOrder;
import hery.itu.erp.service.login.LoginService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PurchaseOrderService {

    @Autowired
    private LoginService loginService;


    private RestTemplate restTemplate =  new RestTemplate();;

    public List<PurchaseOrder> getCommandesParFournisseur(String fournisseurNom, String statut) {
        String baseUrl = "http://erpnext.localhost:8000/api/resource/Purchase Order";
        String fields = "[\"name\",\"title\",\"status\",\"currency\",\"grand_total\"]";
        String filters = "[[\"supplier\",\"=\",\"" + fournisseurNom + "\"]";

        if (statut != null && !statut.isEmpty()) {
            if (statut.equals("recu")) {
                filters += ",[\"status\",\"in\",[\"To Receive and Bill\",\"To Receive\",\"Completed\",\"Delivered\",\"Draft\"]]";
            } else if (statut.equals("paye")) {
                filters += ",[\"status\",\"in\",[\"To Bill\",\"Completed\",\"Closed\"]]";
            }
        }
        filters += "]";

        String url = baseUrl + "?fields=" + fields + "&filters=" + filters;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");

        List<PurchaseOrder> commandes = new ArrayList<>();
        for (Map<String, Object> poData : data) {
            PurchaseOrder po = new PurchaseOrder();
            po.setNumero((String) poData.get("name"));
            po.setTitre((String) poData.get("title"));
            po.setStatus((String) poData.get("status"));
            po.setCurrency((String) poData.get("currency"));
            po.setMontant((Double) poData.get("grand_total"));
            commandes.add(po);
        }

        return commandes;
    }
}
