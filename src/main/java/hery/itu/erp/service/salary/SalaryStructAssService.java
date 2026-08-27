package hery.itu.erp.service.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;

/**
 * Salary Structure Assignment (SSA) et Salary Slip : création/soumission, génération sur une
 * période (aléa 1), modification groupée de la base (aléa 2).
 * <p>
 * Les corrections restantes de la revue (points 1.3, 1.4, 1.5, 3.1) font l'objet de commits séparés.
 */
@Service
public class SalaryStructAssService {

    private static final Logger log = LoggerFactory.getLogger(SalaryStructAssService.class);
    static final String SALARY_STRUCTURE_ASSIGNMENT = "Salary Structure Assignment";
    static final String SALARY_SLIP = "Salary Slip";
    /** docstatus 0 (brouillon) et 1 (soumis) ; 2 = annulé, à ignorer dans les recherches d'existence. */
    static final List<Integer> DOCSTATUS_ACTIF = List.of(0, 1);

    private final ErpNextClient client;

    public SalaryStructAssService(ErpNextClient client) {
        this.client = client;
    }

    // ----------------------------------------------------------------- création

    public String createSalaryStructureAssignmentAndSubmit(SalaryStructAss salaryStructAss) {
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

        String name = client.insert(SALARY_STRUCTURE_ASSIGNMENT, data).path("name").asText();
        if (name.isEmpty()) {
            throw new IllegalStateException("Impossible de récupérer le name de l'Assignment");
        }
        client.runDocMethod(SALARY_STRUCTURE_ASSIGNMENT, name, "submit");
        return name;
    }

    /**
     * Crée puis soumet le Salary Slip de la période du SSA. Le nom est celui attribué par ERPNext
     * (série {@code Sal Slip/{employee}/.#####}) : il n'est jamais calculé côté client.
     */
    public String createSalarySlipAndSubmit(SalaryStructAss salaryStructAss) {
        Map<String, Object> slipData = new HashMap<>();
        slipData.put("employee", salaryStructAss.getEmployee());
        slipData.put("salary_structure", salaryStructAss.getSalary_structure());
        slipData.put("company", salaryStructAss.getCompany());
        slipData.put("start_date", salaryStructAss.getFrom_date());
        slipData.put("end_date", salaryStructAss.getTo_date() != null ? salaryStructAss.getTo_date() : salaryStructAss.getFrom_date());
        slipData.put("posting_date", salaryStructAss.getPosting_date());
        slipData.put("payroll_frequency", "Monthly");

        String slipName = client.insert(SALARY_SLIP, slipData).path("name").asText();
        if (slipName.isEmpty()) {
            throw new IllegalStateException("ERPNext n'a pas renvoyé le nom du Salary Slip créé");
        }
        client.runDocMethod(SALARY_SLIP, slipName, "submit");
        return slipName;
    }

    public Map<String, Object> createAssignmentAndSlip(SalaryStructAss salaryStructAss) {
        if (salaryStructAss.getBase() == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase == null) {
                throw new IllegalStateException("Base introuvable et non fournie !");
            }
            salaryStructAss.setBase(BigDecimal.valueOf(lastBase));
        }

