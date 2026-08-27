package hery.itu.erp.service.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.SalaryStructAssService.AssignmentAndSlips;
import hery.itu.erp.service.salary.SalaryStructAssService.SlipPeriod;

/**
 * Aléa 1 — génération des fiches de paie d'un employé mois par mois sur une période.
 * <p>
 * Pour chaque mois : un SSA (from = 1er du mois, to = dernier jour) et son Salary Slip.
 * Un mois déjà couvert est ignoré sauf si l'écrasement est demandé, auquel cas le SSA de
 * <b>cet employé</b> pour <b>ce mois</b> est remplacé (annulation + amendement) et le slip régénéré.
 * Aucune erreur n'est avalée : chaque mois en échec est rapporté dans le résultat.
 */
@Service
public class PayrollGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PayrollGenerationService.class);

    private final SalaryStructAssService assignments;

    public PayrollGenerationService(SalaryStructAssService assignments) {
        this.assignments = assignments;
    }

    /**
     * @param created slips créés (nom ERPNext)
     * @param skipped mois ignorés, avec la raison
     * @param failed  mois en échec, avec le message d'erreur
     */
    public record GenerationResult(List<String> created, List<String> skipped, List<String> failed) {
        public boolean hasFailures() {
            return !failed.isEmpty();
        }
    }

    /**
     * @param template  employé, structure, société, devise et base (peut être nulle : voir {@code useAverage})
     * @param overwrite remplacer les mois déjà couverts
     * @param useAverage si la base est absente, utiliser la moyenne des bases soumises ; sinon la dernière base de l'employé
     */
    public GenerationResult generate(SalaryStructAss template, LocalDate startDate, LocalDate endDate,
                                     boolean overwrite, boolean useAverage) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin est antérieure à la date de début");
        }
        String employee = template.getEmployee();
        BigDecimal base = resolveBase(template, useAverage);

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate limit = endDate.withDayOfMonth(1);
        while (!current.isAfter(limit)) {
            String monthLabel = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            String start = current.toString();
            String end = current.with(TemporalAdjusters.lastDayOfMonth()).toString();

            try {
                boolean slipExists = assignments.salarySlipExists(employee, start, end);
                Optional<String> existingAssignment = assignments.findAssignmentName(employee, start);
                SalaryStructAss monthAssignment = forMonth(template, base, start, end);

                if (slipExists && !overwrite) {
                    skipped.add(monthLabel + " : fiche de paie déjà existante");
                } else if (existingAssignment.isPresent()) {
                    if (!overwrite) {
                        skipped.add(monthLabel + " : assignation déjà existante (écrasement non demandé)");
                    } else {
                        monthAssignment.setName(existingAssignment.get());
                        List<SlipPeriod> slips = assignments.findActiveSlips(employee, start, end);
                        AssignmentAndSlips result = assignments.replaceAssignment(monthAssignment, slips);
                        created.addAll(result.slips());
                        if (slips.isEmpty()) {
                            created.add(assignments.createSalarySlipAndSubmit(monthAssignment));
                        }
                    }
                } else {
                    if (slipExists) {
                        // écrasement demandé : fiche existante sans assignation (créée à la main) -> on la retire
                        assignments.findActiveSlips(employee, start, end).forEach(assignments::retireSlip);
                    }
                    created.addAll(assignments.createAssignmentAndSlip(monthAssignment).slips());
                }
            } catch (RuntimeException e) {
                log.error("Génération {} pour {} en échec : {}", monthLabel, employee, e.getMessage());
                failed.add(monthLabel + " : " + userMessage(e));
            }
            current = current.plusMonths(1);
        }

        log.info("Génération {} -> {} pour {} : {} créés, {} ignorés, {} en échec",
                startDate, endDate, employee, created.size(), skipped.size(), failed.size());
        return new GenerationResult(created, skipped, failed);
    }

    private BigDecimal resolveBase(SalaryStructAss template, boolean useAverage) {
        if (template.getBase() != null) {
            return template.getBase();
        }
        if (useAverage) {
            return assignments.getMoyenneTotalBaseOfAllEmployees();
        }
        return assignments.getLastSalaryBase(template.getEmployee())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune base fournie et aucune assignation soumise pour l'employé " + template.getEmployee()));
    }

    private static SalaryStructAss forMonth(SalaryStructAss template, BigDecimal base, String start, String end) {
        SalaryStructAss month = new SalaryStructAss();
        month.setEmployee(template.getEmployee());
        month.setSalary_structure(template.getSalary_structure());
        month.setCompany(template.getCompany());
        month.setCurrency(template.getCurrency());
        month.setBase(base);
        month.setFrom_date(start);
        month.setTo_date(end);
        month.setPosting_date(end);
        return month;
    }

    private static String userMessage(RuntimeException e) {
        if (e instanceof ErpNextException erp && !erp.getErpNextMessage().isBlank()) {
            return erp.getErpNextMessage();
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
