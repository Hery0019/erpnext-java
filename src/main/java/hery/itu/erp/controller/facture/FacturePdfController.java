package hery.itu.erp.controller.facture;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hery.itu.erp.service.facture.FacturePdfService;
import hery.itu.erp.web.Downloads;

@RestController
@RequestMapping("/factures")
public class FacturePdfController {
    private final FacturePdfService facturePdfService;

    public FacturePdfController(FacturePdfService facturePdfService) {
        this.facturePdfService = facturePdfService;
    }

    /** Les erreurs ERPNext sont traduites par GlobalExceptionHandler (404, 503…). */
    @GetMapping("/{factureNom}/pdf")
    public ResponseEntity<byte[]> telechargerFacturePdf(@PathVariable String factureNom) throws Exception {
        byte[] pdfBytes = facturePdfService.generateFacturePdf(factureNom);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, Downloads.attachment("facture-" + factureNom + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
