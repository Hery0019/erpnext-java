package hery.itu.erp.service.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.salary.PayrollGenerationService.GenerationResult;
import hery.itu.erp.service.salary.SalaryStructAssService.AssignmentAndSlips;
import hery.itu.erp.service.salary.SalaryStructAssService.SlipPeriod;

@ExtendWith(MockitoExtension.class)
class PayrollGenerationServiceTest {

    private static final String EMP = "HR-EMP-00001";
    private static final LocalDate MARS_1 = LocalDate.of(2025, 3, 1);
    private static final LocalDate MARS_31 = LocalDate.of(2025, 3, 31);

    @Mock
    private SalaryStructAssService assignments;

    @InjectMocks
    private PayrollGenerationService service;

    @Test
    void moisDejaCouvertIgnoreSansEcrasement() {
        when(assignments.salarySlipExists(EMP, "2025-03-01", "2025-03-31")).thenReturn(true);
        when(assignments.findAssignmentName(EMP, "2025-03-01")).thenReturn(Optional.of("HR-SSA-2025-00042"));

        GenerationResult result = service.generate(template("1000"), MARS_1, MARS_31, false, false);

        assertThat(result.created()).isEmpty();
        assertThat(result.skipped()).containsExactly("2025-03 : fiche de paie déjà existante");
        assertThat(result.failed()).isEmpty();
        verify(assignments, never()).replaceAssignment(any(), anyList());
        verify(assignments, never()).createAssignmentAndSlip(any());
    }

    @Test
    void ecrasementRemplaceLeSsaDeLEmployePourLeMoisEtRegenereSesFiches() {
        SlipPeriod existingSlip = new SlipPeriod("Sal Slip/HR-EMP-00001/00003", "2025-03-01", "2025-03-31", "2025-03-31", 1);
        when(assignments.salarySlipExists(EMP, "2025-03-01", "2025-03-31")).thenReturn(true);
        when(assignments.findAssignmentName(EMP, "2025-03-01")).thenReturn(Optional.of("HR-SSA-2025-00042"));
        when(assignments.findActiveSlips(EMP, "2025-03-01", "2025-03-31")).thenReturn(List.of(existingSlip));
        when(assignments.replaceAssignment(any(), eq(List.of(existingSlip))))
                .thenReturn(new AssignmentAndSlips("HR-SSA-2025-00042-1", List.of("Sal Slip/HR-EMP-00001/00003-1")));

        GenerationResult result = service.generate(template("1200"), MARS_1, MARS_31, true, false);

        ArgumentCaptor<SalaryStructAss> captor = ArgumentCaptor.forClass(SalaryStructAss.class);
        verify(assignments).replaceAssignment(captor.capture(), eq(List.of(existingSlip)));
        SalaryStructAss replaced = captor.getValue();
        assertThat(replaced.getName()).isEqualTo("HR-SSA-2025-00042"); // le SSA de CET employé pour CE mois
        assertThat(replaced.getEmployee()).isEqualTo(EMP);
        assertThat(replaced.getFrom_date()).isEqualTo("2025-03-01");
        assertThat(replaced.getTo_date()).isEqualTo("2025-03-31");
        assertThat(replaced.getBase()).isEqualByComparingTo("1200");
        assertThat(result.created()).containsExactly("Sal Slip/HR-EMP-00001/00003-1");
        verify(assignments, never()).createAssignmentAndSlip(any());
    }

    @Test
    void unMoisEnEchecNArretePasLesAutresEtEstRapporte() {
        when(assignments.salarySlipExists(eq(EMP), any(), any())).thenReturn(false);
        when(assignments.findAssignmentName(eq(EMP), any())).thenReturn(Optional.empty());
        when(assignments.createAssignmentAndSlip(any()))
                .thenThrow(new ErpNextValidationException("POST /api/resource/Salary Slip", 417, "Base obligatoire"))
                .thenReturn(new AssignmentAndSlips("HR-SSA-2025-00050", List.of("Sal Slip/HR-EMP-00001/00010")));

        GenerationResult result = service.generate(template("1000"),
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31), false, false);

        assertThat(result.failed()).containsExactly("2025-02 : Base obligatoire");
        assertThat(result.created()).containsExactly("Sal Slip/HR-EMP-00001/00010");
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void baseAbsenteRepriseDeLaDerniereAssignationSoumise() {
        when(assignments.getLastSalaryBase(EMP)).thenReturn(Optional.of(new BigDecimal("900")));
        when(assignments.salarySlipExists(EMP, "2025-03-01", "2025-03-31")).thenReturn(false);
        when(assignments.findAssignmentName(EMP, "2025-03-01")).thenReturn(Optional.empty());
        when(assignments.createAssignmentAndSlip(any()))
                .thenReturn(new AssignmentAndSlips("HR-SSA-2025-00051", List.of("Sal Slip/HR-EMP-00001/00011")));

        service.generate(template(null), MARS_1, MARS_31, false, false);

        ArgumentCaptor<SalaryStructAss> captor = ArgumentCaptor.forClass(SalaryStructAss.class);
        verify(assignments).createAssignmentAndSlip(captor.capture());
        assertThat(captor.getValue().getBase()).isEqualByComparingTo("900");
        assertThat(captor.getValue().getPosting_date()).isEqualTo("2025-03-31");
    }

    @Test
    void baseAbsenteSansHistoriqueEchoueAvantToutAppelErpNext() {
        when(assignments.getLastSalaryBase(EMP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(template(null), MARS_1, MARS_31, false, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EMP);
        verify(assignments, never()).createAssignmentAndSlip(any());
    }

    @Test
    void periodeInverseeRefusee() {
        assertThatThrownBy(() -> service.generate(template("1000"), MARS_31, MARS_1, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SalaryStructAss template(String base) {
        SalaryStructAss t = new SalaryStructAss();
        t.setEmployee(EMP);
        t.setSalary_structure("Standard");
        t.setCompany("Orinasa SA");
        t.setCurrency("MGA");
        if (base != null) {
            t.setBase(new BigDecimal(base));
        }
        return t;
    }
}
