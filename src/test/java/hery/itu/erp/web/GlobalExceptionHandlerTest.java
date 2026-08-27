package hery.itu.erp.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.erpnext.ErpNextNotFoundException;
import hery.itu.erp.erpnext.ErpNextUnavailableException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.service.facture.FactureAchatService;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FactureAchatService factureAchatService;

    @Test
    void validationErpNextAfficheLeMessageMetierSansDetailTechnique() throws Exception {
        when(factureAchatService.getAllFactures())
                .thenThrow(new ErpNextValidationException("GET /api/resource/Purchase Invoice", 417, "Compte Créditeurs introuvable"));

        mvc.perform(get("/factures").with(user("hery")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("message", "Compte Créditeurs introuvable"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("/api/resource"))));
    }

    @Test
    void sessionErpNextExpireeRenvoieAuLogin() throws Exception {
        when(factureAchatService.getAllFactures())
                .thenThrow(new ErpNextForbiddenException("GET /api/resource/Purchase Invoice", 401, "Not logged in"));

        mvc.perform(get("/factures").with(user("hery")))
                .andExpect(redirectedUrl("/?expired"));
    }

    @Test
    void droitsInsuffisantsDonnent403AvecLeMessage() throws Exception {
        when(factureAchatService.getAllFactures())
                .thenThrow(new ErpNextForbiddenException("GET /api/resource/Purchase Invoice", 403, "Not permitted"));

        mvc.perform(get("/factures").with(user("hery")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("message", "Not permitted"));
    }

    @Test
    void documentIntrouvableDonne404() throws Exception {
        when(factureAchatService.getFactureByName("X"))
                .thenThrow(new ErpNextNotFoundException("GET /api/resource/Purchase Invoice/X", "Purchase Invoice X not found"));

        mvc.perform(get("/factures/X").with(user("hery")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void erpNextInjoignableDonne503() throws Exception {
        when(factureAchatService.getAllFactures())
                .thenThrow(new ErpNextUnavailableException("GET /api/resource/Purchase Invoice", 0, "Connection refused", null));

        mvc.perform(get("/factures").with(user("hery")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(content().string(not(containsString("Connection refused"))));
    }

    @Test
    void dateMalFormeeDonne400AvecUnMessageClair() throws Exception {
        mvc.perform(post("/salary-struct-ass/generate").with(user("hery")).with(csrf())
                        .param("employee", "HR-EMP-00001").param("salary_structure", "Standard")
                        .param("company", "Orinasa SA").param("currency", "MGA")
                        .param("from_date", "31/03/2025").param("to_date", "2025-03-31").param("posting_date", "2025-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "Date invalide"))
                .andExpect(content().string(containsString("31/03/2025")));
    }

    @Test
    void parametreManquantDonne400EtNon500() throws Exception {
        mvc.perform(get("/fournisseur/commandes").with(user("hery")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    void erreurInattendueDonne500SansDivulguerLaCause() throws Exception {
        when(factureAchatService.getAllFactures()).thenThrow(new RuntimeException("détail interne secret"));

        mvc.perform(get("/factures").with(user("hery")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(not(containsString("détail interne secret"))));
    }
}
