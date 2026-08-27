package hery.itu.erp.model.salary;

/**
 * Une ligne de composant de salaire trouvée sur un Salary Slip, avec la période du slip.
 */
public class SalaryFilterDTO {

    private String slipName;           // ex: "Sal Slip/EMP001/00001"
    private String employee;
    private String salaryComponent;    // ex: "Basic"
    private Double amount;             // montant trouvé
    private String postingDate;
    private String startDate;
    private String endDate;
    private int docstatus;             // 0 brouillon, 1 soumis

    public SalaryFilterDTO() {
    }

    public SalaryFilterDTO(String slipName, String employee, String salaryComponent, Double amount, String postingDate) {
        this(slipName, employee, salaryComponent, amount, postingDate, null, null, 1);
    }

    public SalaryFilterDTO(String slipName, String employee, String salaryComponent, Double amount, String postingDate,
                           String startDate, String endDate, int docstatus) {
        this.slipName = slipName;
        this.employee = employee;
        this.salaryComponent = salaryComponent;
        this.amount = amount;
        this.postingDate = postingDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.docstatus = docstatus;
    }

    public String getSlipName() { return slipName; }
    public void setSlipName(String slipName) { this.slipName = slipName; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public String getSalaryComponent() { return salaryComponent; }
    public void setSalaryComponent(String salaryComponent) { this.salaryComponent = salaryComponent; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getDocstatus() { return docstatus; }
    public void setDocstatus(int docstatus) { this.docstatus = docstatus; }

    @Override
    public String toString() {
        return "SalaryFilterDTO{slipName='" + slipName + "', salaryComponent='" + salaryComponent
                + "', amount=" + amount + ", period=" + startDate + ".." + endDate + '}';
    }
}
