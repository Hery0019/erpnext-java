package hery.itu.erp.controller.salary;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
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

    private final SalaryTotalService salaryTotalService;

    public SalaryTotalController(SalaryTotalService salaryTotalService) {
        this.salaryTotalService = salaryTotalService;
    }

    @GetMapping("/salary-total")
    public String showSalaryTotal(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "2025") String year,
            Model model) {

        List<SalaryDTO> allSalaries = salaryTotalService.getAllSalary();

        List<String> months = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> String.format("%02d", m))
                .toList();

        List<String> years = IntStream.rangeClosed(2020, 2030)
                .mapToObj(String::valueOf)
                .toList();

        List<SalaryDTO> filteredSalaries;
        if (!month.isEmpty()) {
            String fullMonth = year + "-" + month;
            filteredSalaries = allSalaries.stream()
                    .filter(s -> s.getMonth().equals(fullMonth))
                    .map(slip -> salaryTotalService.getSalarySlipDetail(slip.getSlipName()))
                    .toList();
        } else {
            filteredSalaries = allSalaries.stream()
                    .filter(s -> s.getMonth().startsWith(year))
                    .map(slip -> salaryTotalService.getSalarySlipDetail(slip.getSlipName()))
                    .toList();
        }

        // Regroupement des salaires par mois
        Map<String, SalaryGroupedDTO> groupedMap = new TreeMap<>();
        for (SalaryDTO detailed : filteredSalaries) {
            String monthKey = detailed.getMonth();
            SalaryGroupedDTO group = groupedMap.getOrDefault(monthKey, new SalaryGroupedDTO());
            group.setMonth(monthKey);

            group.setTotalGross(group.getTotalGross().add(detailed.getGrossPay()));
            group.setTotalDeduction(group.getTotalDeduction().add(detailed.getTotalDeduction()));
            group.setTotalNet(group.getTotalNet().add(detailed.getNetPay()));

            if (detailed.getEarnings() != null) {
                for (var e : detailed.getEarnings()) {
                    group.getEarningsTotal().merge(e.getSalaryComponent(), e.getAmount(), BigDecimal::add);
                }
            }

            if (detailed.getDeductions() != null) {
                for (var d : detailed.getDeductions()) {
                    group.getDeductionsTotal().merge(d.getSalaryComponent(), d.getAmount(), BigDecimal::add);
                }
            }

            groupedMap.put(monthKey, group);
        }

        model.addAttribute("groupedSalaries", groupedMap.values());
        model.addAttribute("filterMonth", month);
        model.addAttribute("filterYear", year);
        model.addAttribute("months", months);
        model.addAttribute("years", years);
        model.addAttribute("selectedYear", year);

        return "salary_total";
    }

    @GetMapping("/salary-total/show")
    public String showSalaries(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "2025") String year,
            Model model) {
        return showSalaryTotal(month, year, model);
    }

    @GetMapping("/api/salary-total")
    @ResponseBody
    public List<SalaryDTO> getSalaryTotalJson(
            @RequestParam(name = "month", required = false, defaultValue = "") String month,
            @RequestParam(name = "year", required = false, defaultValue = "2025") String year) {

        List<SalaryDTO> allSalaries = salaryTotalService.getAllSalary();

        if (!month.isEmpty()) {
            String fullMonth = year + "-" + month;
            return allSalaries.stream()
                    .filter(s -> s.getMonth().equals(fullMonth))
                    .map(slip -> salaryTotalService.getSalarySlipDetail(slip.getSlipName()))
                    .toList();
        } else {
            return allSalaries.stream()
                    .filter(s -> s.getMonth().startsWith(year))
                    .map(slip -> salaryTotalService.getSalarySlipDetail(slip.getSlipName()))
                    .toList();
        }
    }

    @GetMapping("/salary-total/month-detail")
    public String showSalaryDetailsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            Model model) {

        List<SalaryDTO> salaries = salaryTotalService.getSalarySlipsByMonth(year, month);

        model.addAttribute("salaries", salaries);
        model.addAttribute("year", year);
        model.addAttribute("month", String.format("%02d", month));
        
        return "salary_detail_by_month"; // À créer en tant que page Thymeleaf
    }

    
}
