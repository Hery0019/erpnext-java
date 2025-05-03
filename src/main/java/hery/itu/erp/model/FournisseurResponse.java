package hery.itu.erp.model;

import hery.itu.erp.model.Fournisseur;
import java.util.List;

public class FournisseurResponse {
    private List<Fournisseur> data;

    public FournisseurResponse(List<Fournisseur> data) {
        this.data = data;
    }

    public List<Fournisseur> getData() {
        return data;
    }

    public void setData(List<Fournisseur> data) {
        this.data = data;
    }
}
