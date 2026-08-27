package hery.itu.erp.service.salary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import hery.itu.erp.config.PayrollProperties;
import hery.itu.erp.model.salary.SalarySlip;

/**
 * Rendu PDF des fiches de paie (une page par fiche), avec OpenPDF — même bibliothèque que les
 * factures, plus de dépendance iText.
 */
@Service
public class SalarySlipPdfService {

    private static final Logger log = LoggerFactory.getLogger(SalarySlipPdfService.class);
    private static final String LOGO_PATH = "static/images/logo.png";

    private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SECTION = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font TEXT = new Font(Font.HELVETICA, 11, Font.NORMAL);

    private final String currencyLabel;
    private final byte[] logo;

    public SalarySlipPdfService(PayrollProperties payrollProperties) {
        this.currencyLabel = payrollProperties.currencyLabel();
        this.logo = loadLogo();
    }

    /** PDF contenant une page par fiche ; document vide (une page « Aucune fiche ») si la liste est vide. */
    public byte[] render(List<SalarySlip> slips) {
        NumberFormat amountFormat = NumberFormat.getInstance(Locale.FRANCE);
        amountFormat.setMinimumFractionDigits(0);
        amountFormat.setMaximumFractionDigits(0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            if (slips.isEmpty()) {
                document.add(new Paragraph("Aucune fiche de paie.", TEXT));
            }
            for (int i = 0; i < slips.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                addSlip(document, slips.get(i), amountFormat);
            }
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Impossible de générer le PDF des fiches de paie", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private void addSlip(Document document, SalarySlip slip, NumberFormat amountFormat) throws DocumentException, IOException {
        if (logo != null) {
            Image image = Image.getInstance(logo);
            image.scaleToFit(120, 60);
            image.setAlignment(Element.ALIGN_CENTER);
            document.add(image);
        }

        Paragraph title = new Paragraph("FICHE DE PAIE", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Paragraph employeeSection = new Paragraph("Informations de l'employé", SECTION);
        employeeSection.setSpacingAfter(8);
        document.add(employeeSection);
        document.add(new Paragraph("Nom de l'employé : " + safe(slip.getEmployee_name()), TEXT));
        document.add(new Paragraph("Matricule : " + safe(slip.getEmployee()), TEXT));
        document.add(new Paragraph("Période : " + safe(slip.getStart_date()) + " au " + safe(slip.getEnd_date()), TEXT));
        document.add(new Paragraph("Date de génération : " + safe(slip.getPosting_date()), TEXT));

        Paragraph salarySection = new Paragraph("Détails du salaire", SECTION);
        salarySection.setSpacingBefore(20);
        salarySection.setSpacingAfter(8);
        document.add(salarySection);
        document.add(new Paragraph("Salaire brut : " + amountFormat.format(slip.getGross_pay()) + " " + currencyLabel, TEXT));
        document.add(new Paragraph("Salaire net : " + amountFormat.format(slip.getNet_pay()) + " " + currencyLabel, TEXT));
        document.add(new Paragraph("Structure salariale : " + safe(slip.getSalary_structure()), TEXT));
        document.add(new Paragraph("Statut : " + safe(slip.getStatus()), TEXT));
        document.add(new Paragraph("Entreprise : " + safe(slip.getCompany()), TEXT));
    }

    private static byte[] loadLogo() {
        try (InputStream in = new ClassPathResource(LOGO_PATH).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            log.warn("Logo {} introuvable : les fiches de paie seront générées sans logo", LOGO_PATH);
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
