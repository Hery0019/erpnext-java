package hery.itu.erp.model.salary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryStructAss {

    private String name;
    private String owner;
    private String creation;
    private String modified;
    private String modified_by;
    private int docstatus;
    private int idx;
    private String employee;
    private String employee_name;
    private String salary_structure;
    private String from_date;
    private String to_date;
    private String posting_date;
    private String company;
    private String payroll_payable_account;
    private String currency;
    private BigDecimal base;
    private BigDecimal variable;
    private BigDecimal taxable_earnings_till_date;
    private BigDecimal tax_deducted_till_date;
    private String salary_component;
    private String salary_component_amount;

    // ✅ Constructeur vide obligatoire
    public SalaryStructAss() {
    }

    // ✅ Getters et setters pour tous les champs

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getModified_by() {
        return modified_by;
    }

    public void setModified_by(String modified_by) {
        this.modified_by = modified_by;
    }

    public int getDocstatus() {
        return docstatus;
    }

    public void setDocstatus(int docstatus) {
        this.docstatus = docstatus;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getEmployee_name() {
        return employee_name;
    }

    public void setEmployee_name(String employee_name) {
        this.employee_name = employee_name;
    }

    public String getSalary_structure() {
        return salary_structure;
    }

    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    public String getFrom_date() {
        return from_date;
    }

    public void setFrom_date(String from_date) {
        this.from_date = from_date;
    }

    public String getTo_date() {
        return to_date;
    }

    public void setTo_date(String to_date) {
        this.to_date = to_date;
    }

    public String getPosting_date() {
        return posting_date;
    }

    public void setPosting_date(String posting_date) {
        this.posting_date = posting_date;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPayroll_payable_account() {
        return payroll_payable_account;
    }

    public void setPayroll_payable_account(String payroll_payable_account) {
        this.payroll_payable_account = payroll_payable_account;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBase() {
        return base;
    }

    public void setBase(BigDecimal base) {
        this.base = base;
    }

    public BigDecimal getVariable() {
        return variable;
    }

    public void setVariable(BigDecimal variable) {
        this.variable = variable;
    }

    public BigDecimal getTaxable_earnings_till_date() {
        return taxable_earnings_till_date;
    }

    public void setTaxable_earnings_till_date(BigDecimal taxable_earnings_till_date) {
        this.taxable_earnings_till_date = taxable_earnings_till_date;
    }

    public BigDecimal getTax_deducted_till_date() {
        return tax_deducted_till_date;
    }

    public void setTax_deducted_till_date(BigDecimal tax_deducted_till_date) {
        this.tax_deducted_till_date = tax_deducted_till_date;
    }

    public String getSalary_component() {
        return salary_component;
    }

    public void setSalary_component(String salary_component) {
        this.salary_component = salary_component;
    }
    
    public String getSalary_component_amount() {
        return salary_component_amount;
    }

    public void setSalary_component_amount(String salary_component_amount) {
        this.salary_component_amount = salary_component_amount;
    }
}
