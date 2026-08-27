package hery.itu.erp.service.salary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;

/**
 * Salary Structure Assignment (SSA) et Salary Slip : création, remplacement et recherche.
 * <p>
 * Règle : un document <b>soumis</b> n'est jamais supprimé. Un remplacement suit le cycle
 * ERPNext natif — annulation puis amendement ({@code amended_from}) — pour conserver
 * l'historique de paie. Seuls les brouillons peuvent être supprimés.
 * <p>
 * La génération sur période (aléa 1) est dans {@link PayrollGenerationService}, la
 * modification groupée (aléa 2) dans {@link BulkSalaryAdjustmentService}.
 */
@Service
public class SalaryStructAssService {

    private static final Logger log = LoggerFactory.getLogger(SalaryStructAssService.class);
    static final String SALARY_STRUCTURE_ASSIGNMENT = "Salary Structure Assignment";
    static final String SALARY_SLIP = "Salary Slip";
    static final String SALARY_DETAIL = "Salary Detail";
    /** docstatus 0 (brouillon) et 1 (soumis) ; 2 = annulé, à ignorer dans les recherches d'existence. */
    static final List<Integer> DOCSTATUS_ACTIF = List.of(0, 1);
    static final int DRAFT = 0;
    static final int SUBMITTED = 1;

    private final ErpNextClient client;

    public SalaryStructAssService(ErpNextClient client) {
        this.client = client;
    }

    /** Un Salary Slip existant et sa période. */
    public record SlipPeriod(String name, String startDate, String endDate, String postingDate, int docstatus) {
    }

    /** Résultat d'une création ou d'un remplacement : le SSA et les slips créés. */
    public record AssignmentAndSlips(String assignment, List<String> slips) {
    }

    // ----------------------------------------------------------------- création

    public String createSalaryStructureAssignmentAndSubmit(SalaryStructAss salaryStructAss) {
        return insertAndSubmitAssignment(salaryStructAss, null);
    }

    private String insertAndSubmitAssignment(SalaryStructAss salaryStructAss, String amendedFrom) {
        Map<String, Object> data = assignmentPayload(salaryStructAss);
        if (amendedFrom != null) {
            data.put("amended_from", amendedFrom);
        }
        String name = client.insert(SALARY_STRUCTURE_ASSIGNMENT, data).path("name").asText();
        if (name.isEmpty()) {
            throw new IllegalStateException("ERPNext n'a pas renvoyé le nom du Salary Structure Assignment créé");
        }
        client.runDocMethod(SALARY_STRUCTURE_ASSIGNMENT, name, "submit");
        log.info("SSA {} créé et soumis pour {} (base {}){}", name, salaryStructAss.getEmployee(),
                salaryStructAss.getBase(), amendedFrom == null ? "" : ", amende " + amendedFrom);
        return name;
    }

    private static Map<String, Object> assignmentPayload(SalaryStructAss salaryStructAss) {
        Map<String, Object> data = new HashMap<>();
        data.put("employee", salaryStructAss.getEmployee());
        data.put("salary_structure", salaryStructAss.getSalary_structure());
        data.put("company", salaryStructAss.getCompany());
        data.put("currency", salaryStructAss.getCurrency());
        data.put("base", salaryStructAss.getBase().toPlainString());
        data.put("from_date", salaryStructAss.getFrom_date());
        if (salaryStructAss.getTo_date() != null) {
            data.put("to_date", salaryStructAss.getTo_date());
        }
        return data;
    }

    /**
     * Crée puis soumet le Salary Slip de la période du SSA. Le nom est celui attribué par ERPNext
     * (série {@code Sal Slip/{employee}/.#####}) : il n'est jamais calculé côté client.
     */
    public String createSalarySlipAndSubmit(SalaryStructAss salaryStructAss) {
        String start = salaryStructAss.getFrom_date();
        String end = salaryStructAss.getTo_date() != null ? salaryStructAss.getTo_date() : start;
        String posting = salaryStructAss.getPosting_date() != null ? salaryStructAss.getPosting_date() : end;
        return createSalarySlipAndSubmit(salaryStructAss, start, end, posting, null);
    }

    private String createSalarySlipAndSubmit(SalaryStructAss salaryStructAss, String startDate, String endDate,
                                             String postingDate, String amendedFrom) {
        Map<String, Object> slipData = new HashMap<>();
        slipData.put("employee", salaryStructAss.getEmployee());
        slipData.put("salary_structure", salaryStructAss.getSalary_structure());
        slipData.put("company", salaryStructAss.getCompany());
        slipData.put("start_date", startDate);
        slipData.put("end_date", endDate);
        slipData.put("posting_date", postingDate);
        slipData.put("payroll_frequency", "Monthly");
        if (amendedFrom != null) {
            slipData.put("amended_from", amendedFrom);
        }

        String slipName = client.insert(SALARY_SLIP, slipData).path("name").asText();
        if (slipName.isEmpty()) {
            throw new IllegalStateException("ERPNext n'a pas renvoyé le nom du Salary Slip créé");
        }
        client.runDocMethod(SALARY_SLIP, slipName, "submit");
        log.info("Salary Slip {} créé et soumis ({} -> {}){}", slipName, startDate, endDate,
                amendedFrom == null ? "" : ", amende " + amendedFrom);
        return slipName;
    }

