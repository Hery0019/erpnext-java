package hery.itu.erp.model;

public class PurchaseOrder {
    private String numero;
    private String titre;
    private String status;
    private String currency;
    private Double montant;

    // Getters et Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }
}
