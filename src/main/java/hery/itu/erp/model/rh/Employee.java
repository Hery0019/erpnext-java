package hery.itu.erp.model.rh;

public class Employee {
    private String name;
    private String first_name;
    private String middle_name;
    private String date_of_birth;
    private String date_of_joining;
    private String status;
    private String gender;
    private String company;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getFirst_name() {
        return first_name;
    }
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }
    public String getMiddle_name() {
        return middle_name;
    }
    public void setMiddle_name(String middle_name) {
        this.middle_name = middle_name;
    }
    public String getDate_of_birth() {
        return date_of_birth;
    }
    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }
    public String getDate_of_joining() {
        return date_of_joining;
    }
    public void setDate_of_joining(String date_of_joining) {
        this.date_of_joining = date_of_joining;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }
}
