package hery.itu.erp.controller.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.service.rh.EmployeeService;

import java.util.List;

@Controller
public class TestController {
    @Autowired
    EmployeeService employeeService;

    @GetMapping("/getEmp")
    public String getEmployeeByCompany() {
        System.out.println("-------------emp------------------");
        List<Employee> employees = employeeService.getImportantEmployees();
        for (Employee employee : employees) {
            System.out.println(employee.getName());
        }
        return "login";
      }
}
