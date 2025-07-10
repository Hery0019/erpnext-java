package hery.itu.erp.controller.rh;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.ui.Model;
import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.service.rh.EmployeeService;

@Controller
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employes")
    public String afficherEmployes(Model model) {
        List<Employee> employes = employeeService.getImportantEmployees();
        model.addAttribute("employees", employes);
        return "employes";
    }

    @GetMapping("/employes/filtre")
    public String afficherEmployes(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) String company,
            Model model
    ) {
        boolean noFilter = (firstName == null || firstName.isEmpty()) &&
                        (lastName == null || lastName.isEmpty()) &&
                        (gender == null || gender.isEmpty()) &&
                        (status == null || status.isEmpty()) &&
                        (dateStart == null || dateStart.isEmpty()) &&
                        (dateEnd == null || dateEnd.isEmpty()) &&
                        (company == null || company.isEmpty());

        List<Employee> employees;
        if (noFilter) {
            employees = employeeService.getImportantEmployees();
        } else {
            employees = employeeService.filterEmployees(
                firstName, lastName, gender, status, dateStart, dateEnd, company
            );
        }

        model.addAttribute("employees", employees);
        return "employes";
    }

    @GetMapping("/employes/new")
    public String showCreateForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee-form";  // à créer
    }

    @PostMapping("/employes/create")
    public String createEmployee(@ModelAttribute("employee") Employee employee, Model model) {
        boolean success = employeeService.createEmployee(employee);

        if (success) {
            return "redirect:/employes";
        } else {
            model.addAttribute("error", "Failed to create employee");
            return "employee-form";
        }
    }

    @GetMapping("/employes/delete/{id}")
    public String deleteEmploye(@PathVariable("id") String id) {
        employeeService.deleteEmploye(id);
        return "redirect:/employes";
    }

    


}
