package hery.itu.erp.erpnext;

/** ERPNext injoignable, timeout, ou erreur serveur (HTTP 5xx). */
public class ErpNextUnavailableException extends ErpNextException {

    public ErpNextUnavailableException(String context, int status, String erpNextMessage, Throwable cause) {
        super(context, status, erpNextMessage, cause);
    }
}
