package hery.itu.erp.model.salary;


import java.util.Map;

public class SalaryReport {
    private String salarySlipId;
    private String employee;
    private String employeeName;
    private String dataOfJoining;
    private String branch;
    private String department;
    private String designation;
    private String company;
    private String startDate;
    private String endDate;
    private Double leaveWithoutPay;
    private Double absentDays;
    private Double paymentDays;
    private String currency;
    private Double totalLoanRepayment;
    private Double grossPay;
    private Double totalDeduction;
    private Double netPay;

    // Map pour stocker tous les composants dynamiques : indemnité, salaire_base, etc.
    private Map<String, Double> components;

    // Getters et Setters

    public String getSalarySlipId() {
        return salarySlipId;
    }

    public void setSalarySlipId(String salarySlipId) {
        this.salarySlipId = salarySlipId;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDataOfJoining() {
        return dataOfJoining;
    }

    public void setDataOfJoining(String dataOfJoining) {
        this.dataOfJoining = dataOfJoining;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Double getLeaveWithoutPay() {
        return leaveWithoutPay;
    }

    public void setLeaveWithoutPay(Double leaveWithoutPay) {
        this.leaveWithoutPay = leaveWithoutPay;
    }

    public Double getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(Double absentDays) {
        this.absentDays = absentDays;
    }

    public Double getPaymentDays() {
        return paymentDays;
    }

    public void setPaymentDays(Double paymentDays) {
        this.paymentDays = paymentDays;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getTotalLoanRepayment() {
        return totalLoanRepayment;
    }

    public void setTotalLoanRepayment(Double totalLoanRepayment) {
        this.totalLoanRepayment = totalLoanRepayment;
    }

    public Double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(Double grossPay) {
        this.grossPay = grossPay;
    }

    public Double getTotalDeduction() {
        return totalDeduction;
    }

    public void setTotalDeduction(Double totalDeduction) {
        this.totalDeduction = totalDeduction;
    }

    public Double getNetPay() {
        return netPay;
    }

    public void setNetPay(Double netPay) {
        this.netPay = netPay;
    }

    public Map<String, Double> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Double> components) {
        this.components = components;
    }
}
