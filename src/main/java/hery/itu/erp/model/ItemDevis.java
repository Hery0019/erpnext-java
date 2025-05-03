package hery.itu.erp.model;

public class ItemDevis {
    private String code;
    private String description;
    private Double quantite;
    private String unite;
    private Double prixUnitaire;
    private Double montant;
    private String entrepot;

    public ItemDevis() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getQuantite() { return quantite; }
    public void setQuantite(Double quantite) { this.quantite = quantite; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getEntrepot() { return entrepot; }
    public void setEntrepot(String entrepot) { this.entrepot = entrepot; }
}
