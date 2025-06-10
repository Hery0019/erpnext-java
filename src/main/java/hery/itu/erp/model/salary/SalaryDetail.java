package hery.itu.erp.model.salary;

import java.util.List;

import java.math.BigDecimal;

public class SalaryDetail {
    public enum Type { EARNING, DEDUCTION }
    private String name;
    private String salaryComponent;
    private String abbr;
    private BigDecimal amount;
    private BigDecimal yearToDate;
    private BigDecimal additionalAmount;
    private BigDecimal defaultAmount;
    private BigDecimal taxOnFlexibleBenefit;
    private BigDecimal taxOnAdditionalSalary;
    private Type type;              

    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSalaryComponent() { return salaryComponent; }
    public void setSalaryComponent(String salaryComponent) { this.salaryComponent = salaryComponent; }

    public String getAbbr() { return abbr; }
    public void setAbbr(String abbr) { this.abbr = abbr; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getYearToDate() { return yearToDate; }
    public void setYearToDate(BigDecimal yearToDate) { this.yearToDate = yearToDate; }

    public BigDecimal getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(BigDecimal additionalAmount) { this.additionalAmount = additionalAmount; }

    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }

    public BigDecimal getTaxOnFlexibleBenefit() { return taxOnFlexibleBenefit; }
    public void setTaxOnFlexibleBenefit(BigDecimal taxOnFlexibleBenefit) { this.taxOnFlexibleBenefit = taxOnFlexibleBenefit; }

    public BigDecimal getTaxOnAdditionalSalary() { return taxOnAdditionalSalary; }
    public void setTaxOnAdditionalSalary(BigDecimal taxOnAdditionalSalary) { this.taxOnAdditionalSalary = taxOnAdditionalSalary; }
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

}
