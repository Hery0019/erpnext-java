package hery.itu.erp.controller.facture;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.model.DetailsFacture;
import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.service.facture.FactureAchatService;

@Controller
@RequestMapping("/factures")
public class FactureAchatController {

    private final FactureAchatService factureAchatService;

    public FactureAchatController(FactureAchatService factureAchatService) {
        this.factureAchatService = factureAchatService;
    }

    @GetMapping("")
    public String afficherFactures(Model model) {
        List<FactureAchat> factures = factureAchatService.getAllFactures();
        model.addAttribute("factures", factures);
        return "liste_factures";
    }

    @GetMapping("/{name}")
    public String afficherDetailFacture(@PathVariable("name") String name, Model model) {
        FactureAchat facture = factureAchatService.getFactureByName(name);
        model.addAttribute("facture", facture);
        return "facture";
    }

    @GetMapping("/{factureNom}/detailsFacture")
    public String voirDetailsFacture(@PathVariable String factureNom, Model model) {
        DetailsFacture detailsFacture = factureAchatService.getDetailsFacture(factureNom);
        model.addAttribute("detailsFactures", detailsFacture);
        return "details_facture";
    }

    @PostMapping("/{factureNom}/payer")
    public ResponseEntity<Map<String, String>> payerFacture(@PathVariable String factureNom, @RequestParam double amount) {
        try {
            factureAchatService.payerFacture(factureNom, amount);
            return ResponseEntity.ok(Map.of("message", "Paiement effectué avec succès"));
        } catch (ErpNextException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur lors du paiement : " + e.getErpNextMessage()));
        }
    }
}
