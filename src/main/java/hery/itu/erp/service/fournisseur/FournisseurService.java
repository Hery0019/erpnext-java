package hery.itu.erp.service.fournisseur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.Devis;
import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.ItemDevis;

@Service
public class FournisseurService {

    private static final Logger log = LoggerFactory.getLogger(FournisseurService.class);
    private static final String SUPPLIER = "Supplier";
    private static final String SUPPLIER_QUOTATION = "Supplier Quotation";
    private static final String SUPPLIER_QUOTATION_ITEM = "Supplier Quotation Item";
    private static final List<String> ITEM_FIELDS = List.of("parent", "item_code", "description", "qty", "uom", "rate", "amount", "warehouse");

    private final ErpNextClient client;

    public FournisseurService(ErpNextClient client) {
        this.client = client;
    }

    public List<Fournisseur> getFournisseurs() {
        return client.list(SUPPLIER)
                .fields("name", "supplier_name", "email_id", "mobile_no")
                .orderBy("supplier_name asc")
                .fetchAll().stream()
                .map(FournisseurService::toFournisseur)
                .toList();
    }

    public List<Devis> getDevisParFournisseur(String fournisseurNom) {
        List<JsonNode> quotations = client.list(SUPPLIER_QUOTATION)
                .fields("name", "title", "status", "currency", "transaction_date")
                .filters(Filters.where("supplier", "=", fournisseurNom))
                .orderBy("transaction_date desc")
                .fetchAll();

        Map<String, List<JsonNode>> itemsByQuotation = loadItems(quotations);

        List<Devis> devisList = new ArrayList<>();
        for (JsonNode quotation : quotations) {
            String devisName = quotation.path("name").asText();

            Devis devis = new Devis();
            devis.setNumero(devisName);
            devis.setDate(quotation.path("transaction_date").asText(quotation.path("title").asText(null)));
            devis.setStatus(quotation.path("status").asText(null));
            devis.setCurrency(quotation.path("currency").asText(null));

            List<ItemDevis> items = new ArrayList<>();
            double total = 0.0;
            for (JsonNode item : itemsByQuotation.getOrDefault(devisName, List.of())) {
                ItemDevis itemDevis = new ItemDevis();
                itemDevis.setCode(item.path("item_code").asText(null));
                itemDevis.setDevis(devis);
                itemDevis.setDescription(item.path("description").asText(null));
                itemDevis.setQuantite(item.path("qty").asDouble(0.0));
                itemDevis.setUnite(item.path("uom").asText(null));
                itemDevis.setPrixUnitaire(item.path("rate").asDouble(0.0));
                itemDevis.setMontant(item.path("amount").asDouble(0.0));
                itemDevis.setEntrepot(item.path("warehouse").asText(null));
                itemDevis.setDevisId(devisName);
                total += itemDevis.getMontant();
                items.add(itemDevis);
            }
            devis.setMontant(total);
            devis.setItems(items);
            devisList.add(devis);
        }
        return devisList;
    }

    /**
     * Modifie le prix unitaire d'un item du devis puis, si tous les prix sont renseignés,
     * soumet le devis (comportement historique, voir point 1.10 de la revue).
     *
     * @throws IllegalArgumentException si aucun item ne correspond (aucune écriture n'est faite)
     */
    public void modifierPrixItem(String devisId, String itemCode, double newRate, String entrepot) {
        JsonNode data = client.getDoc(SUPPLIER_QUOTATION, devisId);
        List<Map<String, Object>> items = client.convert(data.path("items"), new TypeReference<List<Map<String, Object>>>() { });

        boolean modifie = false;
        for (Map<String, Object> item : items) {
            if (itemCode.equals(item.get("item_code"))
                    && (entrepot == null || entrepot.equals(item.get("warehouse")))) {
                double qty = toDouble(item.get("qty"));
                item.put("rate", newRate);
                item.put("amount", qty * newRate);
                modifie = true;
                break;
            }
        }
        if (!modifie) {
            throw new IllegalArgumentException(
                    "Aucun item " + itemCode + " (entrepôt " + entrepot + ") dans le devis " + devisId);
        }

        client.update(SUPPLIER_QUOTATION, devisId, Map.of("items", items));

        boolean tousLesRatesValides = items.stream().allMatch(i -> toDouble(i.get("rate")) > 0);
        if (tousLesRatesValides) {
            client.update(SUPPLIER_QUOTATION, devisId, Map.of("docstatus", 1));
            log.info("Devis {} soumis après mise à jour du prix de {}", devisId, itemCode);
        } else {
            log.info("Devis {} non soumis : au moins un item a un prix unitaire nul", devisId);
        }
    }

    /**
     * Items de tous les devis en une requête par lot sur la table enfant (au lieu d'un GET par devis) ;
     * repli sur un GET par devis si l'instance refuse la lecture directe de la table enfant.
     */
    private Map<String, List<JsonNode>> loadItems(List<JsonNode> quotations) {
        List<String> names = quotations.stream().map(q -> q.path("name").asText()).toList();
        Map<String, List<JsonNode>> byQuotation = new LinkedHashMap<>();
        if (names.isEmpty()) {
            return byQuotation;
        }
        try {
            for (JsonNode row : client.listChildRows(SUPPLIER_QUOTATION_ITEM, SUPPLIER_QUOTATION, ITEM_FIELDS, names, null, "parent asc, idx asc")) {
                byQuotation.computeIfAbsent(row.path("parent").asText(), k -> new ArrayList<>()).add(row);
            }
        } catch (ErpNextValidationException | ErpNextForbiddenException e) {
            log.warn("Lecture groupée des items de devis refusée ({}) : repli sur un appel par devis", e.getErpNextMessage());
            for (String name : names) {
                List<JsonNode> items = new ArrayList<>();
                client.getDoc(SUPPLIER_QUOTATION, name).path("items").forEach(items::add);
                byQuotation.put(name, items);
            }
        }
        return byQuotation;
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static Fournisseur toFournisseur(JsonNode node) {
        Fournisseur f = new Fournisseur();
        f.setName(node.path("name").asText(null));
        f.setSupplierName(node.path("supplier_name").asText(null));
        f.setEmail(node.path("email_id").asText(null));
        f.setPhone(node.path("mobile_no").asText(null));
        return f;
    }
}
