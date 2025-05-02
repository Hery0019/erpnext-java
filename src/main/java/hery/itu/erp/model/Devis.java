package hery.itu.erp.model;

public class Devis {
    private String numero;
    private String date;
    private Double montant;
    private String status;
    private String currency;

    // Getters et Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
