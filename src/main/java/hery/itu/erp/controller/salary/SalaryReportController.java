package hery.itu.erp.controller.salary;

import hery.itu.erp.service.salary.SalaryReportService;
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

    @GetMapping("/salaries")
    public String showSalaryReportPage() {
        return "salary_report";
    }

    @GetMapping("/salaries/report")
    public String showSalaryReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "My Company") String company,
            Model model
    ) {
        // Récupération des données du service
        Map<String, Object> data = salaryReportService.getSalaryReport(fromDate, toDate, company);

        // Extraction du résultat
        Object messageObj = data.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            model.addAttribute("error", "Unexpected data format from ERPNext");
            return "salary_report";
        }

        List<Map<String, Object>> salaries = new ArrayList<>();
        List<Object> totalRow = null;

        Object resultObj = message.get("result");
        if (resultObj instanceof List<?> resultList) {
            for (Object obj : resultList) {
                if (obj instanceof Map<?, ?>) {
                    salaries.add((Map<String, Object>) obj);
                } else if (obj instanceof List<?> rowList) {
                    totalRow = (List<Object>) rowList;
                }
            }
        }

        List<String> componentsName = salaryReportService.getFormattedSalaryComponentNames();

        
        // Attributs pour Thymeleaf
        model.addAttribute("salaries", salaries);
        model.addAttribute("totalRow", totalRow);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("company", company);
        model.addAttribute("componentsName", componentsName);

        return "salary_report";
    }


}
