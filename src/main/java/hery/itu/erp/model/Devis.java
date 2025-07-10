package hery.itu.erp.model;

import java.util.List;

public class Devis {
    private String numero;
    private String date;
    private String status;
    private String currency;
    private Double montant;
    private java.util.List<ItemDevis> items;

    public Devis() {}

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public java.util.List<ItemDevis> getItems() { return items; }
    public void setItems(java.util.List<ItemDevis> items) { this.items = items; }
}
