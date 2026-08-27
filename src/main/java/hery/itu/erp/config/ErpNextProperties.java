package hery.itu.erp.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Paramètres de connexion à l'instance ERPNext (préfixe {@code erpnext} dans application.properties).
 *
 * @param baseUrl        URL racine de l'instance, sans slash final (ex. {@code https://erp.example.com})
 * @param connectTimeout délai max d'établissement de connexion
 * @param readTimeout    délai max d'attente d'une réponse
 * @param pageSize       taille de page utilisée pour parcourir les listes ({@code limit_page_length})
 */
@ConfigurationProperties(prefix = "erpnext")
public record ErpNextProperties(
        String baseUrl,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("500") int pageSize) {

    @ConstructorBinding
    public ErpNextProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("La propriété erpnext.base-url est obligatoire");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("erpnext.page-size doit être > 0");
        }
    }

    /** Valeurs par défaut pour les timeouts et la pagination. */
    public ErpNextProperties(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(5), Duration.ofSeconds(30), 500);
    }
}
