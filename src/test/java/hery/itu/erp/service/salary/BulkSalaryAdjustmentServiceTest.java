package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hery.itu.erp.erpnext.ErpNextForbiddenException;
import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.BulkSalaryAdjustmentService.AdjustmentResult;
import hery.itu.erp.service.salary.SalaryStructAssService.AssignmentAndSlips;
import hery.itu.erp.service.salary.SalaryStructAssService.SlipPeriod;

@ExtendWith(MockitoExtension.class)
class BulkSalaryAdjustmentServiceTest {

    private static final String EMP = "HR-EMP-00001";

    @Mock
    private SalaryStructAssService assignments;

    @InjectMocks
    private BulkSalaryAdjustmentService service;

    @Test
    void plusieursFichesSurLeMemeSsaNeComposentPasLePourcentage() {
        SalaryFilterDTO jan = match("Sal Slip/HR-EMP-00001/00001", "2025-01-01", "2025-01-31");
        SalaryFilterDTO fev = match("Sal Slip/HR-EMP-00001/00002", "2025-02-01", "2025-02-28");
        SalaryFilterDTO mar = match("Sal Slip/HR-EMP-00001/00003", "2025-03-01", "2025-03-31");
        when(assignments.getSalaryComponentValues(EMP, "Indemnité", "inf", 100000.0)).thenReturn(List.of(jan, fev, mar));
        when(assignments.getSalaryStructureAssignmentByEmployeeAndDate(any())).thenReturn("HR-SSA-2025-00001");
        when(assignments.getAssignmentById("HR-SSA-2025-00001")).thenReturn(assignment("HR-SSA-2025-00001", "1000"));
        when(assignments.replaceAssignment(any(), anyList()))
                .thenReturn(new AssignmentAndSlips("HR-SSA-2025-00001-1", List.of("a-1", "b-1", "c-1")));

        AdjustmentResult result = service.apply(List.of(EMP), "Indemnité", "inf", "retirer", 100000.0, 10.0);

        ArgumentCaptor<SalaryStructAss> ssa = ArgumentCaptor.forClass(SalaryStructAss.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlipPeriod>> slips = ArgumentCaptor.forClass(List.class);
        verify(assignments, times(1)).replaceAssignment(ssa.capture(), slips.capture());
        assertThat(ssa.getValue().getBase()).isEqualByComparingTo("900.00"); // -10 % une seule fois, pas 0,9³
        assertThat(slips.getValue()).extracting(SlipPeriod::name)
                .containsExactly("Sal Slip/HR-EMP-00001/00001", "Sal Slip/HR-EMP-00001/00002", "Sal Slip/HR-EMP-00001/00003");
        assertThat(result.adjusted()).hasSize(1);
        assertThat(result.adjusted().get(0).oldBase()).isEqualByComparingTo("1000");
        assertThat(result.adjusted().get(0).newBase()).isEqualByComparingTo("900.00");
        assertThat(result.failed()).isEmpty();
    }

    @Test
    void desFichesSurDesSsaDifferentsSontAjusteesChacuneUneFois() {
        SalaryFilterDTO jan = match("Sal Slip/HR-EMP-00001/00001", "2025-01-01", "2025-01-31");
        SalaryFilterDTO fev = match("Sal Slip/HR-EMP-00001/00002", "2025-02-01", "2025-02-28");
        when(assignments.getSalaryComponentValues(EMP, "Basic", "sup", 500.0)).thenReturn(List.of(jan, fev));
        when(assignments.getSalaryStructureAssignmentByEmployeeAndDate(jan)).thenReturn("SSA-JAN");
        when(assignments.getSalaryStructureAssignmentByEmployeeAndDate(fev)).thenReturn("SSA-FEV");
        when(assignments.getAssignmentById("SSA-JAN")).thenReturn(assignment("SSA-JAN", "1000"));
        when(assignments.getAssignmentById("SSA-FEV")).thenReturn(assignment("SSA-FEV", "2000"));
        when(assignments.replaceAssignment(any(), anyList()))
                .thenReturn(new AssignmentAndSlips("SSA-JAN-1", List.of("j-1")))
                .thenReturn(new AssignmentAndSlips("SSA-FEV-1", List.of("f-1")));

        AdjustmentResult result = service.apply(List.of(EMP), "Basic", "sup", "ajouter", 500.0, 10.0);

        ArgumentCaptor<SalaryStructAss> ssa = ArgumentCaptor.forClass(SalaryStructAss.class);
        verify(assignments, times(2)).replaceAssignment(ssa.capture(), anyList());
        assertThat(ssa.getAllValues()).extracting(SalaryStructAss::getBase)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1100.00"), new BigDecimal("2200.00"));
        assertThat(result.adjusted()).extracting(a -> a.assignment()).containsExactly("SSA-JAN-1", "SSA-FEV-1");
    }

    @Test
    void unEmployeEnEchecNArretePasLesAutresEtEstRapporte() {
        when(assignments.getSalaryComponentValues("HR-EMP-00001", "Basic", "inf", 100.0))
                .thenThrow(new ErpNextForbiddenException("GET /api/resource/Salary Slip", 403, "Not permitted"));
        SalaryFilterDTO m = new SalaryFilterDTO("Sal Slip/HR-EMP-00002/00001", "HR-EMP-00002", "Basic", 50.0,
                "2025-01-31", "2025-01-01", "2025-01-31", 1);
        when(assignments.getSalaryComponentValues("HR-EMP-00002", "Basic", "inf", 100.0)).thenReturn(List.of(m));
        when(assignments.getSalaryStructureAssignmentByEmployeeAndDate(m)).thenReturn("SSA-2");
        when(assignments.getAssignmentById("SSA-2")).thenReturn(assignment("SSA-2", "1000"));
        when(assignments.replaceAssignment(any(), anyList())).thenReturn(new AssignmentAndSlips("SSA-2-1", List.of("s")));

        AdjustmentResult result = service.apply(List.of("HR-EMP-00001", "HR-EMP-00002"), "Basic", "inf", "retirer", 100.0, 5.0);

        assertThat(result.failed()).containsExactly("HR-EMP-00001 : Not permitted");
        assertThat(result.adjusted()).hasSize(1);
        assertThat(result.adjusted().get(0).employee()).isEqualTo("HR-EMP-00002");
    }

    @Test
    void facteurSelonLAction() {
        assertThat(BulkSalaryAdjustmentService.multiplier("ajouter", 10)).isEqualByComparingTo("1.1");
        assertThat(BulkSalaryAdjustmentService.multiplier("retirer", 25)).isEqualByComparingTo("0.75");
        assertThatThrownBy(() -> BulkSalaryAdjustmentService.multiplier("doubler", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SalaryFilterDTO match(String slip, String start, String end) {
        return new SalaryFilterDTO(slip, EMP, "Indemnité", 50000.0, end, start, end, 1);
    }

    private static SalaryStructAss assignment(String name, String base) {
        SalaryStructAss ass = new SalaryStructAss();
        ass.setName(name);
        ass.setEmployee(EMP);
        ass.setBase(new BigDecimal(base));
        return ass;
    }
}
