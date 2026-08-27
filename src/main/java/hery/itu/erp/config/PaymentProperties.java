package hery.itu.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Paramètres du paiement des factures d'achat (préfixe {@code erp.payment}).
 *
 * @param namingSeries     série de numérotation des Payment Entry (ex. {@code PE-.YYYY.-})
 * @param paidFromAccount  compte ERPNext débité (ex. {@code Banque - XX}) — dépend du plan comptable de la société
 * @param paidFromCurrency devise de ce compte
 */
@ConfigurationProperties(prefix = "erp.payment")
public record PaymentProperties(
        @DefaultValue("PE-.YYYY.-") String namingSeries,
        String paidFromAccount,
        String paidFromCurrency) {

    public PaymentProperties {
        if (paidFromAccount == null || paidFromAccount.isBlank()) {
            throw new IllegalArgumentException("La propriété erp.payment.paid-from-account est obligatoire");
        }
        if (paidFromCurrency == null || paidFromCurrency.isBlank()) {
            throw new IllegalArgumentException("La propriété erp.payment.paid-from-currency est obligatoire");
        }
    }
}
