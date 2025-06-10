package hery.itu.erp.model.salary;

import java.util.Map;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

public class SalaryGroupedDTO {
    private String month;
    private Map<String, BigDecimal> earningsTotal = new LinkedHashMap<>();
    private Map<String, BigDecimal> deductionsTotal = new LinkedHashMap<>();
    private BigDecimal totalGross = BigDecimal.ZERO;
    private BigDecimal totalDeduction = BigDecimal.ZERO;
    private BigDecimal totalNet = BigDecimal.ZERO;

    public SalaryGroupedDTO() {
    }

    public SalaryGroupedDTO(String month, Map<String, BigDecimal> earningsTotal, Map<String, BigDecimal> deductionsTotal, BigDecimal totalGross, BigDecimal totalDeduction, BigDecimal totalNet) {
        this.month = month;
        this.earningsTotal = earningsTotal;
        this.deductionsTotal = deductionsTotal;
        this.totalGross = totalGross;
        this.totalDeduction = totalDeduction;
        this.totalNet = totalNet;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Map<String, BigDecimal> getEarningsTotal() {
        return earningsTotal;
    }

    public void setEarningsTotal(Map<String, BigDecimal> earningsTotal) {
        this.earningsTotal = earningsTotal;
    }

    public Map<String, BigDecimal> getDeductionsTotal() {
        return deductionsTotal;
    }

    public void setDeductionsTotal(Map<String, BigDecimal> deductionsTotal) {
        this.deductionsTotal = deductionsTotal;
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = totalGross;
    }

    public BigDecimal getTotalDeduction() {
        return totalDeduction;
    }

    public void setTotalDeduction(BigDecimal totalDeduction) {
        this.totalDeduction = totalDeduction;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(BigDecimal totalNet) {
        this.totalNet = totalNet;
    }

   
    
}
