package hery.itu.erp.erpnext;

/**
 * Erreur renvoyée par ERPNext ou survenue en lui parlant.
 * Les sous-classes distinguent les cas que l'application doit traiter différemment.
 */
public class ErpNextException extends RuntimeException {

    private final int status;
    private final String erpNextMessage;

    public ErpNextException(String context, int status, String erpNextMessage, Throwable cause) {
        super(buildMessage(context, status, erpNextMessage), cause);
        this.status = status;
        this.erpNextMessage = erpNextMessage == null ? "" : erpNextMessage;
    }

    public ErpNextException(String context, int status, String erpNextMessage) {
        this(context, status, erpNextMessage, null);
    }

    /** Code HTTP renvoyé par ERPNext, ou 0 si la requête n'a pas abouti. */
    public int getStatus() {
        return status;
    }

    /** Message métier extrait de la réponse Frappe (peut être vide). */
    public String getErpNextMessage() {
        return erpNextMessage;
    }

    private static String buildMessage(String context, int status, String erpNextMessage) {
        StringBuilder sb = new StringBuilder("ERPNext");
        if (status > 0) {
            sb.append(" [").append(status).append(']');
        }
        if (context != null && !context.isBlank()) {
            sb.append(' ').append(context);
        }
        if (erpNextMessage != null && !erpNextMessage.isBlank()) {
            sb.append(" : ").append(erpNextMessage);
        }
        return sb.toString();
    }
}
