package hery.itu.erp.erpnext;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extrait un message lisible d'une réponse d'erreur Frappe.
 * Frappe renvoie selon les cas {@code _server_messages} (liste JSON de chaînes JSON),
 * {@code exception} ({@code module.Classe: message}) ou {@code message}.
 */
final class FrappeErrorParser {

    private static final int MAX_LENGTH = 300;

    private FrappeErrorParser() {
    }

    static String extractMessage(String body, ObjectMapper mapper) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = mapper.readTree(body);
            if (!root.isObject()) {
                return abbreviate(stripHtml(body));
            }

            String fromServerMessages = fromServerMessages(root.path("_server_messages"), mapper);
            if (fromServerMessages != null) {
                return fromServerMessages;
            }

            JsonNode exception = root.path("exception");
            if (exception.isTextual() && !exception.asText().isBlank()) {
                String text = exception.asText();
                int sep = text.indexOf(": ");
                return abbreviate(stripHtml(sep > 0 ? text.substring(sep + 2) : text));
            }

            JsonNode message = root.path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                return abbreviate(stripHtml(message.asText()));
            }
            return abbreviate(body);
        } catch (JsonProcessingException e) {
            return abbreviate(stripHtml(body));
        }
    }

    private static String fromServerMessages(JsonNode serverMessages, ObjectMapper mapper) {
        if (!serverMessages.isTextual()) {
            return null;
        }
        try {
            JsonNode list = mapper.readTree(serverMessages.asText());
            List<String> messages = new ArrayList<>();
            for (JsonNode entry : list) {
                JsonNode obj = entry.isTextual() ? mapper.readTree(entry.asText()) : entry;
                JsonNode message = obj.path("message");
                if (message.isTextual() && !message.asText().isBlank()) {
                    messages.add(stripHtml(message.asText()));
                }
            }
            return messages.isEmpty() ? null : abbreviate(String.join(" ; ", messages));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String stripHtml(String text) {
        return text.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
    }

    private static String abbreviate(String text) {
        String single = text.replaceAll("\\s+", " ").trim();
        return single.length() <= MAX_LENGTH ? single : single.substring(0, MAX_LENGTH) + "…";
    }
}
