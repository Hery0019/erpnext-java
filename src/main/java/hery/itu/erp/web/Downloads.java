package hery.itu.erp.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;

/** En-têtes de téléchargement : le nom de fichier est assaini avant d'être placé dans Content-Disposition. */
public final class Downloads {

    private Downloads() {
    }

    /**
     * {@code attachment; filename="..."} avec un nom réduit à [A-Za-z0-9._-] (les autres caractères,
     * dont "/" et les guillemets, deviennent "_") : jamais de valeur utilisateur brute dans l'en-tête.
     */
    public static String attachment(String filename) {
        String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return ContentDisposition.attachment().filename(safe, StandardCharsets.UTF_8).build().toString();
    }
}
