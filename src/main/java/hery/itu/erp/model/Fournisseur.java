package hery.itu.erp.model;

public class Fournisseur {
    private String name;
    private String supplierName;
    private String email;
    private String phone;

    public Fournisseur() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
