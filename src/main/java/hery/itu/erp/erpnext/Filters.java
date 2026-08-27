package hery.itu.erp.erpnext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Filtres de liste Frappe, au format {@code [[champ, opérateur, valeur], ...]}.
 * Les valeurs sont sérialisées en JSON par Jackson : aucune concaténation de chaîne,
 * donc aucune possibilité d'injection dans le filtre.
 */
public final class Filters {

    private final List<List<Object>> clauses = new ArrayList<>();

    private Filters() {
    }

    public static Filters none() {
        return new Filters();
    }

    public static Filters where(String field, String operator, Object value) {
        return new Filters().and(field, operator, value);
    }

    public Filters and(String field, String operator, Object value) {
        clauses.add(Collections.unmodifiableList(Arrays.asList(field, operator, value)));
        return this;
    }

    public Filters eq(String field, Object value) {
        return and(field, "=", value);
    }

    public Filters ne(String field, Object value) {
        return and(field, "!=", value);
    }

    public Filters gt(String field, Object value) {
        return and(field, ">", value);
    }

    public Filters gte(String field, Object value) {
        return and(field, ">=", value);
    }

    public Filters lt(String field, Object value) {
        return and(field, "<", value);
    }

    public Filters lte(String field, Object value) {
        return and(field, "<=", value);
    }

    /** {@code like} Frappe : le motif doit contenir les {@code %} voulus. */
    public Filters like(String field, String pattern) {
        return and(field, "like", pattern);
    }

    public Filters in(String field, Collection<?> values) {
        return and(field, "in", List.copyOf(values));
    }

    public Filters between(String field, Object from, Object to) {
        return and(field, "between", List.of(from, to));
    }

    public boolean isEmpty() {
        return clauses.isEmpty();
    }

    /** Représentation sérialisable en JSON. */
    public List<List<Object>> asList() {
        return Collections.unmodifiableList(clauses);
    }

    @Override
    public String toString() {
        return clauses.toString();
    }
}
