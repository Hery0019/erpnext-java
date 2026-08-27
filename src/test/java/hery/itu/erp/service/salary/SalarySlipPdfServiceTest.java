package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import hery.itu.erp.config.PayrollProperties;
import hery.itu.erp.model.salary.SalarySlip;

class SalarySlipPdfServiceTest {

    private final SalarySlipPdfService service = new SalarySlipPdfService(new PayrollProperties("Orinasa SA", "MGA"));

    @Test
    void unePageParFicheAvecLesMontantsFormates() throws IOException {
        byte[] pdf = service.render(List.of(slip("00001", "2025-01-01", "2025-01-31", 1500000), slip("00002", "2025-02-01", "2025-02-28", 1600000)));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        String page1 = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(page1).contains("FICHE DE PAIE").contains("HR-EMP-00001").contains("2025-01-01").contains("MGA");
        // le séparateur de milliers (espace insécable) ressort comme espaces à l'extraction : on compare sans blancs
        assertThat(page1.replaceAll("[\s\u00A0\u202F]", "")).contains("Salairebrut:1500000MGA");
        reader.close();
    }

    @Test
    void listeVideDonneUnPdfValide() throws IOException {
        byte[] pdf = service.render(List.of());

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        reader.close();
    }

    private static SalarySlip slip(String suffix, String start, String end, double gross) {
        SalarySlip s = new SalarySlip();
        s.setName("Sal Slip/HR-EMP-00001/" + suffix);
        s.setEmployee("HR-EMP-00001");
        s.setEmployee_name("Ana Bé");
        s.setStart_date(start);
        s.setEnd_date(end);
        s.setPosting_date(end);
        s.setGross_pay(gross);
        s.setNet_pay(gross * 0.8);
        s.setStatus("Submitted");
        s.setSalary_structure("Standard");
        s.setCompany("Orinasa SA");
        return s;
    }
}
