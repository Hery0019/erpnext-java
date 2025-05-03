package hery.itu.erp.controller.facture;

import hery.itu.erp.model.FactureAchat;
import hery.itu.erp.service.facture.FactureAchatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return "detail_facture";
    }
}
