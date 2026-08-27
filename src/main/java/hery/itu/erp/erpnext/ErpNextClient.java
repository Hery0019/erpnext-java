package hery.itu.erp.erpnext;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import hery.itu.erp.config.ErpNextProperties;
import hery.itu.erp.service.login.LoginService;

/**
 * Point d'accès unique à l'API REST de Frappe/ERPNext.
 * <ul>
 *   <li>URL de base, timeouts et taille de page viennent de {@link ErpNextProperties} ;</li>
 *   <li>le cookie de session est celui de l'utilisateur courant ;</li>
 *   <li>doctypes, noms de documents et filtres sont encodés — jamais concaténés ;</li>
 *   <li>toute réponse non 2xx devient une {@link ErpNextException} typée avec le message Frappe.</li>
 * </ul>
 */
@Component
public class ErpNextClient {

    private static final Logger log = LoggerFactory.getLogger(ErpNextClient.class);
    /** Garde-fou contre une pagination infinie. */
    private static final int MAX_PAGES = 200;

    private final RestTemplate restTemplate;
    private final ErpNextProperties properties;
    private final LoginService loginService;
    private final ObjectMapper objectMapper;

    public ErpNextClient(RestTemplateBuilder builder, ErpNextProperties properties,
                         LoginService loginService, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .errorHandler(new PassThroughErrorHandler())
                .build();
        this.properties = properties;
        this.loginService = loginService;
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // ------------------------------------------------------------------ lecture

    /** Requête de liste sur un DocType ({@code GET /api/resource/{doctype}}). */
    public ListQuery list(String doctype) {
        return new ListQuery(doctype);
    }

    /** Document complet ({@code data} de {@code GET /api/resource/{doctype}/{name}}). */
    public JsonNode getDoc(String doctype, String name) {
        return exchange(HttpMethod.GET, resourceUri(doctype, name, Map.of()), null, null).path("data");
    }

    public <T> T getDoc(String doctype, String name, Class<T> type) {
        return convert(getDoc(doctype, name), type);
    }

    public Optional<JsonNode> findDoc(String doctype, String name) {
        try {
            return Optional.of(getDoc(doctype, name));
        } catch (ErpNextNotFoundException e) {
            return Optional.empty();
        }
    }

    // ----------------------------------------------------------------- écriture

    /** Création ({@code POST /api/resource/{doctype}}) ; renvoie le document créé. */
    public JsonNode insert(String doctype, Object document) {
        return exchange(HttpMethod.POST, resourceUri(doctype, null, Map.of()), document, MediaType.APPLICATION_JSON).path("data");
    }

    /** Mise à jour partielle ({@code PUT /api/resource/{doctype}/{name}}) ; renvoie le document mis à jour. */
    public JsonNode update(String doctype, String name, Object changes) {
        return exchange(HttpMethod.PUT, resourceUri(doctype, name, Map.of()), changes, MediaType.APPLICATION_JSON).path("data");
    }

    public void delete(String doctype, String name) {
        exchange(HttpMethod.DELETE, resourceUri(doctype, name, Map.of()), null, null);
    }

    /** Méthode de document Frappe : {@code submit}, {@code cancel}, {@code amend}… */
    public JsonNode runDocMethod(String doctype, String name, String method) {
        return exchange(HttpMethod.POST, resourceUri(doctype, name, Map.of("run_method", method)), null, null);
    }

    /** Méthode whitelistée ({@code POST /api/method/{method}}) avec corps JSON ; renvoie la réponse complète. */
    public JsonNode callMethod(String method, Object body) {
        return exchange(HttpMethod.POST, methodUri(method), body, MediaType.APPLICATION_JSON);
    }

    /** Méthode whitelistée avec corps {@code application/x-www-form-urlencoded}. */
    public JsonNode callMethodForm(String method, MultiValueMap<String, String> form) {
        return exchange(HttpMethod.POST, methodUri(method), form, MediaType.APPLICATION_FORM_URLENCODED);
    }

    // --------------------------------------------------------------- conversion

    public <T> T convert(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new ErpNextException("conversion en " + type.getSimpleName(), 0, e.getOriginalMessage(), e);
        }
    }

    public <T> T convert(JsonNode node, TypeReference<T> type) {
        try {
            return objectMapper.readValue(objectMapper.treeAsTokens(node), type);
        } catch (IOException e) {
            throw new ErpNextException("conversion en " + type.getType(), 0, e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------- interne

    private JsonNode exchange(HttpMethod method, URI uri, Object body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, loginService.getSessionCookie());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        String context = method + " " + uri.getPath();
        log.debug("ERPNext {}{}", context, uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), String.class);
        } catch (ResourceAccessException e) {
            throw new ErpNextUnavailableException(context, 0, e.getMessage(), e);
        }

        HttpStatusCode status = response.getStatusCode();
        if (!status.is2xxSuccessful()) {
            throw translate(context, status.value(), response.getBody());
        }
        return parse(response.getBody());
    }

