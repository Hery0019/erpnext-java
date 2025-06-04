package hery.itu.erp.model.salary;

import java.util.List;

public class SalarySlipListResponse {
    private List<SalarySlip> data;

    public List<SalarySlip> getData() {
        return data;
    }

    public void setData(List<SalarySlip> data) {
        this.data = data;
    }
}