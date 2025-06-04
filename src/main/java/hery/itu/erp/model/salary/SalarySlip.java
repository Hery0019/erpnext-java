package hery.itu.erp.model.salary;

public class SalarySlip {
    private String name;
    private String employee;
    private String employee_name;
    private String start_date;
    private String end_date;
    private double gross_pay;
    private double net_pay;
    private String posting_date;
    private String status;
    private String salary_structure;
    private String company;

    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }

    public String getSalary_structure() {
        return salary_structure;
    }
    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPosting_date() {
        return posting_date;
    }
    public void setPosting_date(String posting_date) {
        this.posting_date = posting_date;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
    public String getStart_date() {
        return start_date;
    }
    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }
    public String getEnd_date() {
        return end_date;
    }
    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }
    public double getGross_pay() {
        return gross_pay;
    }
    public void setGross_pay(double gross_pay) {
        this.gross_pay = gross_pay;
    }
    public double getNet_pay() {
        return net_pay;
    }
    public void setNet_pay(double net_pay) {
        this.net_pay = net_pay;
    }


}