        String assignmentName = createSalaryStructureAssignmentAndSubmit(salaryStructAss);
        String slipName = createSalarySlipAndSubmit(salaryStructAss);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", assignmentName);
        result.put("slip", slipName);
        return result;
    }

    // ---------------------------------------------------------------- existence

    /** Un slip brouillon ou soumis existe-t-il pour l'employé sur exactement cette période ? (les annulés ne comptent pas) */
    private boolean salarySlipExists(String employee, String startDate, String endDate) {
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

    // --------------------------------------------------------- génération (aléa 1)

    public List<String> generateSalary(SalaryStructAss salaryStructAss, LocalDate startDate, LocalDate endDate,
                                       String ecraser, String moyenne) {
        List<String> generatedSlips = new ArrayList<>();
        String employee = salaryStructAss.getEmployee();
        BigDecimal base = salaryStructAss.getBase();
        boolean ecraserOui = "oui".equalsIgnoreCase(ecraser);

        if (base == null && "oui".equalsIgnoreCase(moyenne)) {
            base = getMoyenneTotalBaseOfAllEmployees();
            log.info("Génération pour {} : base moyenne utilisée", employee);
        }
        if (base == null) {
            Double lastBase = getLastSalaryBase(employee);
            if (lastBase == null) {
                throw new IllegalStateException("Base introuvable pour l'employé : " + employee);
            }
            base = BigDecimal.valueOf(lastBase);
            log.info("Génération pour {} : base reprise de la dernière assignation", employee);
        }

        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate limit = endDate.withDayOfMonth(1);

        while (!current.isAfter(limit)) {
            LocalDate slipStart = current;
            LocalDate slipEnd = current.with(TemporalAdjusters.lastDayOfMonth());

            boolean slipExists = salarySlipExists(employee, slipStart.toString(), slipEnd.toString());
            Optional<String> existingAssignment = findAssignmentName(employee, slipStart.toString());
            boolean assignmentExists = existingAssignment.isPresent();

            SalaryStructAss slipAss = new SalaryStructAss();
            slipAss.setEmployee(employee);
            slipAss.setSalary_structure(salaryStructAss.getSalary_structure());
            slipAss.setCompany(salaryStructAss.getCompany());
            slipAss.setCurrency(salaryStructAss.getCurrency());
            slipAss.setBase(base);
            slipAss.setFrom_date(slipStart.toString());
            slipAss.setTo_date(slipEnd.toString());
            slipAss.setPosting_date(slipEnd.toString());

            try {
                if (slipExists && !ecraserOui) {
                    log.info("Slip déjà existant pour {} {} -> ignoré", employee, slipStart);
                } else if (assignmentExists && ecraserOui) {
                    // On remplace le SSA de CET employé pour CE mois — jamais celui d'un autre employé (point 1.2).
                    slipAss.setName(existingAssignment.get());
                    Map<String, Object> result = updateAssignment(slipAss);
                    generatedSlips.add(result.get("slip").toString());
                    log.info("Slip mis à jour pour {} {}", employee, slipStart);
                } else if (!assignmentExists) {
                    Map<String, Object> result = createAssignmentAndSlip(slipAss);
                    generatedSlips.add(result.get("slip").toString());
                    log.info("Slip créé pour {} {}", employee, slipStart);
                } else {
                    log.info("Assignment déjà existant et écrasement non autorisé -> ignoré pour {} {}", employee, slipStart);
                }
            } catch (RuntimeException e) {
                log.error("Erreur lors de la génération du slip pour {} {} : {}", employee, slipStart, e.getMessage());
            }

            current = current.plusMonths(1);
        }

        return generatedSlips;
    }

    private Double getLastSalaryBase(String employee) {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name", "base", "from_date")
                .filters(Filters.where("employee", "=", employee).eq("docstatus", 1))
                .orderBy("from_date desc")
                .first()
                .map(node -> node.path("base").asDouble())
                .orElse(null);
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

    // ------------------------------------------------------------------- CRUD

    public List<SalaryStructAss> getAllSalaryStructureAssignments() {
        // NB : interroge Salary Slip et non Salary Structure Assignment (point 1.4, corrigé séparément)
        List<JsonNode> rows = client.list(SALARY_SLIP)
                .fields("name", "employee", "posting_date", "salary_structure_assignment", "earnings", "deductions")
                .fetchAll();
        List<SalaryStructAss> assignments = new ArrayList<>();
        for (JsonNode row : rows) {
            SalaryStructAss ass = new SalaryStructAss();
            ass.setName(row.path("name").asText());
            ass.setEmployee(row.path("employee_name").asText());
            ass.setSalary_structure(row.path("salary_structure").asText());
            ass.setCompany(row.path("company").asText());
            ass.setCurrency(row.path("currency").asText());
            ass.setBase(row.path("base").decimalValue());
            ass.setFrom_date(row.path("from_date").asText());
            assignments.add(ass);
        }
        return assignments;
    }

    public SalaryStructAss getAssignmentById(String id) {
        return client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, id, SalaryStructAss.class);
    }

    /**
     * Remplace un SSA : annule/supprime le SSA et un slip lié, puis recrée SSA + slip.
     * Séquence non transactionnelle (point 3.1, corrigé séparément).
     */
    public Map<String, Object> updateAssignment(SalaryStructAss updatedAss) {
        JsonNode existing = client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, updatedAss.getName());
        int docstatus = existing.path("docstatus").asInt();

        Optional<String> slipName = client.list(SALARY_SLIP)
                .fields("name")
                .filters(Filters.where("salary_structure", "=", updatedAss.getSalary_structure())
                        .eq("employee", updatedAss.getEmployee()))
                .first()
                .map(node -> node.path("name").asText());

        if (docstatus == 1) {
            cancelSalaryStructureAssignment(updatedAss.getName());
        }
        if (slipName.isPresent()) {
            cancelSalarySlip(slipName.get());
            client.delete(SALARY_SLIP, slipName.get());
            log.info("Salary Slip {} annulé et supprimé", slipName.get());
        }
        deleteAssignment(updatedAss.getName());

        Map<String, Object> result = createAssignmentAndSlip(updatedAss);
        log.info("Nouveau SSA et Salary Slip créés : {}", result);
        return result;
    }

    public void deleteAssignment(String assignmentName) {
        JsonNode existing = client.getDoc(SALARY_STRUCTURE_ASSIGNMENT, assignmentName);
        if (existing.path("docstatus").asInt() == 1) {
            cancelSalaryStructureAssignment(assignmentName);
        }
        client.delete(SALARY_STRUCTURE_ASSIGNMENT, assignmentName);
        log.info("Salary Structure Assignment {} supprimé", assignmentName);
    }

    // ---------------------------------------------------- modification (aléa 2)

    /**
     * Valeurs d'un Salary Component sur les slips d'un employé, filtrées par condition
     * ({@code inf} : montant &lt; seuil, sinon montant &gt; seuil).
     */
    public List<SalaryFilterDTO> getSalaryComponentValues(String employee, String salaryComponent,
                                                          String condition, double montant) {
        List<JsonNode> slips = client.list(SALARY_SLIP)
                .fields("name", "employee", "posting_date")
                .filters(Filters.where("employee", "=", employee))
                .fetchAll();
        log.debug("getSalaryComponentValues {} / {} : {} slips", employee, salaryComponent, slips.size());

        List<SalaryFilterDTO> matchingResults = new ArrayList<>();
        for (JsonNode slip : slips) {
            String slipName = slip.path("name").asText();
            JsonNode details = client.getDoc(SALARY_SLIP, slipName);
            String postingDate = details.path("posting_date").asText();

            for (String table : List.of("earnings", "deductions")) {
                for (JsonNode line : details.path(table)) {
                    String component = line.path("salary_component").asText();
                    double amount = line.path("amount").asDouble();
                    if (!salaryComponent.equalsIgnoreCase(component)) {
                        continue;
                    }
                    boolean matches = "inf".equals(condition) ? amount < montant : amount > montant;
                    if (matches) {
                        matchingResults.add(new SalaryFilterDTO(slipName, employee, component, amount, postingDate));
                    }
                }
            }
        }
        return matchingResults;
    }

    public BigDecimal getMoyenneTotalBaseOfAllEmployees() {
        List<JsonNode> rows = client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("base")
                .filters(Filters.where("docstatus", "=", 1))
                .fetchAll();

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (JsonNode node : rows) {
            total = total.add(node.path("base").decimalValue());
            count++;
        }
        // NB : division sans échelle ni garde sur count == 0 (point 1.5, corrigé séparément)
        return total.divide(new BigDecimal(count));
    }

    /** Nom du SSA actif pour l'employé à la date du slip (le plus récent dont from_date &lt;= posting_date). */
    public String getSalaryStructureAssignmentByEmployeeAndDate(SalaryFilterDTO salaryFilterDTO) {
        return client.list(SALARY_STRUCTURE_ASSIGNMENT)
                .fields("name", "from_date")
                .filters(Filters.where("employee", "=", salaryFilterDTO.getEmployee())
                        .lte("from_date", salaryFilterDTO.getPostingDate()))
                .orderBy("from_date desc")
                .first()
                .map(node -> node.path("name").asText())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun Salary Structure Assignment trouvé pour l'employé à cette date."));
    }

    /**
     * Applique un pourcentage sur la base des SSA des employés dont un composant de salaire
     * remplit la condition.
     *
     * @param condition {@code inf} ou {@code sup}
     * @param then      {@code ajouter} ou {@code retirer}
     */
    public void applyModification(List<String> employees, String salaryComponent, String condition,
                                  String then, double montant, double pourcentage) {
        log.info("Modification groupée : composant={}, condition={}, action={}, seuil={}, pourcentage={}%, employés={}",
                salaryComponent, condition, then, montant, pourcentage, employees.size());

        for (String employee : employees) {
            List<SalaryFilterDTO> matchingResults = getSalaryComponentValues(employee, salaryComponent, condition, montant);
            log.debug("Employé {} : {} slips concernés", employee, matchingResults.size());

            for (SalaryFilterDTO result : matchingResults) {
                String assignmentName = getSalaryStructureAssignmentByEmployeeAndDate(result);
                SalaryStructAss salaryStructAss = getAssignmentById(assignmentName);

                BigDecimal base = salaryStructAss.getBase();
                if (base == null) {
                    log.warn("Employé {} : base nulle sur {}, ignoré", employee, assignmentName);
                    continue;
                }

                BigDecimal percentage = BigDecimal.valueOf(pourcentage).divide(BigDecimal.valueOf(100));
                BigDecimal adjustment;
                if ("retirer".equals(then)) {
                    adjustment = base.subtract(base.multiply(percentage));
                } else if ("ajouter".equals(then)) {
                    adjustment = base.add(base.multiply(percentage));
                } else {
                    log.warn("Action inconnue '{}', ignorée", then);
                    continue;
                }

                salaryStructAss.setBase(adjustment);
                updateAssignment(salaryStructAss);
                log.info("Employé {} : SSA {} mis à jour", employee, assignmentName);
            }
        }
    }
}
