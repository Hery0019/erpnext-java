package hery.itu.erp.service.salary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.SalaryStructAssService.AssignmentAndSlips;
import hery.itu.erp.service.salary.SalaryStructAssService.SlipPeriod;

/**
 * Aléa 2 — modification groupée de la base salariale.
 * <p>
 * Pour chaque employé sélectionné, on cherche les fiches de paie dont un composant remplit la
 * condition (montant &lt; ou &gt; seuil). Les fiches sont regroupées par assignation (SSA) : le
 * pourcentage est appliqué <b>une seule fois</b> par SSA, quel que soit le nombre de fiches
 * concernées, puis le SSA est remplacé (annulation + amendement) et <b>ces</b> fiches — et
 * seulement elles — sont régénérées. Les échecs sont rapportés par employé, jamais avalés.
 */
@Service
public class BulkSalaryAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(BulkSalaryAdjustmentService.class);
    private static final int BASE_SCALE = 2;

    private final SalaryStructAssService assignments;

    public BulkSalaryAdjustmentService(SalaryStructAssService assignments) {
        this.assignments = assignments;
    }

    /** Un SSA ajusté : ancienne et nouvelle base, fiches régénérées. */
    public record Adjusted(String employee, String assignment, BigDecimal oldBase, BigDecimal newBase, List<String> slips) {
    }

    public record AdjustmentResult(List<Adjusted> adjusted, List<String> failed) {
        public boolean hasFailures() {
            return !failed.isEmpty();
        }
    }

    /**
     * @param condition   {@code inf} (montant &lt; seuil) ou {@code sup} (montant &gt; seuil)
     * @param action      {@code ajouter} ou {@code retirer}
     * @param threshold   seuil comparé au montant du composant
     * @param percentage  pourcentage appliqué à la base (0..100 attendu)
     */
    public AdjustmentResult apply(List<String> employees, String salaryComponent, String condition,
                                  String action, double threshold, double percentage) {
        if (employees == null || employees.isEmpty()) {
            throw new IllegalArgumentException("Sélectionnez au moins un employé.");
        }
        if (salaryComponent == null || salaryComponent.isBlank()) {
            throw new IllegalArgumentException("Choisissez un composant de salaire.");
        }
        if (!"inf".equals(condition) && !"sup".equals(condition)) {
            throw new IllegalArgumentException("Condition inconnue : " + condition + " (attendu : inf ou sup)");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Le seuil doit être positif ou nul.");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Le pourcentage doit être compris entre 0 et 100.");
        }
        BigDecimal multiplier = multiplier(action, percentage);
        log.info("Modification groupée : composant={}, condition={}, action={}, seuil={}, {}%, {} employé(s)",
                salaryComponent, condition, action, threshold, percentage, employees.size());

        List<Adjusted> adjusted = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String employee : employees) {
            try {
                List<SalaryFilterDTO> matches = assignments.getSalaryComponentValues(employee, salaryComponent, condition, threshold);
                if (matches.isEmpty()) {
                    continue;
                }

                // Regroupement par SSA : le pourcentage s'applique une fois par assignation (point 1.3)
                Map<String, List<SlipPeriod>> slipsByAssignment = new LinkedHashMap<>();
                for (SalaryFilterDTO match : matches) {
                    String assignmentName = assignments.getSalaryStructureAssignmentByEmployeeAndDate(match);
                    slipsByAssignment.computeIfAbsent(assignmentName, k -> new ArrayList<>())
                            .add(new SlipPeriod(match.getSlipName(), match.getStartDate(), match.getEndDate(),
                                    match.getPostingDate(), match.getDocstatus()));
                }

                for (Map.Entry<String, List<SlipPeriod>> entry : slipsByAssignment.entrySet()) {
                    String assignmentName = entry.getKey();
                    SalaryStructAss assignment = assignments.getAssignmentById(assignmentName);
                    BigDecimal oldBase = assignment.getBase();
                    if (oldBase == null) {
                        failed.add(employee + " : base absente sur " + assignmentName);
                        continue;
                    }
                    BigDecimal newBase = oldBase.multiply(multiplier).setScale(BASE_SCALE, RoundingMode.HALF_UP);
                    assignment.setBase(newBase);

                    AssignmentAndSlips result = assignments.replaceAssignment(assignment, entry.getValue());
                    adjusted.add(new Adjusted(employee, result.assignment(), oldBase, newBase, result.slips()));
                    log.info("Employé {} : SSA {} -> {} base {} -> {}, {} fiche(s) régénérée(s)",
                            employee, assignmentName, result.assignment(), oldBase, newBase, result.slips().size());
                }
            } catch (RuntimeException e) {
                log.error("Modification groupée en échec pour {} : {}", employee, e.getMessage());
                failed.add(employee + " : " + userMessage(e));
            }
        }
        return new AdjustmentResult(adjusted, failed);
    }

    /** Facteur appliqué à la base : 1 + p/100 pour « ajouter », 1 − p/100 pour « retirer ». */
    static BigDecimal multiplier(String action, double percentage) {
        BigDecimal ratio = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100));
        return switch (action) {
            case "ajouter" -> BigDecimal.ONE.add(ratio);
            case "retirer" -> BigDecimal.ONE.subtract(ratio);
            default -> throw new IllegalArgumentException("Action inconnue : " + action + " (attendu : ajouter ou retirer)");
        };
    }

    private static String userMessage(RuntimeException e) {
        if (e instanceof ErpNextException erp && !erp.getErpNextMessage().isBlank()) {
            return erp.getErpNextMessage();
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