    private ErpNextException translate(String context, int status, String body) {
        String message = FrappeErrorParser.extractMessage(body, objectMapper);
        log.warn("ERPNext {} -> {} {}", context, status, message);
        return switch (status) {
            case 404 -> new ErpNextNotFoundException(context, message);
            case 401, 403 -> new ErpNextForbiddenException(context, status, message);
            case 400, 409, 417, 422 -> new ErpNextValidationException(context, status, message);
            default -> status >= 500
                    ? new ErpNextUnavailableException(context, status, message, null)
                    : new ErpNextException(context, status, message);
        };
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ErpNextException("réponse non JSON", 0, FrappeErrorParser.extractMessage(body, objectMapper), e);
        }
    }

    private URI resourceUri(String doctype, String name, Map<String, String> query) {
        StringBuilder path = new StringBuilder("/api/resource/")
                .append(UriUtils.encodePathSegment(doctype, StandardCharsets.UTF_8));
        if (name != null) {
            // Les noms Frappe peuvent contenir des "/" (ex. "Sal Slip/HR-EMP-00001/00001") : on encode
            // chaque segment mais on conserve les "/" — c'est ainsi que Frappe route <path:name>.
            path.append('/').append(UriUtils.encodePath(name, StandardCharsets.UTF_8));
        }
        return buildUri(path.toString(), query);
    }

    private URI methodUri(String method) {
        return buildUri("/api/method/" + UriUtils.encodePathSegment(method, StandardCharsets.UTF_8), Map.of());
    }

    private URI buildUri(String encodedPath, Map<String, String> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl()).path(encodedPath);
        query.forEach((key, value) -> builder.queryParam(key, UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)));
        return builder.build(true).toUri();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Impossible de sérialiser " + value, e);
        }
    }

    /** Les codes d'erreur sont traduits par {@link #translate} ; RestTemplate ne doit pas lever lui-même. */
    private static final class PassThroughErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) {
            // jamais appelé : hasError() renvoie false
        }
    }

    // --------------------------------------------------------------- ListQuery

    /**
     * Construction fluide d'une requête de liste. {@link #fetch()} renvoie une page,
     * {@link #fetchAll()} parcourt toutes les pages, {@link #first()} la première ligne.
     */
    public final class ListQuery {
        private final String doctype;
        private List<String> fields = List.of("name");
        private Filters filters = Filters.none();
        private String orderBy;
        private String parent;
        private int limitStart = 0;
        private Integer limitPageLength;

        private ListQuery(String doctype) {
            this.doctype = doctype;
        }

        public ListQuery fields(String... fields) {
            this.fields = List.of(fields);
            return this;
        }

        public ListQuery fields(List<String> fields) {
            this.fields = List.copyOf(fields);
            return this;
        }

        public ListQuery filters(Filters filters) {
            this.filters = filters;
            return this;
        }

        /** Ex. {@code "creation desc"}. */
        public ListQuery orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /** DocType parent, obligatoire pour lister une table enfant (ex. "Salary Slip" pour "Salary Detail"). */
        public ListQuery parent(String parentDoctype) {
            this.parent = parentDoctype;
            return this;
        }

        public ListQuery limit(int limit) {
            this.limitPageLength = limit;
            return this;
        }

        public ListQuery offset(int offset) {
            this.limitStart = offset;
            return this;
        }

        /** Une seule page (taille : {@link #limit(int)} ou {@code erpnext.page-size}). */
        public List<JsonNode> fetch() {
            return page(limitStart, limitPageLength == null ? properties.pageSize() : limitPageLength);
        }

        public Optional<JsonNode> first() {
            List<JsonNode> rows = page(limitStart, 1);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        }

        /** Toutes les lignes, en parcourant les pages de {@code erpnext.page-size}. */
        public List<JsonNode> fetchAll() {
            int pageSize = limitPageLength == null ? properties.pageSize() : limitPageLength;
            List<JsonNode> all = new ArrayList<>();
            int start = limitStart;
            for (int pageNo = 0; pageNo < MAX_PAGES; pageNo++) {
                List<JsonNode> page = page(start, pageSize);
                all.addAll(page);
                if (page.size() < pageSize) {
                    return all;
                }
                start += pageSize;
            }
            log.warn("Liste {} tronquée après {} pages de {} lignes", doctype, MAX_PAGES, pageSize);
            return all;
        }

        private List<JsonNode> page(int start, int length) {
            Map<String, String> query = new LinkedHashMap<>();
            query.put("fields", toJson(fields));
            if (!filters.isEmpty()) {
                query.put("filters", toJson(filters.asList()));
            }
            if (orderBy != null) {
                query.put("order_by", orderBy);
            }
            if (parent != null) {
                query.put("parent", parent);
            }
            query.put("limit_start", String.valueOf(start));
            query.put("limit_page_length", String.valueOf(length));

            JsonNode data = exchange(HttpMethod.GET, resourceUri(doctype, null, query), null, null).path("data");
            List<JsonNode> rows = new ArrayList<>();
            data.forEach(rows::add);
            return rows;
        }
    }

    /** Utilitaire : liste de champs à partir de varargs, pour les constantes de service. */
    public static List<String> fields(String... names) {
        return Arrays.asList(names);
    }
}
