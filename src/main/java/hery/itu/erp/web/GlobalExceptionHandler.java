package hery.itu.erp.web;

import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.ui.Model;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.erpnext.ErpNextNotFoundException;
import hery.itu.erp.erpnext.ErpNextUnavailableException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Traduit les exceptions en pages d'erreur compréhensibles, sans jamais exposer de détail interne
 * (stack trace, URL ERPNext, classes). Le détail technique va dans les logs serveur.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_VIEW = "error";

    /** 401 = session ERPNext expirée : on ferme la session applicative et on renvoie au login. 403 = droits insuffisants. */
    @ExceptionHandler(ErpNextForbiddenException.class)
    public String erpNextForbidden(ErpNextForbiddenException e, HttpServletRequest request,
                                   HttpServletResponse response, Model model) {
        if (e.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            log.info("Session ERPNext expirée pour '{}' ({})", currentUser(), e.getMessage());
            new SecurityContextLogoutHandler().logout(request, response, currentAuthentication());
            return "redirect:/?expired";
        }
        log.warn("Accès refusé par ERPNext pour '{}' : {}", currentUser(), e.getMessage());
        return errorView(model, response, HttpStatus.FORBIDDEN, "Accès refusé par ERPNext",
                orDefault(e.getErpNextMessage(), "Votre compte ERPNext n'a pas les droits nécessaires pour cette opération."));
    }

    @ExceptionHandler(ErpNextNotFoundException.class)
    public String erpNextNotFound(ErpNextNotFoundException e, HttpServletResponse response, Model model) {
        log.info("Document ERPNext introuvable : {}", e.getMessage());
        return errorView(model, response, HttpStatus.NOT_FOUND, "Document introuvable",
                orDefault(e.getErpNextMessage(), "Le document demandé n'existe pas (ou plus) dans ERPNext."));
    }

    @ExceptionHandler(ErpNextValidationException.class)
    public String erpNextValidation(ErpNextValidationException e, HttpServletResponse response, Model model) {
        log.warn("Opération refusée par ERPNext : {}", e.getMessage());
        return errorView(model, response, HttpStatus.BAD_REQUEST, "Opération refusée par ERPNext",
                orDefault(e.getErpNextMessage(), "ERPNext a refusé l'opération."));
    }

    @ExceptionHandler(ErpNextUnavailableException.class)
    public String erpNextUnavailable(ErpNextUnavailableException e, HttpServletResponse response, Model model) {
        log.error("ERPNext injoignable : {}", e.getMessage());
        return errorView(model, response, HttpStatus.SERVICE_UNAVAILABLE, "ERPNext est injoignable",
                "Impossible de joindre ERPNext pour le moment. Réessayez dans quelques instants ; si le problème persiste, contactez l'administrateur.");
    }

    @ExceptionHandler(ErpNextException.class)
    public String erpNextError(ErpNextException e, HttpServletResponse response, Model model) {
        log.error("Erreur ERPNext : {}", e.getMessage(), e);
        return errorView(model, response, HttpStatus.BAD_GATEWAY, "Erreur ERPNext",
                orDefault(e.getErpNextMessage(), "ERPNext a renvoyé une réponse inattendue."));
    }

    /** Erreurs de saisie ou d'état signalées par l'application elle-même : le message est destiné à l'utilisateur. */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String invalidRequest(RuntimeException e, HttpServletResponse response, Model model) {
        log.warn("Requête invalide : {}", e.getMessage());
        return errorView(model, response, HttpStatus.BAD_REQUEST, "Requête invalide",
                orDefault(e.getMessage(), "Les données envoyées sont invalides."));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public String invalidDate(DateTimeParseException e, HttpServletResponse response, Model model) {
        log.warn("Date invalide : {}", e.getParsedString());
        return errorView(model, response, HttpStatus.BAD_REQUEST, "Date invalide",
                "La date « " + e.getParsedString() + " » n'est pas au format attendu (AAAA-MM-JJ).");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(MaxUploadSizeExceededException e, HttpServletResponse response, Model model) {
        log.warn("Upload refusé : {}", e.getMessage());
        return errorView(model, response, HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux",
                "Le fichier dépasse la taille maximale autorisée (10 Mo par fichier).");
    }

    @ExceptionHandler(Exception.class)
    public String unexpected(Exception e, HttpServletRequest request, HttpServletResponse response, Model model) {
        // Exceptions MVC standard (405, paramètre manquant, ressource inconnue…) : on garde leur statut.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            log.warn("{} {} -> {} : {}", request.getMethod(), request.getRequestURI(), status.value(), e.getMessage());
            return errorView(model, response, status, status.getReasonPhrase(), messageFor(status));
        }
        if (e instanceof TypeMismatchException) {
            log.warn("{} {} -> paramètre invalide : {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            return errorView(model, response, HttpStatus.BAD_REQUEST, "Requête invalide", "Un paramètre a un format invalide.");
        }
        log.error("Erreur inattendue sur {} {} pour '{}'", request.getMethod(), request.getRequestURI(), currentUser(), e);
        return errorView(model, response, HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue s'est produite. Elle a été journalisée ; contactez l'administrateur si elle se reproduit.");
    }

    private static String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Cette page n'existe pas.";
            case METHOD_NOT_ALLOWED -> "Cette action n'est pas accessible par cette méthode.";
            case BAD_REQUEST -> "La requête est incomplète ou invalide (paramètre manquant ou mal formé).";
            case PAYLOAD_TOO_LARGE -> "Le contenu envoyé est trop volumineux.";
            default -> "La requête n'a pas pu être traitée.";
        };
    }

    private static String errorView(Model model, HttpServletResponse response, HttpStatus status, String title, String message) {
        response.setStatus(status.value());
        model.addAttribute("error", title);
        model.addAttribute("message", message);
        model.addAttribute("status", status.value());
        return ERROR_VIEW;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static String currentUser() {
        Authentication authentication = currentAuthentication();
        return authentication == null ? "anonyme" : authentication.getName();
    }
}
