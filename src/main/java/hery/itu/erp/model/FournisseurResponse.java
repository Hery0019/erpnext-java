package hery.itu.erp.model;

import java.util.List;

public class FournisseurResponse {
    private List<Fournisseur> data;
    private String message;
    private int statusCode;

    public FournisseurResponse() {}

    public List<Fournisseur> getData() {
        return data;
    }

    public void setData(List<Fournisseur> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
