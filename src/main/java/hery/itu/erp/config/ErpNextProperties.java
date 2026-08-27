package hery.itu.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de connexion à l'instance ERPNext (préfixe {@code erpnext} dans application.properties).
 *
 * @param baseUrl URL racine de l'instance, sans slash final (ex. {@code https://erp.example.com})
 */
@ConfigurationProperties(prefix = "erpnext")
public record ErpNextProperties(String baseUrl) {

    public ErpNextProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("La propriété erpnext.base-url est obligatoire");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }
}