    /** Crée et soumet un SSA puis le slip de sa période. Sans base, reprend la dernière base soumise de l'employé. */
    public AssignmentAndSlips createAssignmentAndSlip(SalaryStructAss salaryStructAss) {
        if (salaryStructAss.getBase() == null) {
            BigDecimal lastBase = getLastSalaryBase(salaryStructAss.getEmployee())
                    .orElseThrow(() -> new IllegalStateException("Base introuvable et non fournie !"));
            salaryStructAss.setBase(lastBase);
        }
        String assignmentName = createSalaryStructureAssignmentAndSubmit(salaryStructAss);
        String slipName = createSalarySlipAndSubmit(salaryStructAss);
        return new AssignmentAndSlips(assignmentName, List.of(slipName));
    }

    // ---------------------------------------------------------------- recherche

    /** Un slip brouillon ou soumis existe-t-il pour l'employé sur exactement cette période ? (les annulés ne comptent pas) */
    public boolean salarySlipExists(String employee, String startDate, String endDate) {
        return client.list(SALARY_SLIP)
                .fields("name")
                .filters(Filters.where("employee", "=", employee)
                        .eq("start_date", startDate)
                        .eq("end_date", endDate)
                        .in("docstatus", DOCSTATUS_ACTIF))
                .first()
                .isPresent();
    }

    public boolean salaryAssignmentExists(String employeeId, String fromDate) {
        return findAssignmentName(employeeId, fromDate).isPresent();
    }

    /** Nom du SSA (brouillon ou soumis) de <b>cet</b> employé commençant à cette date. */
    public Optional<String> findAssignmentName(String employee, String fromDate) {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name")
                .filters(Filters.where("employee", "=", employee)
                        .eq("from_date", fromDate)
                        .in("docstatus", DOCSTATUS_ACTIF))
                .orderBy("creation desc")
                .first()
                .map(node -> node.path("name").asText());
    }

    /**
     * Slips actifs (brouillon ou soumis) de l'employé dont la période est comprise dans
     * [fromDate, toDate] ({@code toDate} nul = sans borne de fin), par date croissante.
     */
    public List<SlipPeriod> findActiveSlips(String employee, String fromDate, String toDate) {
        Filters filters = Filters.where("employee", "=", employee)
                .gte("start_date", fromDate)
                .in("docstatus", DOCSTATUS_ACTIF);
        if (toDate != null) {
            filters.lte("end_date", toDate);
        }
        return client.list(SALARY_SLIP)
                .fields("name", "start_date", "end_date", "posting_date", "docstatus")
                .filters(filters)
                .orderBy("start_date asc")
                .fetchAll().stream()
                .map(SalaryStructAssService::toSlipPeriod)
                .toList();
    }

    /** Base du dernier SSA soumis de l'employé (par from_date décroissante). */
    public Optional<BigDecimal> getLastSalaryBase(String employee) {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name", "base", "from_date")
                .filters(Filters.where("employee", "=", employee).eq("docstatus", SUBMITTED))
                .orderBy("from_date desc")
                .first()
                .map(node -> node.path("base").decimalValue());
    }

