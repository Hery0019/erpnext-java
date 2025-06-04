package hery.itu.erp.controller.salary;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Table;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.UnitValue;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import hery.itu.erp.model.salary.SalarySlip;
import hery.itu.erp.service.salary.SalarySlipService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SalarySlipController {
    private SalarySlipService salarySlipService;

    public SalarySlipController(SalarySlipService salarySlipService) {
        this.salarySlipService = salarySlipService;
    }

    @GetMapping("/{salarySlipName}")
    public SalarySlip getSalarySlipDetail(@PathVariable String salarySlipName) {
        return salarySlipService.getSalarySlipDetail(salarySlipName);
    }

    @GetMapping("/salaires/{employeeId}")
    public String afficherSalaires(@PathVariable String employeeId, Model model) {
        List<SalarySlip> salaires = salarySlipService.getSalarySlipsByEmployee(employeeId);
        model.addAttribute("salaires", salaires);
        model.addAttribute("employeeId", employeeId);
        return "salaire-page"; // correspond à salaire-page.html dans templates
    }

    @GetMapping("/salaires/{employeeId}/pdf") 
    public void downloadSalarySlipsPdf(
        @PathVariable String employeeId,
        jakarta.servlet.http.HttpServletResponse response
    ) throws IOException {
        List<SalarySlip> slips = salarySlipService.getSalarySlipsByEmployee(employeeId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=salary-slips-" + employeeId + ".pdf");

        try (
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(response.getOutputStream());
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc)
        ) {
            TextAlignment alignLeft = TextAlignment.LEFT;
            TextAlignment alignCenter = TextAlignment.CENTER;

            NumberFormat currencyFormat = NumberFormat.getInstance(Locale.FRANCE);
            currencyFormat.setMinimumFractionDigits(0);
            currencyFormat.setMaximumFractionDigits(0);

            for (SalarySlip slip : slips) {

                // --- Ajouter un logo ---
                try {
                    ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
                    ImageData logoData = ImageDataFactory.create(logoResource.getInputStream().readAllBytes());
                    Image logo = new Image(logoData).scaleToFit(120, 60).setHorizontalAlignment(HorizontalAlignment.CENTER);
                    document.add(logo);
                } catch (Exception e) {
                    // Logo manquant ? Pas bloquant
                }

                // --- Ligne décorative ---
                document.add(new Paragraph("______________________________________________________________________________"));
                
                document.add(new Paragraph("FICHE DE PAIE")
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(alignCenter)
                    .setMarginBottom(20));

                document.add(new Paragraph("Informations de l'employé")
                    .setFontSize(12)
                    .setBold()
                    .setMarginBottom(10));

                document.add(new Paragraph("Nom de l'employé : " + safeString(slip.getEmployee_name())).setTextAlignment(alignLeft));
                document.add(new Paragraph("Matricule : " + safeString(slip.getEmployee())).setTextAlignment(alignLeft));
                document.add(new Paragraph("Période : " + safeString(slip.getStart_date()) + " au " + safeString(slip.getEnd_date())).setTextAlignment(alignLeft));
                document.add(new Paragraph("Date de génération : " + safeString(slip.getPosting_date())).setTextAlignment(alignLeft));

                document.add(new Paragraph("\nDétails du salaire")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(20)
                    .setMarginBottom(10));

                document.add(new Paragraph("Salaire brut : " + currencyFormat.format(slip.getGross_pay()) + " MGA").setTextAlignment(alignLeft));
                document.add(new Paragraph("Salaire net : " + currencyFormat.format(slip.getNet_pay()) + " MGA").setTextAlignment(alignLeft));
                document.add(new Paragraph("Structure salariale : " + safeString(slip.getSalary_structure())).setTextAlignment(alignLeft));
                document.add(new Paragraph("Statut : " + safeString(slip.getStatus())).setTextAlignment(alignLeft));
                document.add(new Paragraph("Entreprise : " + safeString(slip.getCompany())).setTextAlignment(alignLeft));

                // document.add(new Paragraph("\nSignature RH : __________________________")
                //     .setMarginTop(30));

                // Saut de page
                if (slips.indexOf(slip) < slips.size() - 1) {
                    document.add(new AreaBreak());
                }
            }
        }
    }

    // Méthode utilitaire pour éviter les NullPointerException
    private String safeString(String value) {
        return value != null ? value : "";
    }
}