package hery.itu.erp.model;

public class ItemDevis {
    private String code;
    private String description;
    private double quantite;
    private String unite;
    private String numeroDevis;
    private double prixUnitaire;
    private double montant;
    private String entrepot; // warehouse

    // Getters et Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getQuantite() { return quantite; }
    public void setQuantite(double quantite) { this.quantite = quantite; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getEntrepot() { return entrepot; }
    public void setEntrepot(String entrepot) { this.entrepot = entrepot; }
}


