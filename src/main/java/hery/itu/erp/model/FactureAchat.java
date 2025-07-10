package hery.itu.erp.model;


public class FactureAchat {
    private String name;
    private String supplier;
    private String supplierName;
    private String postingDate;
    private String status;
    private double grandTotal;
    private double outstandingAmount;

    public FactureAchat() {}
    public FactureAchat(String name, String supplierName, String postingDate, String status, double outstandingAmount) {
        this.name = name;
        this.supplierName = supplierName;
        this.postingDate = postingDate;
        this.status = status;
        this.outstandingAmount = outstandingAmount;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public double getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(double outstandingAmount) { this.outstandingAmount = outstandingAmount; }
}

