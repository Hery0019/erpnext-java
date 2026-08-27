package hery.itu.erp.controller.facture;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /** Formulaire HTML : le résultat est affiché sur la page de détail (message flash). */
    @PostMapping("/{factureNom}/payer")
    public String payerFacture(@PathVariable String factureNom, @RequestParam double amount,
                               RedirectAttributes redirectAttributes) {
        if (amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Le montant à payer doit être positif.");
        } else {
            try {
                factureAchatService.payerFacture(factureNom, amount);
                redirectAttributes.addFlashAttribute("success",
                        "Paiement de " + amount + " enregistré et soumis pour la facture " + factureNom + ".");
            } catch (ErpNextException e) {
                redirectAttributes.addFlashAttribute("error", "Paiement refusé par ERPNext : " + e.getErpNextMessage());
            }
        }
        redirectAttributes.addAttribute("name", factureNom);
        return "redirect:/factures/{name}/detailsFacture";
    }
}
