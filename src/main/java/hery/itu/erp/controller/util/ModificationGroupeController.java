package hery.itu.erp.controller.util;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hery.itu.erp.service.rh.EmployeeService;
import hery.itu.erp.service.salary.BulkSalaryAdjustmentService;
import hery.itu.erp.service.salary.BulkSalaryAdjustmentService.AdjustmentResult;
import hery.itu.erp.service.salary.SalaryComponentService;

@Controller
public class ModificationGroupeController {
    private final SalaryComponentService salaryComponentService;
    private final EmployeeService employeeService;
    private final BulkSalaryAdjustmentService bulkSalaryAdjustmentService;

    public ModificationGroupeController(SalaryComponentService salaryComponentService,
                                        EmployeeService employeeService,
                                        BulkSalaryAdjustmentService bulkSalaryAdjustmentService) {
        this.salaryComponentService = salaryComponentService;
        this.employeeService = employeeService;
        this.bulkSalaryAdjustmentService = bulkSalaryAdjustmentService;
    }

    @GetMapping("/modification-groupe/show")
    public String getModificationGroupe(Model model) {
        model.addAttribute("employees", employeeService.getImportantEmployees());
        model.addAttribute("salaryComponents", salaryComponentService.getAllSalaryComponentNames());
        return "modification_groupe";
    }

    @PostMapping("/modification-groupe/execute")
    public String executeModification(
            @RequestParam("employees") List<String> employees,
            @RequestParam("salary_component") String salaryComponent,
            @RequestParam("condition") String condition,
            @RequestParam("then") String then,
            @RequestParam("montant") double montant,
            @RequestParam("pourcentage") double pourcentage,
            RedirectAttributes redirectAttributes) {

        AdjustmentResult result = bulkSalaryAdjustmentService.apply(
                employees, salaryComponent, condition, then, montant, pourcentage);

        redirectAttributes.addFlashAttribute("success", String.format(
                "Modification appliquée : %d assignation(s) ajustée(s) pour %d employé(s) sélectionné(s) "
                + "— composant '%s', condition '%s' %.2f, action '%s' %.2f%%.",
                result.adjusted().size(), employees.size(), salaryComponent, condition, montant, then, pourcentage));
        if (result.hasFailures()) {
            redirectAttributes.addFlashAttribute("warning",
                    "Échecs (" + result.failed().size() + ") : " + String.join(" ; ", result.failed()));
        }
        return "redirect:/modification-groupe/show";
    }
}
