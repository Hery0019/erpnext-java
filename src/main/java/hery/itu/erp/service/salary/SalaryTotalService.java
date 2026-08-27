package hery.itu.erp.service.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;
import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.erpnext.Filters;
import hery.itu.erp.model.salary.SalaryDTO;
import hery.itu.erp.model.salary.SalaryDetail;
import hery.itu.erp.model.salary.SalaryGroupedDTO;

/**
 * Totaux de paie : fiches d'une période avec leurs composants, regroupées par mois.
 * Les composants sont lus en une requête sur la table enfant {@code Salary Detail} (par lots)
 * et non plus par un GET par fiche.
 */
@Service
public class SalaryTotalService {

    private static final Logger log = LoggerFactory.getLogger(SalaryTotalService.class);
    private static final String SALARY_SLIP = "Salary Slip";
    private static final String SALARY_DETAIL = "Salary Detail";
    private static final List<Integer> DOCSTATUS_ACTIF = List.of(0, 1);
    private static final List<String> SUMMARY_FIELDS = List.of(
            "name", "employee", "employee_name", "company", "posting_date", "start_date", "status", "currency",
            "gross_pay", "net_pay", "total_deduction", "total_earnings", "month_to_date", "year_to_date", "total_in_words");
    private static final List<String> DETAIL_FIELDS = List.of("parent", "parentfield", "salary_component", "amount");

    private final ErpNextClient client;

    public SalaryTotalService(ErpNextClient client) {
        this.client = client;
    }

    /**
     * Fiches actives (brouillon ou soumises) dont la période commence dans l'année, ou dans le
     * mois si fourni, avec leurs composants. Deux requêtes (plus une par lot de 100 fiches).
     *
     * @param month 1..12, ou null pour toute l'année
     */
    public List<SalaryDTO> getDetailedSlips(int year, Integer month) {
        LocalDate from = month == null ? LocalDate.of(year, 1, 1) : YearMonth.of(year, month).atDay(1);
        LocalDate to = month == null ? LocalDate.of(year, 12, 31) : YearMonth.of(year, month).atEndOfMonth();

        List<SalaryDTO> slips = client.list(SALARY_SLIP)
                .fields(SUMMARY_FIELDS)
                .filters(Filters.none()
                        .between("start_date", from.toString(), to.toString())
                        .in("docstatus", DOCSTATUS_ACTIF))
                .orderBy("start_date asc, employee_name asc")
                .fetchAll().stream()
                .map(SalaryTotalService::toSummary)
                .toList();
        attachComponents(slips);
        return slips;
    }

    /** @see #getDetailedSlips(int, Integer) */
    public List<SalaryDTO> getSalarySlipsByMonth(int year, int month) {
        return getDetailedSlips(year, month);
    }

    /** Une fiche complète (avec composants) par son nom. */
    public SalaryDTO getSalarySlipDetail(String slipName) {
        JsonNode data = client.getDoc(SALARY_SLIP, slipName);
        SalaryDTO slip = toSummary(data);
        fillComponentsFromDocument(slip, data);
        return slip;
    }

    /** Totaux par mois (brut, déductions, net, et par composant), triés par mois. */
    public Map<String, SalaryGroupedDTO> groupByMonth(List<SalaryDTO> slips) {
        Map<String, SalaryGroupedDTO> grouped = new TreeMap<>();
        for (SalaryDTO slip : slips) {
            if (slip.getMonth() == null) {
                continue;
            }
            SalaryGroupedDTO group = grouped.computeIfAbsent(slip.getMonth(), month -> {
                SalaryGroupedDTO g = new SalaryGroupedDTO();
                g.setMonth(month);
                return g;
            });
            group.setTotalGross(group.getTotalGross().add(orZero(slip.getGrossPay())));
            group.setTotalDeduction(group.getTotalDeduction().add(orZero(slip.getTotalDeduction())));
            group.setTotalNet(group.getTotalNet().add(orZero(slip.getNetPay())));
            for (SalaryDetail e : slip.getEarnings()) {
                group.getEarningsTotal().merge(e.getSalaryComponent(), orZero(e.getAmount()), BigDecimal::add);
            }
            for (SalaryDetail d : slip.getDeductions()) {
                group.getDeductionsTotal().merge(d.getSalaryComponent(), orZero(d.getAmount()), BigDecimal::add);
            }
        }
        return grouped;
    }

