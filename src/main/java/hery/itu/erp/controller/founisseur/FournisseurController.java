package hery.itu.erp.controller.founisseur;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hery.itu.erp.service.fourniseur.FournisseurService;
import hery.itu.erp.service.login.LoginService;

import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.FournisseurResponse;
import org.springframework.stereotype.Controller;   
import java.util.List;

import org.springframework.ui.Model;
import hery.itu.erp.model.Devis;
import hery.itu.erp.model.ItemDevis;

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

        // Calculer les totaux par devise
        java.util.Map<String, Double> totauxParDevise = new java.util.HashMap<>();
        for (Devis d : devis) {
            String currency = d.getCurrency();
            if (currency == null) continue;
            double montant = d.getMontant() != null ? d.getMontant() : 0.0;
            totauxParDevise.put(currency, totauxParDevise.getOrDefault(currency, 0.0) + montant);
        }
        model.addAttribute("totauxParDevise", totauxParDevise);

        // Préparer la map itemsParDevise
        java.util.Map<String, java.util.List<ItemDevis>> itemsParDevise = new java.util.HashMap<>();
        for (Devis d : devis) {
            String currency = d.getCurrency();
            java.util.List<ItemDevis> items = d.getItems();
            if (currency == null || items == null) continue;
            itemsParDevise.computeIfAbsent(currency, k -> new java.util.ArrayList<>()).addAll(items);
        }
        model.addAttribute("itemsParDevise", itemsParDevise);

        return "liste_devis";
    }


    @PostMapping("/fournisseurs/devis/{devisId}/items/{itemCode}/updatePrice")
    @ResponseBody
    public ResponseEntity<String> updateItemPrice(
            @PathVariable String devisId,
            @PathVariable String itemCode,
            @RequestParam Double newPrice) {
        try {
            fournisseurService.modifierPrixItem(devisId, itemCode, newPrice);
            return ResponseEntity.ok("Prix mis à jour avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du prix: " + e.getMessage());
        }
    }

    
}
