package hery.itu.erp.model.salary;

import java.math.BigDecimal;
import java.time.LocalDate;


public class SalaryStructAss {
    private String employee;
    private String salary_structure;
    private String company;
    private LocalDate from_date;
    private String currency;
    private BigDecimal base;
    
    public SalaryStructAss(String employee, String salary_structure, String company, LocalDate from_date,
            String currency, BigDecimal base) {
        this.employee = employee;
        this.salary_structure = salary_structure;
        this.company = company;
        this.from_date = from_date;
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

    public LocalDate getFrom_date() {
        return from_date;
    }

    public void setFrom_date(LocalDate from_date) {
        this.from_date = from_date;
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
