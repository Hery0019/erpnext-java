package hery.itu.erp.erpnext;

/** Requête refusée par les règles métier ERPNext (HTTP 400 / 409 / 417 / 422). */
public class ErpNextValidationException extends ErpNextException {

    public ErpNextValidationException(String context, int status, String erpNextMessage) {
        super(context, status, erpNextMessage);
    }
}
