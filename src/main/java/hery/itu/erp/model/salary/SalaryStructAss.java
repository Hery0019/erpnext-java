package hery.itu.erp.model.salary;

import java.math.BigDecimal;
import java.time.LocalDate;


public class SalaryStructAss {
    private String employee;
    private String salary_structure;
    private String company;
    private String from_date;
    private String to_date;
    private String posting_date;
    private String currency;
    private BigDecimal base;
    
    public SalaryStructAss(String employee, String salary_structure, String company, String from_date,
            String to_date, String posting_date, String currency, BigDecimal base) {
        this.employee = employee;
        this.salary_structure = salary_structure;
        this.company = company;
        this.from_date = from_date;
        this.to_date = to_date;
        this.posting_date = posting_date;
        this.currency = currency;
        this.base = base;
    }

    public SalaryStructAss() {}

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getSalary_structure() {
        return salary_structure;
    }

    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

}
