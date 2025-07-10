package hery.itu.erp.service.facture;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import hery.itu.erp.model.DetailsFacture;
import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.model.Item;

@Service
public class FacturePdfService {
    @Autowired
    private FactureAchatService factureAchatService;

    public byte[] generateFacturePdf(String factureNom) throws Exception {
        // Récupérer les détails de la facture
        var details = factureAchatService.getDetailsFacture(factureNom);
        if (details == null) throw new Exception("Facture introuvable");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph("Facture : " + factureNom));
        document.add(new Paragraph("Fournisseur : " + details.getFacture().getSupplierName()));
        document.add(new Paragraph("Date : " + details.getFacture().getPostingDate()));
        document.add(new Paragraph("Statut : " + details.getFacture().getStatus()));
        document.add(new Paragraph("Montant total : " + details.getGrandTotal()));
        document.add(new Paragraph("Montant dû : " + details.getFacture().getOutstandingAmount()));
        document.add(new Paragraph("---"));
        document.add(new Paragraph("Détails des items : "));
        for (var item : details.getItems()) {
            document.add(new Paragraph(item.getItem_name() + " - Qte: " + item.getQty() + " - PU: " + item.getRate()));
        }
        document.close();
        return baos.toByteArray();
    }
}
