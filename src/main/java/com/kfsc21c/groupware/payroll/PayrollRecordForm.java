package com.kfsc21c.groupware.payroll;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 급여 등록/수정 폼 전용 객체. PayrollRecord는 Employee 엔티티를 참조하는데,
 * 폼에서는 <select>로 고른 직원 id(employeeId)만 받아서 컨트롤러에서
 * Employee로 바꿔 끼워 넣는다(admin.UserAccountForm의 employeeId 패턴과 동일).
 */
@Getter
@Setter
@NoArgsConstructor
public class PayrollRecordForm {
    private Long employeeId;
    private Integer payYear;
    private Integer payMonth;
    private BigDecimal baseSalary = BigDecimal.ZERO;
    private BigDecimal positionAllowance = BigDecimal.ZERO;
    private BigDecimal overtimeAllowance = BigDecimal.ZERO;
    private BigDecimal otherAllowance = BigDecimal.ZERO;
    private BigDecimal nationalPension = BigDecimal.ZERO;
    private BigDecimal healthInsurance = BigDecimal.ZERO;
    private BigDecimal longTermCare = BigDecimal.ZERO;
    private BigDecimal employmentInsurance = BigDecimal.ZERO;
    private BigDecimal incomeTax = BigDecimal.ZERO;
    private BigDecimal localIncomeTax = BigDecimal.ZERO;
    private String memo;

    public static PayrollRecordForm from(PayrollRecord record) {
        PayrollRecordForm form = new PayrollRecordForm();
        form.setEmployeeId(record.getEmployee().getId());
        form.setPayYear(record.getPayYear());
        form.setPayMonth(record.getPayMonth());
        form.setBaseSalary(record.getBaseSalary());
        form.setPositionAllowance(record.getPositionAllowance());
        form.setOvertimeAllowance(record.getOvertimeAllowance());
        form.setOtherAllowance(record.getOtherAllowance());
        form.setNationalPension(record.getNationalPension());
        form.setHealthInsurance(record.getHealthInsurance());
        form.setLongTermCare(record.getLongTermCare());
        form.setEmploymentInsurance(record.getEmploymentInsurance());
        form.setIncomeTax(record.getIncomeTax());
        form.setLocalIncomeTax(record.getLocalIncomeTax());
        form.setMemo(record.getMemo());
        return form;
    }

    public void applyTo(PayrollRecord record) {
        record.setPayYear(payYear);
        record.setPayMonth(payMonth);
        record.setBaseSalary(nz(baseSalary));
        record.setPositionAllowance(nz(positionAllowance));
        record.setOvertimeAllowance(nz(overtimeAllowance));
        record.setOtherAllowance(nz(otherAllowance));
        record.setNationalPension(nz(nationalPension));
        record.setHealthInsurance(nz(healthInsurance));
        record.setLongTermCare(nz(longTermCare));
        record.setEmploymentInsurance(nz(employmentInsurance));
        record.setIncomeTax(nz(incomeTax));
        record.setLocalIncomeTax(nz(localIncomeTax));
        record.setMemo(memo);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
