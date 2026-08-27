package hery.itu.erp.erpnext;

/** Session ERPNext expirée ou droits insuffisants (HTTP 401 / 403). */
public class ErpNextForbiddenException extends ErpNextException {

    public ErpNextForbiddenException(String context, int status, String erpNextMessage) {
        super(context, status, erpNextMessage);
    }
}
