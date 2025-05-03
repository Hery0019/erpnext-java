package hery.itu.erp.model;


public class FactureAchat {
    private String name;
    private String supplier;
    private String postingDate;
    private String status;
    private double grandTotal;

    // Getters et Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
}
