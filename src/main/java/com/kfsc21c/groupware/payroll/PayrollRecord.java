package com.kfsc21c.groupware.payroll;

import com.kfsc21c.groupware.common.BaseEntity;
import com.kfsc21c.groupware.staff.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 직원 한 명의 특정 월 급여 지급 내역. 지급/공제 항목을 각각 분리해서 저장하고
 * 지급총액/공제총액/실수령액은 저장하지 않고 매번 계산한다(값이 바뀌어도 항상
 * 항목 합계와 어긋나지 않도록).
 */
@Entity
@Table(name = "payroll_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @Column(nullable = false)
    private Integer payYear;

    @NotNull
    @Column(nullable = false)
    private Integer payMonth;

    // ---------- 지급 항목 ----------
    @NotNull
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal baseSalary;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal positionAllowance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal overtimeAllowance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    // ---------- 공제 항목 ----------
    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal nationalPension = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal healthInsurance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal longTermCare = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal employmentInsurance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal incomeTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal localIncomeTax = BigDecimal.ZERO;

    @Column(length = 500)
    private String memo;

    public BigDecimal getTotalPayment() {
        return baseSalary.add(positionAllowance).add(overtimeAllowance).add(otherAllowance);
    }

    public BigDecimal getTotalDeduction() {
        return nationalPension.add(healthInsurance).add(longTermCare)
                .add(employmentInsurance).add(incomeTax).add(localIncomeTax);
    }

    public BigDecimal getNetPay() {
        return getTotalPayment().subtract(getTotalDeduction());
    }
}
