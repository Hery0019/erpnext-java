package hery.itu.erp.model;

import java.util.Date;
import java.util.List;

public class DetailsFacture {
    private FactureAchat facture;
    private List<Item> items;
    private String grandTotal;

    public DetailsFacture(FactureAchat facture, List<Item> items, String grandTotal) {
        this.facture = facture;
        this.items = items;
        this.grandTotal = grandTotal;
    }

    public FactureAchat getFacture() {
        return facture;
    }

    public void setFacture(FactureAchat facture) {
        this.facture = facture;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(String grandTotal) {
        this.grandTotal = grandTotal;
    }
}
