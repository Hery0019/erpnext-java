package hery.itu.erp.controller.salary;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.SalaryStructAssService;
import hery.itu.erp.service.rh.EmployeeService;
import hery.itu.erp.service.util.StringConvertService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;



@Controller
public class SalaryStructAssController {    
    private final SalaryStructAssService salaryStructAssService;
    private final EmployeeService employeeService;
    private final StringConvertService stringConvertService;

    public SalaryStructAssController(SalaryStructAssService salaryStructAssService, EmployeeService employeeService, StringConvertService stringConvertService) {
        this.salaryStructAssService = salaryStructAssService;
        this.employeeService = employeeService;
        this.stringConvertService = stringConvertService;
    }

    @GetMapping("/salary-struct-ass/list")
    public String listSalaryStructureAssignments(Model model) throws Exception {
        // Appel du service pour récupérer la liste
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
        Model model
    ) throws Exception {
        // Appel service
        SalaryStructAss salaryStructAss = new SalaryStructAss();
        salaryStructAss.setEmployee(employee);
        salaryStructAss.setSalary_structure(salary_structure);
        salaryStructAss.setCompany(company);
        salaryStructAss.setFrom_date(from_date);
        salaryStructAss.setTo_date(to_date);
        salaryStructAss.setPosting_date(posting_date);
        salaryStructAss.setBase(new BigDecimal(base));
        salaryStructAss.setCurrency(currency);

        salaryStructAssService.createAssignmentAndSlip(salaryStructAss);

        model.addAttribute("success", "Salary Struct Ass created successfully");

        return "redirect:/salary-struct-ass";
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
        @RequestParam String from_date,  // date début
        @RequestParam String to_date,    // date fin
        @RequestParam String posting_date,
        @RequestParam(required = false) String base,  // peut être null ou vide
        @RequestParam String currency,
        @RequestParam(required = false) String ecraser,  // peut être null ou vide
        @RequestParam(required = false) String moyenne,  // peut être null ou vide
        Model model
    ) throws Exception {

        // ✅ Nettoyer base si vide ou seulement des espaces
        if (base != null && base.trim().isEmpty()) {
            base = null;
        }

        // 🔑 Construire SalaryStructAss de base
        SalaryStructAss salaryStructAss = new SalaryStructAss();
        salaryStructAss.setName("Salary Structure Assignment " + employee);
        salaryStructAss.setEmployee(employee);
        salaryStructAss.setSalary_structure(salary_structure);
        salaryStructAss.setCompany(company);
        salaryStructAss.setCurrency(currency);

        if (base != null) {
            salaryStructAss.setBase(new BigDecimal(base));
        }

        salaryStructAss.setPosting_date(posting_date);

        // ✅ Appeler la nouvelle méthode generateSalary avec LocalDate
        LocalDate start = LocalDate.parse(from_date);
        LocalDate end = LocalDate.parse(to_date);

        var slips = salaryStructAssService.generateSalary(salaryStructAss, start, end, ecraser, moyenne);

        model.addAttribute("success",
            "Salary Slips générés : " + slips.size() + " slips créés de " + from_date + " à " + to_date
        );

        return "redirect:/salary-struct-ass/generate-form";
    }

    // ✅ Affiche le formulaire de modification pour un Salary Structure Assignment
    @GetMapping("/salary-struct-ass/edit/{id}")
    public String editSalaryStructAss(@PathVariable("id") String id, Model model) throws Exception {
        // Récupère les infos depuis ERPNext via le service
        SalaryStructAss ass = salaryStructAssService.getAssignmentById(id);
        model.addAttribute("assignment", ass);
        return "salary-struct-ass-edit"; // la page Thymeleaf qu'on va créer
    }

    // ✅ Soumet le formulaire modifié
    @PostMapping("/salary-struct-ass/update")
    public String updateSalaryStructAss(@ModelAttribute SalaryStructAss updatedAss, Model model) throws Exception {
        salaryStructAssService.updateAssignment(updatedAss);
        return "redirect:/salary-struct-ass/list";
    }

    // ✅ Supprime un Salary Structure Assignment
    @GetMapping("/salary-struct-ass/delete/{id}")
    public String deleteSalaryStructAss(@PathVariable("id") String id, Model model) throws Exception {
        salaryStructAssService.deleteAssignment(id);
        return "redirect:/salary-struct-ass/list";
    }
    
}