    public SalaryStructAss getAssignmentById(String id) {
        return client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, id, SalaryStructAss.class);
    }

    /** Tous les SSA actifs (brouillons et soumis), du plus récent au plus ancien. */
    public List<SalaryStructAss> getAllSalaryStructureAssignments() {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name", "employee", "employee_name", "salary_structure", "company", "currency",
                        "base", "from_date", "to_date", "docstatus")
                .filters(Filters.where("docstatus", "in", DOCSTATUS_ACTIF))
                .orderBy("from_date desc, employee_name asc")
                .fetchAll().stream()
                .map(node -> client.convert(node, SalaryStructAss.class))
                .toList();
    }

    // ------------------------------------------------------------- workflow doc

    public void cancelSalarySlip(String slipName) {
        client.runDocMethod(SALARY_SLIP, slipName, "cancel");
        log.info("Salary Slip {} annulé", slipName);
    }

    public void cancelSalaryStructureAssignment(String assignmentName) {
        client.runDocMethod(SALARY_STRUCTURE_ASSIGNMENT, assignmentName, "cancel");
        log.info("Salary Structure Assignment {} annulé", assignmentName);
    }

    public void submitAssignment(String name) {
        client.runDocMethod(SALARY_STRUCTURE_ASSIGNMENT, name, "submit");
    }

    public void submitSalarySlip(String slipName) {
        client.runDocMethod(SALARY_SLIP, slipName, "submit");
    }

    /** Retire un slip sans perdre d'historique : annulation s'il est soumis, suppression s'il n'est qu'un brouillon. */
    public void retireSlip(SlipPeriod slip) {
        if (slip.docstatus() == SUBMITTED) {
            cancelSalarySlip(slip.name());
        } else if (slip.docstatus() == DRAFT) {
            client.delete(SALARY_SLIP, slip.name());
            log.info("Salary Slip {} (brouillon) supprimé", slip.name());
        }
    }

    // ------------------------------------------------------------ remplacement

    /**
     * Remplace un SSA existant (nouvelle base, structure…) en régénérant les slips actifs de
     * l'employé sur la période du SSA.
     *
     * @see #replaceAssignment(SalaryStructAss, List)
     */
    public AssignmentAndSlips replaceAssignment(SalaryStructAss updated) {
        JsonNode existing = client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, updated.getName());
        List<SlipPeriod> slips = findActiveSlips(existing.path("employee").asText(),
                existing.path("from_date").asText(), existing.path("to_date").asText(null));
        return replace(updated, existing, slips);
    }

    /**
     * Remplace un SSA existant et régénère les slips indiqués, sans jamais supprimer un document soumis :
     * <ol>
     *   <li>les slips indiqués sont annulés (ou supprimés s'ils sont brouillons) ;</li>
     *   <li>le SSA est annulé s'il était soumis, puis un SSA amendé ({@code amended_from}) est créé et soumis
     *       — un SSA encore brouillon est simplement mis à jour puis soumis ;</li>
     *   <li>chaque slip est recréé sur sa période, amendé depuis l'ancien, et soumis.</li>
     * </ol>
     * En cas d'échec en cours de route rien n'est perdu : les documents annulés restent visibles
     * dans ERPNext et peuvent être amendés à la main.
     *
     * @param updated SSA portant le {@code name} du SSA à remplacer et les valeurs cibles ; les champs
     *                absents (employé, période, structure, société, devise, base) sont repris de l'existant
     */
    public AssignmentAndSlips replaceAssignment(SalaryStructAss updated, List<SlipPeriod> slipsToRegenerate) {
        JsonNode existing = client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, updated.getName());
        return replace(updated, existing, slipsToRegenerate);
    }

    private AssignmentAndSlips replace(SalaryStructAss updated, JsonNode existing, List<SlipPeriod> slipsToRegenerate) {
        String oldName = existing.path("name").asText();
        int docstatus = existing.path("docstatus").asInt();
        completeFromExisting(updated, existing);

        for (SlipPeriod slip : slipsToRegenerate) {
            retireSlip(slip);
        }

        String newName;
        if (docstatus == DRAFT) {
            client.update(SALARY_STRUCTURE_ASSIGNMENT, oldName, assignmentPayload(updated));
            client.runDocMethod(SALARY_STRUCTURE_ASSIGNMENT, oldName, "submit");
            newName = oldName;
            log.info("SSA {} (brouillon) mis à jour et soumis", oldName);
        } else {
            if (docstatus == SUBMITTED) {
                cancelSalaryStructureAssignment(oldName);
            }
            newName = insertAndSubmitAssignment(updated, oldName);
        }

        List<String> newSlips = new ArrayList<>();
        for (SlipPeriod slip : slipsToRegenerate) {
            String amendedFrom = slip.docstatus() == SUBMITTED ? slip.name() : null;
            newSlips.add(createSalarySlipAndSubmit(updated, slip.startDate(), slip.endDate(), slip.endDate(), amendedFrom));
        }
        return new AssignmentAndSlips(newName, newSlips);
    }

    private static void completeFromExisting(SalaryStructAss updated, JsonNode existing) {
        updated.setEmployee(existing.path("employee").asText());
        if (updated.getFrom_date() == null) updated.setFrom_date(existing.path("from_date").asText(null));
        if (updated.getTo_date() == null) updated.setTo_date(existing.path("to_date").asText(null));
        if (updated.getSalary_structure() == null) updated.setSalary_structure(existing.path("salary_structure").asText(null));
        if (updated.getCompany() == null) updated.setCompany(existing.path("company").asText(null));
        if (updated.getCurrency() == null) updated.setCurrency(existing.path("currency").asText(null));
        if (updated.getBase() == null) updated.setBase(existing.path("base").decimalValue());
    }

    /** Retire un SSA : annulation s'il est soumis, suppression s'il n'est qu'un brouillon. */
    public void deleteAssignment(String assignmentName) {
        int docstatus = client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, assignmentName).path("docstatus").asInt();
        if (docstatus == SUBMITTED) {
            cancelSalaryStructureAssignment(assignmentName);
        } else if (docstatus == DRAFT) {
            client.delete(SALARY_STRUCTURE_ASSIGNMENT, assignmentName);
            log.info("Salary Structure Assignment {} (brouillon) supprimé", assignmentName);
        }
    }

    // --------------------------------------------------- composants (aléa 2)

    /**
     * Lignes d'un Salary Component sur les slips actifs d'un employé, filtrées par condition
     * ({@code inf} : montant &lt; seuil, sinon montant &gt; seuil).
     */
    public List<SalaryFilterDTO> getSalaryComponentValues(String employee, String salaryComponent,
                                                          String condition, double montant) {
        Map<String, SlipPeriod> periods = new LinkedHashMap<>();
        client.list(SALARY_SLIP)
                .fields("name", "start_date", "end_date", "posting_date", "docstatus")
                .filters(Filters.where("employee", "=", employee).in("docstatus", DOCSTATUS_ACTIF))
                .orderBy("start_date asc")
                .fetchAll()
                .forEach(node -> periods.put(node.path("name").asText(), toSlipPeriod(node)));
        if (periods.isEmpty()) {
            return List.of();
        }

        List<SalaryFilterDTO> matchingResults = new ArrayList<>();
        for (JsonNode line : componentLines(periods.keySet(), salaryComponent)) {
            SlipPeriod period = periods.get(line.path("parent").asText());
            String component = line.path("salary_component").asText();
            if (period == null || !salaryComponent.equalsIgnoreCase(component)) {
                continue;
            }
            double amount = line.path("amount").asDouble();
            boolean matches = "inf".equals(condition) ? amount < montant : amount > montant;
            if (matches) {
                matchingResults.add(new SalaryFilterDTO(period.name(), employee, component, amount,
                        period.postingDate(), period.startDate(), period.endDate(), period.docstatus()));
            }
        }
        return matchingResults;
    }

    /**
     * Lignes {@code Salary Detail} du composant pour les fiches données : une requête par lot de fiches,
     * avec repli sur un GET par fiche si l'instance refuse la lecture directe de la table enfant.
     */
    private List<JsonNode> componentLines(Collection<String> slipNames, String salaryComponent) {
        try {
            return client.listChildRows(SALARY_DETAIL, SALARY_SLIP, List.of("parent", "salary_component", "amount"),
                    slipNames, Filters.where("salary_component", "=", salaryComponent), "parent asc, idx asc");
        } catch (ErpNextValidationException | ErpNextForbiddenException e) {
            log.warn("Lecture groupée de Salary Detail refusée ({}) : repli sur un appel par fiche", e.getErpNextMessage());
            List<JsonNode> lines = new ArrayList<>();
            for (String slipName : slipNames) {
                JsonNode details = client.getDoc(SALARY_SLIP, slipName);
                for (String table : List.of("earnings", "deductions")) {
                    for (JsonNode line : details.path(table)) {
                        ((ObjectNode) line).put("parent", slipName);
                        lines.add(line);
                    }
                }
            }
            return lines;
        }
    }

    /** Moyenne (2 décimales) des bases de tous les SSA soumis ; vide s'il n'y en a aucun. */
    public Optional<BigDecimal> getMoyenneTotalBaseOfAllEmployees() {
        List<JsonNode> rows = client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("base")
                .filters(Filters.where("docstatus", "=", SUBMITTED))
                .fetchAll();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode node : rows) {
            total = total.add(node.path("base").decimalValue());
        }
        return Optional.of(total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP));
    }

    /** Nom du SSA actif pour l'employé à la date du slip (le plus récent dont from_date &lt;= posting_date). */
    public String getSalaryStructureAssignmentByEmployeeAndDate(SalaryFilterDTO salaryFilterDTO) {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name", "from_date")
                .filters(Filters.where("employee", "=", salaryFilterDTO.getEmployee())
                        .lte("from_date", salaryFilterDTO.getPostingDate())
                        .in("docstatus", DOCSTATUS_ACTIF))
                .orderBy("from_date desc, creation desc")
                .first()
                .map(node -> node.path("name").asText())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun Salary Structure Assignment actif pour " + salaryFilterDTO.getEmployee()
                        + " au " + salaryFilterDTO.getPostingDate()));
    }

    private static SlipPeriod toSlipPeriod(JsonNode node) {
        return new SlipPeriod(
                node.path("name").asText(),
                node.path("start_date").asText(null),
                node.path("end_date").asText(null),
                node.path("posting_date").asText(null),
                node.path("docstatus").asInt(SUBMITTED));
    }
}
