package hery.itu.erp.controller.salary;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import hery.itu.erp.model.rh.Employee;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.SalaryStructAssService;
import hery.itu.erp.service.rh.EmployeeService;
import hery.itu.erp.service.util.StringConvertService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
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
    
    @GetMapping("/salary-struct-ass")
    public String showSalaryStructAss(Model model) {
        List<Employee> employees = employeeService.getImportantEmployees();
        model.addAttribute("employees", employees);
        return "salary-struct-ass-form";
    }

    @PostMapping("/salary-struct-ass/create")
    public boolean createSalaryStructAssCsv(SalaryStructAss salaryStructAss) {
        try {
            // 1️⃣ Définir le nom et le dossier
            String refEmploye = stringConvertService.convertEmployeeId(salaryStructAss.getEmployee());
            String folder = "export";
            String fileName = "salary_struct_ass_" + refEmploye + ".csv";

            // 2️⃣ Créer le contenu du CSV
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Mois,Ref Employe,Salaire Base,Salaire\n");
            csvContent.append(
                String.format(
                    "%s,%s,%s,%s\n",
                    salaryStructAss.getFrom_date(),     // Mois
                    refEmploye,                         // Ref Employe
                    salaryStructAss.getBase(),          // Salaire Base
                    salaryStructAss.getSalary_structure() // Salaire Structure
                )
            );

            // 3️⃣ Créer le dossier s'il n'existe pas
            Path directory = Path.of(folder);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 4️⃣ Écrire le fichier CSV
            Path filePath = directory.resolve(fileName);
            Files.writeString(filePath, csvContent.toString());

            // 5️⃣ Lire le CSV en bytes
            byte[] fileBytes = Files.readAllBytes(filePath);

            // 6️⃣ Appeler la fonction qui POST vers ERPNext
            Map result = salaryStructAssService.createSalaryStructAss(fileBytes, "Orinasa SA");
            System.out.println("Résultat ERPNext : " + result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    @GetMapping("/salary-struct-ass/generate-form")
    public String showGenerateForm(Model model) {
        model.addAttribute("salaryStructAss", new SalaryStructAss());
        return "salary-struct-ass-form";
    }

    // @PostMapping("/salary-struct-ass/generate")
    // public String generateSalaryStructAss(@ModelAttribute("salaryStructAss") SalaryStructAss salaryStructAss, @RequestParam("toDate") LocalDate toDate, Model model) {
    //     List<Boolean> success = salaryStructAssService.generateSalaryStructAss(salaryStructAss, toDate);

    //     if (success.stream().allMatch(b -> b)) {
    //         model.addAttribute("success", "Salary struct ass generated successfully");
    //         return "salary-struct-ass-form";
    //     } else {
    //         model.addAttribute("error", "Failed to generate salary struct ass");
    //         return "salary-struct-ass-form";
    //     }
    // }
}
