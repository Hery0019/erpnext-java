package hery.itu.erp.controller.facture;

import hery.itu.erp.service.facture.FacturePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/factures")
public class FacturePdfController {
    @Autowired
    private FacturePdfService facturePdfService;

    @GetMapping("/{factureNom}/pdf")
    public ResponseEntity<byte[]> telechargerFacturePdf(@PathVariable String factureNom) {
        try {
            byte[] pdfBytes = facturePdfService.generateFacturePdf(factureNom);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture-" + factureNom + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
