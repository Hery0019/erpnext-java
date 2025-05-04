package hery.itu.erp.controller.facture;

import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.service.facture.FactureAchatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import hery.itu.erp.model.DetailsFacture;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/factures")
public class FactureAchatController {

    @Autowired
    private FactureAchatService factureAchatService;

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
        if (detailsFacture != null) {
            model.addAttribute("detailsFactures", detailsFacture);
            return "details_facture";
        } else {
            // Gérer l'erreur ici
            return "redirect:/error";
        }
    }

    @PostMapping("/{factureNom}/payer")
    public ResponseEntity<?> payerFacture(@PathVariable String factureNom, @RequestParam Double amount) {
        boolean success = factureAchatService.payerFacture(factureNom, amount);
        if (success) {
            return ResponseEntity.ok().body(Map.of("message", "Paiement effectué avec succès"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur lors du paiement"));
        }
    }
}
