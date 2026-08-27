package hery.itu.erp.controller.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.rh.EmployeeService;
import hery.itu.erp.service.salary.PayrollGenerationService;
import hery.itu.erp.service.salary.PayrollGenerationService.GenerationResult;
import hery.itu.erp.service.salary.SalaryStructAssService;
import hery.itu.erp.service.salary.SalaryStructAssService.AssignmentAndSlips;

@Controller
public class SalaryStructAssController {
    private final SalaryStructAssService salaryStructAssService;
    private final PayrollGenerationService payrollGenerationService;
    private final EmployeeService employeeService;

    public SalaryStructAssController(SalaryStructAssService salaryStructAssService,
                                     PayrollGenerationService payrollGenerationService,
                                     EmployeeService employeeService) {
        this.salaryStructAssService = salaryStructAssService;
        this.payrollGenerationService = payrollGenerationService;
        this.employeeService = employeeService;
    }

    @GetMapping("/salary-struct-ass/list")
    public String listSalaryStructureAssignments(Model model) {
        List<SalaryStructAss> assignments = salaryStructAssService.getAllSalaryStructureAssignments();
        model.addAttribute("assignments", assignments);
        return "salary-struct-ass";
    }

    @GetMapping("/salary-struct-ass/new")
    public String showSalaryStructAssForm(Model model) {
        List<Employee> employees = employeeService.getImportantEmployees();
        model.addAttribute("employees", employees);
        return "salary-struct-ass-form";
    }

    @PostMapping("/salary-struct-ass/create")
    public String createSalaryStructAss(
        @RequestParam String employee,
        @RequestParam String salary_structure,
        @RequestParam String company,
        @RequestParam String from_date,
        @RequestParam(required = false) String to_date,
        @RequestParam String posting_date,
        @RequestParam String base,
        @RequestParam String currency,
        RedirectAttributes redirectAttributes
    ) {
        SalaryStructAss salaryStructAss = new SalaryStructAss();
        salaryStructAss.setEmployee(employee);
        salaryStructAss.setSalary_structure(salary_structure);
        salaryStructAss.setCompany(company);
        salaryStructAss.setFrom_date(from_date);
        salaryStructAss.setTo_date(to_date);
        salaryStructAss.setPosting_date(posting_date);
        salaryStructAss.setBase(new BigDecimal(base));
        salaryStructAss.setCurrency(currency);

        AssignmentAndSlips created = salaryStructAssService.createAssignmentAndSlip(salaryStructAss);

        redirectAttributes.addFlashAttribute("success", "Assignation " + created.assignment()
                + " créée et soumise ; fiche de paie : " + String.join(", ", created.slips()) + ".");
        return "redirect:/salary-struct-ass/list";
    }

    @GetMapping("/salary-struct-ass/generate-form")
    public String generateSalaryStructAssForm(Model model) {
        List<Employee> employees = employeeService.getImportantEmployees();
        model.addAttribute("employees", employees);
        return "salary-struct-ass-generate-form";
    }

    @PostMapping("/salary-struct-ass/generate")
    public String generateSalaryStructAss(
        @RequestParam String employee,
        @RequestParam String salary_structure,
        @RequestParam String company,
        @RequestParam String from_date,
        @RequestParam String to_date,
        @RequestParam String posting_date,
        @RequestParam(required = false) String base,
        @RequestParam String currency,
        @RequestParam(required = false) String ecraser,
        @RequestParam(required = false) String moyenne,
        RedirectAttributes redirectAttributes
    ) {
        SalaryStructAss template = new SalaryStructAss();
        template.setEmployee(employee);
        template.setSalary_structure(salary_structure);
        template.setCompany(company);
        template.setCurrency(currency);
        template.setPosting_date(posting_date);
        if (base != null && !base.trim().isEmpty()) {
            template.setBase(new BigDecimal(base.trim()));
        }

        GenerationResult result = payrollGenerationService.generate(
                template, LocalDate.parse(from_date), LocalDate.parse(to_date),
                "oui".equalsIgnoreCase(ecraser), "oui".equalsIgnoreCase(moyenne));

        StringBuilder message = new StringBuilder()
                .append(result.created().size()).append(" fiche(s) de paie générée(s) pour ").append(employee)
                .append(" du ").append(from_date).append(" au ").append(to_date).append('.');
        if (!result.skipped().isEmpty()) {
            message.append(" Mois ignorés : ").append(String.join(" ; ", result.skipped())).append('.');
        }
        redirectAttributes.addFlashAttribute("success", message.toString());
        if (result.hasFailures()) {
            redirectAttributes.addFlashAttribute("warning",
                    "Échecs (" + result.failed().size() + ") : " + String.join(" ; ", result.failed()));
        }
        return "redirect:/salary-struct-ass/generate-form";
    }

    @GetMapping("/salary-struct-ass/edit/{id}")
    public String editSalaryStructAss(@PathVariable("id") String id, Model model) {
        SalaryStructAss ass = salaryStructAssService.getAssignmentById(id);
        model.addAttribute("assignment", ass);
        return "salary-struct-ass-edit";
    }

    /** Remplace le SSA (annulation + amendement) et régénère ses fiches de paie. */
    @PostMapping("/salary-struct-ass/update")
    public String updateSalaryStructAss(@ModelAttribute SalaryStructAss updatedAss, RedirectAttributes redirectAttributes) {
        AssignmentAndSlips replaced = salaryStructAssService.replaceAssignment(updatedAss);
        redirectAttributes.addFlashAttribute("success", "Assignation " + updatedAss.getName() + " remplacée par "
                + replaced.assignment() + " ; fiche(s) régénérée(s) : "
                + (replaced.slips().isEmpty() ? "aucune" : String.join(", ", replaced.slips())) + ".");
        return "redirect:/salary-struct-ass/list";
    }

    /** Annule un SSA soumis (ou supprime un brouillon). */
    @PostMapping("/salary-struct-ass/delete/{id}")
    public String deleteSalaryStructAss(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        salaryStructAssService.deleteAssignment(id);
        redirectAttributes.addFlashAttribute("success", "Assignation " + id + " annulée (ou supprimée si brouillon).");
        return "redirect:/salary-struct-ass/list";
    }
}
