package hery.itu.erp.controller.util;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import hery.itu.erp.service.salary.SalaryComponentService;
import hery.itu.erp.service.rh.EmployeeService;
import hery.itu.erp.service.salary.SalaryStructAssService;

import java.util.List;

@Controller
public class ModificationGroupeController {   
    private SalaryComponentService salaryComponentService;
    private EmployeeService employeeService;
    private SalaryStructAssService salaryStructAssService;

    public ModificationGroupeController(SalaryComponentService salaryComponentService, EmployeeService employeeService, SalaryStructAssService salaryStructAssService) {
        this.salaryComponentService = salaryComponentService;
        this.employeeService = employeeService;
        this.salaryStructAssService = salaryStructAssService;
    }

    @GetMapping("/modification-groupe/show")
    public String getModificationGroupe(Model model) throws Exception {
        model.addAttribute("employees", employeeService.getImportantEmployees());
        model.addAttribute("salaryComponents", salaryComponentService.getAllSalaryComponentNames());
        return "modification_groupe";
    }

      /**
     * Traite le formulaire après soumission
     */
    @PostMapping("/modification-grouper/execute")
    public String executeModification(
            @RequestParam("employees") List<String> employees,
            @RequestParam("salary_component") String salaryComponent,
            @RequestParam("condition") String condition,
            @RequestParam("montant") double montant,
            @RequestParam("pourcentage") double pourcentage,
            Model model) throws Exception {

        // Appelle ton service métier pour appliquer la modification groupe
        salaryStructAssService.applyModification(
                employees,
                salaryComponent,
                condition,
                montant,
                pourcentage
        );

        // Message de succès ou redirection
        model.addAttribute("message", "Modification appliquée avec succès !");
        return "redirect:/modification-grouper/show";
    }
   
}
