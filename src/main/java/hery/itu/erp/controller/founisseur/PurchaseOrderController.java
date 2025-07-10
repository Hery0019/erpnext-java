package hery.itu.erp.controller.founisseur;

import hery.itu.erp.model.PurchaseOrder;
import hery.itu.erp.service.fourniseur.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/fournisseur")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping("/commandes")
    public String getCommandesFournisseur(
            @RequestParam String nom,
            @RequestParam(required = false) String statut,
            Model model) {
        List<PurchaseOrder> commandes = purchaseOrderService.getCommandesParFournisseur(nom, statut);
        model.addAttribute("nomFournisseur", nom);
        model.addAttribute("commandes", commandes);
        model.addAttribute("statutFiltre", statut);
        return "liste_commandes";
    }
}
