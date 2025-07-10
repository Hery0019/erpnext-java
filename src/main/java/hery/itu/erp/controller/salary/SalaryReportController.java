package hery.itu.erp.controller.salary;

import hery.itu.erp.service.salary.SalaryReportService;
import hery.itu.erp.service.salary.SalaryComponentService;
import hery.itu.erp.service.rh.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.math.BigDecimal;

import java.util.*;

@Controller
public class SalaryReportController {

    @Autowired
    SalaryReportService salaryReportService;
    @Autowired
    SalaryComponentService salaryComponentService;
    @Autowired
    EmployeeService employeeService;

    @GetMapping("/salaries")
    public String showSalaryReportPage(Model model) throws Exception {
        model.addAttribute("employees", employeeService.getImportantEmployees());
        model.addAttribute("salaryComponents", salaryComponentService.getAllSalaryComponentNames());
        return "salary_report";
    }

    @GetMapping("/salaries/report")
    public String showSalaryReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "Orinasa SA") String company,
            @RequestParam(defaultValue = "") String salaryComponent,
            @RequestParam(defaultValue = "") String signe,
            @RequestParam(defaultValue = "") String employee,
            @RequestParam(defaultValue = "0") double combien,
            Model model) {

        try {
            Map<String, Object> data = salaryReportService.getSalaryReport(fromDate, toDate, company, employee, salaryComponent, signe, combien);

            Object messageObj = data.get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                model.addAttribute("error", "Format inattendu des données provenant d'ERPNext.");
                return "salary_report";
            }

            Object resultObj = message.get("result");
            if (!(resultObj instanceof List<?> resultList)) {
                model.addAttribute("error", "Résultat vide ou mal formé.");
                return "salary_report";
            }

            List<Map<String, Object>> salaries = new ArrayList<>();
            List<Object> totalRow = null;

            for (Object obj : resultList) {
                if (obj instanceof Map<?, ?> rowMap) {
                    salaries.add((Map<String, Object>) rowMap);
                } else if (obj instanceof List<?> listRow) {
                    totalRow = (List<Object>) listRow;
                }
            }

            model.addAttribute("salaries", salaries);
            model.addAttribute("totalRow", totalRow);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("company", company);
            model.addAttribute("employee", employee);
            model.addAttribute("salaryComponent", salaryComponent);
            model.addAttribute("signe", signe);
            model.addAttribute("componentsName", salaryReportService.getFormattedSalaryComponentNames());

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération du rapport : " + e.getMessage());
        }

        return "salary_report";
    }




}
