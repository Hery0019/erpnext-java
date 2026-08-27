package hery.itu.erp.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * Principal d'un utilisateur authentifié auprès d'ERPNext.
 * Le cookie de session ERPNext est propre à cet utilisateur et vit dans sa HttpSession.
 *
 * @param username  identifiant ERPNext saisi au login
 * @param sidCookie cookie de session au format {@code sid=xxxx}
 */
public record ErpNextUser(String username, String sidCookie) implements Principal, Serializable {

    /** Nom exposé par {@code Authentication.getName()}. */
    @Override
    public String getName() {
        return username;
    }

    /** Ne jamais exposer le cookie de session dans les logs. */
    @Override
    public String toString() {
        return "ErpNextUser[" + username + "]";
    }
}
