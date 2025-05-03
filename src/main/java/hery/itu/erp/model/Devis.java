package hery.itu.erp.model;

import java.util.List;

public class Devis {
    private String numero;
    private String date;
    private String status;
    private String currency;
    private Double montant;
    private List<ItemDevis> items;

    // Getters et Setters
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

    public List<ItemDevis> getItems() { return items; }
    public void setItems(List<ItemDevis> items) { this.items = items; }
}

