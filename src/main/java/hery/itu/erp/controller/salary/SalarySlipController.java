package hery.itu.erp.controller.salary;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import hery.itu.erp.model.salary.SalarySlip;
import hery.itu.erp.service.salary.SalarySlipPdfService;
import hery.itu.erp.service.salary.SalarySlipService;
import hery.itu.erp.web.Downloads;

@Controller
public class SalarySlipController {
    private final SalarySlipService salarySlipService;
    private final SalarySlipPdfService salarySlipPdfService;

    public SalarySlipController(SalarySlipService salarySlipService, SalarySlipPdfService salarySlipPdfService) {
        this.salarySlipService = salarySlipService;
        this.salarySlipPdfService = salarySlipPdfService;
    }

    @GetMapping("/salaires/{employeeId}")
    public String afficherSalaires(@PathVariable String employeeId, Model model) {
        List<SalarySlip> salaires = salarySlipService.getSalarySlipsByEmployee(employeeId);
        model.addAttribute("salaires", salaires);
        model.addAttribute("employeeId", employeeId);
        return "salaire-page";
    }

    @GetMapping("/salaires/{employeeId}/pdf")
    public ResponseEntity<byte[]> downloadSalarySlipsPdf(@PathVariable String employeeId) {
        byte[] pdf = salarySlipPdfService.render(salarySlipService.getSalarySlipsByEmployee(employeeId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, Downloads.attachment("salary-slips-" + employeeId + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
