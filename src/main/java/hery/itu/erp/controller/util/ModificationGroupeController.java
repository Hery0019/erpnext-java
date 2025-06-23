package hery.itu.erp.controller.util;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import hery.itu.erp.service.util.ModificationGroupeService;
import hery.itu.erp.service.salary.SalaryComponentService;
import hery.itu.erp.service.rh.EmployeeService;

@Controller
public class ModificationGroupeController {   
    private ModificationGroupeService modificationGroupeService;
    private SalaryComponentService salaryComponentService;
    private EmployeeService employeeService;

    public ModificationGroupeController(ModificationGroupeService modificationGroupeService, SalaryComponentService salaryComponentService, EmployeeService employeeService) {
        this.modificationGroupeService = modificationGroupeService;
        this.salaryComponentService = salaryComponentService;
        this.employeeService = employeeService;
    }

    @GetMapping("/modification-groupe/show")
    public String getModificationGroupe(Model model) throws Exception {
        model.addAttribute("employees", employeeService.getImportantEmployees());
        model.addAttribute("salaryComponents", salaryComponentService.getAllSalaryComponentNames());
        return "modification_groupe";
    }
}
