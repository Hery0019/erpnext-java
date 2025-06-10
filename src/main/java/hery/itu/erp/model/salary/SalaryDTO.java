package hery.itu.erp.model.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour représenter une fiche de paie (Salary Slip) ERPNext.
 */
public class SalaryDTO {
    private String slipName;         // ex: "Sal Slip/HR-EMP-00001/00001"
    private String employeeId;       // ex: "HR-EMP-00001"
    private String employeeName;     // ex: "Iray Roa Bist"
    private String company;
    private LocalDate postingDate;
    private String status;
    private String currency;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private BigDecimal totalDeduction;
    private BigDecimal totalEarnings;
    private BigDecimal monthToDate;
    private BigDecimal yearToDate;
    private String totalInWords;
    private String month;
            

    
    private List<SalaryDetail> earnings  = new ArrayList<>();
    private List<SalaryDetail> deductions = new ArrayList<>();

    // ────────── GETTERS / SETTERS ──────────

    public String getSlipName() { return slipName; }
    public void setSlipName(String slipName) { this.slipName = slipName; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getMonthToDate() { return monthToDate; }
    public void setMonthToDate(BigDecimal monthToDate) { this.monthToDate = monthToDate; }

    public BigDecimal getYearToDate() { return yearToDate; }
    public void setYearToDate(BigDecimal yearToDate) { this.yearToDate = yearToDate; }

    public String getTotalInWords() { return totalInWords; }
    public void setTotalInWords(String totalInWords) { this.totalInWords = totalInWords; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public List<SalaryDetail> getEarnings() { return earnings; }
    public List<SalaryDetail> getDeductions() { return deductions; }
}
