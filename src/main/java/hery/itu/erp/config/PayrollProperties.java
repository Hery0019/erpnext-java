package hery.itu.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Paramètres de la paie (préfixe {@code erp.payroll}).
 *
 * @param defaultCompany société proposée par défaut dans le rapport de salaires (vide = aucune)
 * @param currencyLabel  libellé de devise affiché sur les fiches de paie PDF
 */
@ConfigurationProperties(prefix = "erp.payroll")
public record PayrollProperties(
        @DefaultValue("") String defaultCompany,
        @DefaultValue("MGA") String currencyLabel) {
}