    // ------------------------------------------------------------------ interne

    private void attachComponents(List<SalaryDTO> slips) {
        if (slips.isEmpty()) {
            return;
        }
        Map<String, SalaryDTO> byName = new LinkedHashMap<>();
        slips.forEach(slip -> byName.put(slip.getSlipName(), slip));

        List<JsonNode> rows;
        try {
            rows = client.listChildRows(SALARY_DETAIL, SALARY_SLIP, DETAIL_FIELDS, byName.keySet(), null, "parent asc, idx asc");
        } catch (ErpNextValidationException | ErpNextForbiddenException e) {
            // Instance ERPNext qui refuse la lecture directe de la table enfant : repli sur un GET par fiche.
            log.warn("Lecture groupée de Salary Detail refusée ({}) : repli sur un appel par fiche", e.getErpNextMessage());
            for (SalaryDTO slip : slips) {
                fillComponentsFromDocument(slip, client.getDoc(SALARY_SLIP, slip.getSlipName()));
            }
            return;
        }
        for (JsonNode row : rows) {
            SalaryDTO slip = byName.get(row.path("parent").asText());
            if (slip == null) {
                continue;
            }
            boolean deduction = "deductions".equals(row.path("parentfield").asText());
            (deduction ? slip.getDeductions() : slip.getEarnings())
                    .add(toDetail(row, deduction ? SalaryDetail.Type.DEDUCTION : SalaryDetail.Type.EARNING));
        }
    }

    private static void fillComponentsFromDocument(SalaryDTO slip, JsonNode data) {
        for (JsonNode node : data.path("earnings")) {
            slip.getEarnings().add(toDetail(node, SalaryDetail.Type.EARNING));
        }
        for (JsonNode node : data.path("deductions")) {
            slip.getDeductions().add(toDetail(node, SalaryDetail.Type.DEDUCTION));
        }
    }

    private static SalaryDTO toSummary(JsonNode item) {
        SalaryDTO slip = new SalaryDTO();
        slip.setSlipName(item.path("name").asText(null));
        slip.setEmployeeId(item.path("employee").asText(null));
        slip.setEmployeeName(item.path("employee_name").asText(null));
        slip.setCompany(item.path("company").asText(null));
        String postingDate = item.path("posting_date").asText(null);
        if (postingDate != null) {
            slip.setPostingDate(LocalDate.parse(postingDate));
        }
        // Mois de paie = période du slip (start_date), jamais la date de comptabilisation (point 3.5)
        String startDate = item.path("start_date").asText(postingDate);
        if (startDate != null) {
            slip.setMonth(toMonthKey(LocalDate.parse(startDate)));
        }
        slip.setStatus(item.path("status").asText(null));
        slip.setCurrency(item.path("currency").asText(null));
        slip.setGrossPay(item.path("gross_pay").decimalValue());
        slip.setNetPay(item.path("net_pay").decimalValue());
        slip.setTotalDeduction(item.path("total_deduction").decimalValue());
        slip.setTotalEarnings(item.path("total_earnings").decimalValue());
        slip.setMonthToDate(item.path("month_to_date").decimalValue());
        slip.setYearToDate(item.path("year_to_date").decimalValue());
        slip.setTotalInWords(item.path("total_in_words").asText(null));
        return slip;
    }

    private static SalaryDetail toDetail(JsonNode node, SalaryDetail.Type type) {
        SalaryDetail detail = new SalaryDetail();
        detail.setSalaryComponent(node.path("salary_component").asText(null));
        detail.setAmount(node.path("amount").decimalValue());
        detail.setType(type);
        return detail;
    }

    private static String toMonthKey(LocalDate date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
