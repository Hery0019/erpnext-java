package hery.itu.erp.service.fournisseur;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.PurchaseOrder;

@Service
public class PurchaseOrderService {

    private static final String PURCHASE_ORDER = "Purchase Order";
    private static final List<String> STATUTS_RECU = List.of("To Receive and Bill", "To Receive", "Completed", "Delivered", "Draft");
    private static final List<String> STATUTS_PAYE = List.of("To Bill", "Completed", "Closed");

    private final ErpNextClient client;

    public PurchaseOrderService(ErpNextClient client) {
        this.client = client;
    }

    /**
     * @param statut {@code recu}, {@code paye} ou vide/null pour tous
     */
    public List<PurchaseOrder> getCommandesParFournisseur(String fournisseurNom, String statut) {
        Filters filters = Filters.where("supplier", "=", fournisseurNom);
        if ("recu".equals(statut)) {
            filters.in("status", STATUTS_RECU);
        } else if ("paye".equals(statut)) {
            filters.in("status", STATUTS_PAYE);
        }

        return client.list(PURCHASE_ORDER)
                .fields("name", "title", "status", "currency", "grand_total")
                .filters(filters)
                .orderBy("transaction_date desc")
                .fetchAll().stream()
                .map(PurchaseOrderService::toPurchaseOrder)
                .toList();
    }

    private static PurchaseOrder toPurchaseOrder(JsonNode node) {
        PurchaseOrder po = new PurchaseOrder();
        po.setNumero(node.path("name").asText(null));
        po.setTitre(node.path("title").asText(null));
        po.setStatus(node.path("status").asText(null));
        po.setCurrency(node.path("currency").asText(null));
        po.setMontant(node.path("grand_total").asDouble(0.0));
        return po;
    }
}
