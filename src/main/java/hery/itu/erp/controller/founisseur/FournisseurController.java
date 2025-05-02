package hery.itu.erp.controller.founisseur;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;

import hery.itu.erp.service.fourniseur.FournisseurService;
import hery.itu.erp.service.login.LoginService;
import hery.itu.erp.model.Fournisseur;
import hery.itu.erp.model.FournisseurResponse;
import org.springframework.stereotype.Controller;   
import java.util.List;

import org.springframework.ui.Model;
import hery.itu.erp.model.Devis;

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
        List<Devis> devis = fournisseurService.getDevisParFournisseur(nom); // implémente cette méthode
        model.addAttribute("devis", devis);
        model.addAttribute("nomFournisseur", nom);
        return "liste_devis"; // nom du fichier HTML à afficher
    }

}
