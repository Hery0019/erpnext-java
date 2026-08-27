package hery.itu.erp.controller.salary;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.model.salary.SalaryGroupedDTO;
import hery.itu.erp.service.salary.SalaryTotalService;

@Controller
public class SalaryTotalController {

    private static final int FIRST_YEAR = 2020;

    private final SalaryTotalService salaryTotalService;

    public SalaryTotalController(SalaryTotalService salaryTotalService) {
        this.salaryTotalService = salaryTotalService;
    }

    @GetMapping("/salary-total")
    public String showSalaryTotal(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "") String year,
            Model model) {

        int selectedYear = parseYear(year);
        Integer selectedMonth = parseMonth(month);

        List<SalaryDTO> slips = salaryTotalService.getDetailedSlips(selectedYear, selectedMonth);
        Map<String, SalaryGroupedDTO> grouped = salaryTotalService.groupByMonth(slips);

        model.addAttribute("groupedSalaries", grouped.values());
        model.addAttribute("filterMonth", selectedMonth == null ? "" : String.format("%02d", selectedMonth));
        model.addAttribute("filterYear", String.valueOf(selectedYear));
        model.addAttribute("months", IntStream.rangeClosed(1, 12).mapToObj(m -> String.format("%02d", m)).toList());
        model.addAttribute("years", IntStream.rangeClosed(FIRST_YEAR, Year.now().getValue() + 1).mapToObj(String::valueOf).toList());
        model.addAttribute("selectedYear", String.valueOf(selectedYear));
        return "salary_total";
    }

    @GetMapping("/salary-total/show")
    public String showSalaries(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "") String year,
            Model model) {
        return showSalaryTotal(month, year, model);
    }

    @GetMapping("/api/salary-total")
    @ResponseBody
    public List<SalaryDTO> getSalaryTotalJson(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "") String year) {
        return salaryTotalService.getDetailedSlips(parseYear(year), parseMonth(month));
    }

    @GetMapping("/salary-total/month-detail")
    public String showSalaryDetailsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            Model model) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mois invalide : " + month);
        }
        model.addAttribute("salaries", salaryTotalService.getDetailedSlips(year, month));
        model.addAttribute("year", year);
        model.addAttribute("month", String.format("%02d", month));
        return "salary_detail_by_month";
    }

    /** Année demandée, ou l'année courante par défaut (plus de 2025 en dur). */
    private static int parseYear(String year) {
        if (year == null || year.isBlank()) {
            return Year.now().getValue();
        }
        try {
            return Integer.parseInt(year.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Année invalide : " + year);
        }
    }

    private static Integer parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(month.trim());
            if (value < 1 || value > 12) {
                throw new IllegalArgumentException("Mois invalide : " + month);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mois invalide : " + month);
        }
    }
}
