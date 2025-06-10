package hery.itu.erp.model.salary;

import java.util.LinkedHashMap;
import java.util.Map;

public class TotauxMensuels {
    private String moisNum;
    private String anneeNum;
    private Double totalBrut = 0.0;
    private Double totalDeduction = 0.0;
    private Double totalNet = 0.0;
    private Map<String, String> listComponents = new LinkedHashMap<>();

    public TotauxMensuels(){}

    public TotauxMensuels(String moisNum, String anneeNum, Double brut, Double deduction, Double net) {
        this.moisNum = moisNum;
        this.anneeNum = anneeNum;
        this.totalBrut = brut;
        this.totalDeduction = deduction;
        this.totalNet = net;
    }

    public void ajouter(Double brut, Double deduction, Double net) {
        this.totalBrut += (brut != null) ? brut : 0.0;
        this.totalDeduction += (deduction != null) ? deduction : 0.0;
        this.totalNet += (net != null) ? net : 0.0;
    }
    
    public void ajouterComponents(String nomComponents, String valeur) {
        listComponents.put(nomComponents, valeur);
    }
    
    public String getComponents(String nomComponents) {
        return listComponents.get(nomComponents);
    }
    
    public Map<String, String> getVariablesSupplementaires() {
        return new LinkedHashMap<>(listComponents);
    }

    public String getMoisNum() {
        return moisNum;
    }

    public void setMoisNum(String moisNum) {
        this.moisNum = moisNum;
    }

    public String getAnneeNum() {
        return anneeNum;
    }

    public void setAnneeNum(String anneeNum) {
        this.anneeNum = anneeNum;
    }

    public Double getBrut() {
        return totalBrut;
    }

    public void setBrut(Double brut) {
        this.totalBrut = brut;
    }

    public Double getDeduction() {
        return totalDeduction;
    }

    public void setDeduction(Double deduction) {
        this.totalDeduction = deduction;
    }

    public Double getNet() {
        return totalNet;
    }

    public void setNet(Double net) {
        this.totalNet = net;
    }
}