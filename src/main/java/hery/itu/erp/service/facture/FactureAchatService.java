package hery.itu.erp.service.facture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.model.DetailsFacture;
import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.model.Item;

@Service
public class FactureAchatService {

    private static final Logger log = LoggerFactory.getLogger(FactureAchatService.class);
    private static final String PURCHASE_INVOICE = "Purchase Invoice";
    private static final String PAYMENT_ENTRY = "Payment Entry";

    // TODO (revue 6.3) : valeurs de test à externaliser dans la configuration
    private static final String PAYMENT_NAMING_SERIES = "PE-.YYYY.-";
    private static final String PAID_FROM_ACCOUNT = "Capital Social - RS";
    private static final String PAID_FROM_CURRENCY = "EUR";

    private final ErpNextClient client;

    public FactureAchatService(ErpNextClient client) {
        this.client = client;
    }

    public List<FactureAchat> getAllFactures() {
        return client.list(PURCHASE_INVOICE)
                .fields("name", "supplier", "supplier_name", "posting_date", "status", "grand_total", "outstanding_amount")
                .orderBy("posting_date desc")
                .fetchAll().stream()
                .map(FactureAchatService::toFacture)
                .toList();
    }

    public FactureAchat getFactureByName(String name) {
        return toFacture(client.getDoc(PURCHASE_INVOICE, name));
    }

    public DetailsFacture getDetailsFacture(String factureNom) {
        JsonNode data = client.getDoc(PURCHASE_INVOICE, factureNom);

        List<Item> items = new ArrayList<>();
        for (JsonNode itemData : data.path("items")) {
            Item item = new Item();
            item.setItem_code(itemData.path("item_code").asText(null));
            item.setItem_name(itemData.path("item_name").asText(null));
            item.setQty(itemData.path("qty").asInt(0));
            item.setRate(itemData.path("rate").asDouble(0.0));
            item.setAmount(itemData.path("amount").asDouble(0.0));
            items.add(item);
        }
        return new DetailsFacture(toFacture(data), items, data.path("grand_total").asText("0"));
    }

    /**
     * Crée puis soumet un Payment Entry référençant la facture.
     *
     * @throws hery.itu.erp.erpnext.ErpNextException si ERPNext refuse le paiement (montant, compte, droits…)
     */
    public void payerFacture(String factureNom, double amount) {
        JsonNode facture = client.getDoc(PURCHASE_INVOICE, factureNom);

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("doctype", PAYMENT_ENTRY);
        paymentData.put("naming_series", PAYMENT_NAMING_SERIES);
        paymentData.put("payment_type", "Pay");
        paymentData.put("party_type", "Supplier");
        paymentData.put("party", facture.path("supplier").asText());
        paymentData.put("posting_date", LocalDate.now().toString());
        paymentData.put("company", facture.path("company").asText());
        paymentData.put("paid_amount", amount);
        paymentData.put("received_amount", amount);
        paymentData.put("source_exchange_rate", 1.0);
        paymentData.put("target_exchange_rate", 1.0);
        paymentData.put("paid_from", PAID_FROM_ACCOUNT);
        paymentData.put("paid_from_account_currency", PAID_FROM_CURRENCY);
        paymentData.put("references", List.of(Map.of(
                "reference_doctype", PURCHASE_INVOICE,
                "reference_name", factureNom,
                "allocated_amount", amount)));

        String paymentEntryName = client.insert(PAYMENT_ENTRY, paymentData).path("name").asText();
        client.update(PAYMENT_ENTRY, paymentEntryName, Map.of("docstatus", 1));
        log.info("Paiement {} soumis pour la facture {}", paymentEntryName, factureNom);
    }

    private static FactureAchat toFacture(JsonNode node) {
        FactureAchat f = new FactureAchat();
        f.setName(node.path("name").asText(null));
        f.setSupplier(node.path("supplier").asText(null));
        f.setSupplierName(node.path("supplier_name").asText(null));
        f.setPostingDate(node.path("posting_date").asText(null));
        f.setStatus(node.path("status").asText(null));
        f.setGrandTotal(node.path("grand_total").asDouble(0.0));
        f.setOutstandingAmount(node.path("outstanding_amount").asDouble(0.0));
        return f;
    }
}
