
package hery.itu.erp.service.facture;

import hery.itu.erp.model.DetailsFacture;
import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.model.Item;
import hery.itu.erp.service.login.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
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

    public DetailsFacture getDetailsFacture(String factureNom) {
        try {
            String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice/" + factureNom;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", loginService.getSessionCookie());
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            System.out.println("ETOOOOOOOOOO : ");
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.get("items") != null) {
                    List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");
                    
                    // Créer la facture
                    String name = (String) data.get("name");
                    String supplierName = (String) data.get("supplier_name");
                    String postingDateStr = (String) data.get("posting_date");
                    String status = (String) data.get("status");
                    String outstandingAmountStr = data.get("outstanding_amount").toString();
                    
                    // Conversion de la date
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date postingDate = sdf.parse(postingDateStr);
                    
                    // Conversion du montant
                    Double outstandingAmount = Double.parseDouble(outstandingAmountStr);
                    
                    // Création de l'objet FactureAchat avec le constructeur
                    FactureAchat factureAchat = new FactureAchat(name, supplierName, postingDateStr, status, outstandingAmount);
                    
                    // Créer la liste des items
                    List<Item> itemList = new ArrayList<>();
                    for (Map<String, Object> itemData : itemsData) {
                        Item item = new Item();
                        item.setItem_code((String) itemData.get("item_code"));
                        item.setItem_name((String) itemData.get("item_name"));
                        
                        // Conversion sûre des nombres
                        Object qtyObj = itemData.get("qty");
                        if (qtyObj != null) {
                            if (qtyObj instanceof Integer) {
                                item.setQty((Integer) qtyObj);
                            } else if (qtyObj instanceof Double) {
                                item.setQty(((Double) qtyObj).intValue());
                            }
                        }

                        Object rateObj = itemData.get("rate");
                        if (rateObj != null) {
                            if (rateObj instanceof Double) {
                                item.setRate((Double) rateObj);
                            } else if (rateObj instanceof Integer) {
                                item.setRate(((Integer) rateObj).doubleValue());
                            }
                        }

                        Object amountObj = itemData.get("amount");
                        if (amountObj != null) {
                            if (amountObj instanceof Double) {
                                item.setAmount((Double) amountObj);
                            } else if (amountObj instanceof Integer) {
                                item.setAmount(((Integer) amountObj).doubleValue());
                            }
                        }

                        itemList.add(item);
                    }
                    System.out.println("Data reçu de l'API : " + data);
                    
                    // Créer et retourner l'objet DetailsFacture
                    return new DetailsFacture(factureAchat, itemList, data.get("grand_total").toString());
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des détails de la facture: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public boolean payerFacture(String factureNom, Double amount) {
        try {
            // D'abord, obtenons les détails de la facture pour avoir la société
            String factureUrl = "http://erpnext.localhost:8001/api/resource/Purchase Invoice/" + factureNom;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", loginService.getSessionCookie());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> factureEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> factureResponse = restTemplate.exchange(factureUrl, HttpMethod.GET, factureEntity, Map.class);
            Map<String, Object> factureData = (Map<String, Object>) factureResponse.getBody().get("data");
            String company = (String) factureData.get("company");
            
            // Maintenant créons le Payment Entry
            String url = "http://erpnext.localhost:8001/api/resource/Payment Entry";
            
            // Create payment entry data
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("doctype", "Payment Entry");
            paymentData.put("naming_series", "PE-.YYYY.-");
            paymentData.put("payment_type", "Pay");
            paymentData.put("party_type", "Supplier");
            paymentData.put("party", getSupplierFromFacture(factureNom));
            paymentData.put("posting_date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            paymentData.put("company", company);
            paymentData.put("paid_amount", amount);
            paymentData.put("received_amount", amount);
            
            // Champs obligatoires pour la validation
            paymentData.put("source_exchange_rate", 1.0);
            paymentData.put("target_exchange_rate", 1.0);
            
            // Définir directement le compte de paiement
            paymentData.put("paid_from", "Capital Social - RS");
            paymentData.put("paid_from_account_currency", "EUR");
            
            List<Map<String, Object>> references = new ArrayList<>();
            Map<String, Object> reference = new HashMap<>();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", factureNom);
            reference.put("allocated_amount", amount);
            references.add(reference);
            
            paymentData.put("references", references);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentData, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
                String paymentEntryName = (String) responseData.get("name");
                String submitUrl = "http://erpnext.localhost:8001/api/resource/Payment Entry/" + paymentEntryName;
                Map<String, Object> submitData = new HashMap<>();
                submitData.put("docstatus", 1); // 1 pour soumis
                HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitData, headers);
                ResponseEntity<Map> submitResponse = restTemplate.exchange(submitUrl, HttpMethod.PUT, submitEntity, Map.class);
                
                return submitResponse.getStatusCode() == HttpStatus.OK;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors du paiement de la facture: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public Map<String, String> getComptesParEntreprise(String company) {
        try {
            String url = "http://erpnext.localhost:8001/api/resource/Company/" + company;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", loginService.getSessionCookie());
            HttpEntity<String> entity = new HttpEntity<>(headers);
    
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    
            // Supposons que la structure des comptes dans ERPNext soit dans les champs `paid_from_account` et `paid_to_account`
            String paidFromAccount = (String) data.get("paid_from_account");
            String paidToAccount = (String) data.get("paid_to_account");
    
            Map<String, String> comptes = new HashMap<>();
            comptes.put("paid_from", paidFromAccount);
            comptes.put("paid_to", paidToAccount);
    
            return comptes;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des comptes pour l'entreprise " + company + " : " + e.getMessage());
            return Collections.emptyMap();  // Retourne une carte vide si erreur
        }
    }
    
    
    public String getCompanyFromFacture(String factureNom) {
        try {
            String url = "http://erpnext.localhost:8001/api/resource/Purchase Invoice/" + factureNom;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", loginService.getSessionCookie());
            HttpEntity<String> entity = new HttpEntity<>(headers);
    
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return (String) data.get("company");
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'entreprise : " + e.getMessage());
            return null;
        }
    }
    

    private String getSupplierFromFacture(String factureNom) {
        DetailsFacture details = getDetailsFacture(factureNom);
        return details != null ? details.getFacture().getSupplierName() : null;
    } 
}
