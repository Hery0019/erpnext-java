package hery.itu.erp.model.salary;

public class SalaryFilterDTO {

    private String slipName;           // ex: "Sal Slip/EMP001/00001"
    private String employee;
    private String salaryComponent;    // ex: "Basic"
    private Double amount;             // Montant trouvé
    private String postingDate;        // Date du Salary Slip, optionnel

    public SalaryFilterDTO() {}

    public SalaryFilterDTO(String slipName, String employee, String salaryComponent, Double amount, String postingDate) {
        this.slipName = slipName;
        this.employee = employee;
        this.salaryComponent = salaryComponent;
        this.amount = amount;
        this.postingDate = postingDate;
    }

    public String getSlipName() {
        return slipName;
    }

    public void setSlipName(String slipName) {
        this.slipName = slipName;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getSalaryComponent() {
        return salaryComponent;
    }

    public void setSalaryComponent(String salaryComponent) {
        this.salaryComponent = salaryComponent;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(String postingDate) {
        this.postingDate = postingDate;
    }


    @Override
    public String toString() {
        return "SalaryFilterDTO{" +
                "slipName='" + slipName + '\'' +
                ", salaryComponent='" + salaryComponent + '\'' +
                ", amount=" + amount +
                ", postingDate='" + postingDate + '\'' +
                '}';
    }
}
