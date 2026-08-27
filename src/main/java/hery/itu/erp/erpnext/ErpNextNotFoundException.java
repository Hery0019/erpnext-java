package hery.itu.erp.erpnext;

/** Document ou ressource inexistant (HTTP 404). */
public class ErpNextNotFoundException extends ErpNextException {

    public ErpNextNotFoundException(String context, String erpNextMessage) {
        super(context, 404, erpNextMessage);
    }
}
