package hery.itu.erp.controller.fournisseur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.model.Devis;
import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.ItemDevis;
import hery.itu.erp.service.fournisseur.FournisseurService;

@Controller
public class FournisseurController {
    private final FournisseurService fournisseurService;

    public FournisseurController(FournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    @GetMapping("/fournisseurs")
    public String getFournisseurs(Model model) {
        List<Fournisseur> fournisseurs = fournisseurService.getFournisseurs();
        model.addAttribute("fournisseurs", fournisseurs);
        return "accueil";
    }

    @GetMapping("/fournisseurs/{nom}/devis")
    public String voirDevisParFournisseur(@PathVariable String nom, Model model) {
        List<Devis> devis = fournisseurService.getDevisParFournisseur(nom);
        model.addAttribute("devis", devis);
        model.addAttribute("nomFournisseur", nom);

        // Totaux par devise
        Map<String, Double> totauxParDevise = new HashMap<>();
        for (Devis d : devis) {
            String currency = d.getCurrency();
            if (currency == null) continue;
            double montant = d.getMontant() != null ? d.getMontant() : 0.0;
            totauxParDevise.put(currency, totauxParDevise.getOrDefault(currency, 0.0) + montant);
        }
        model.addAttribute("totauxParDevise", totauxParDevise);

        Map<String, List<ItemDevis>> itemsParDevise = new HashMap<>();
        for (Devis d : devis) {
            String currency = d.getCurrency();
            List<ItemDevis> items = d.getItems();
            if (currency == null || items == null) continue;
            itemsParDevise.computeIfAbsent(currency, k -> new ArrayList<>()).addAll(items);
        }
        model.addAttribute("itemsParDevise", itemsParDevise);

        return "liste_devis";
    }

    @PostMapping("/fournisseurs/devis/{devisId}/items/{itemCode}/updatePrice")
    @ResponseBody
    public ResponseEntity<String> updateItemPrice(
            @PathVariable String devisId,
            @PathVariable String itemCode,
            @RequestParam double newPrice,
            @RequestParam String entrepot) {
        try {
            fournisseurService.modifierPrixItem(devisId, itemCode, newPrice, entrepot);
            return ResponseEntity.ok("Prix mis à jour avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ErpNextException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Erreur lors de la mise à jour du prix : " + e.getErpNextMessage());
        }
    }
}
