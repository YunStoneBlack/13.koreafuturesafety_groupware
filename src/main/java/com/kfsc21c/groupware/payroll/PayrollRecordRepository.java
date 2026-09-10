package com.kfsc21c.groupware.payroll;

import com.kfsc21c.groupware.staff.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    @Query("SELECT p FROM PayrollRecord p JOIN FETCH p.employee " +
            "WHERE p.employee = :employee ORDER BY p.payYear DESC, p.payMonth DESC")
    List<PayrollRecord> findByEmployeeOrderByPayYearDescPayMonthDesc(Employee employee);

    @Query("SELECT p FROM PayrollRecord p JOIN FETCH p.employee " +
            "ORDER BY p.payYear DESC, p.payMonth DESC, p.employee.name")
    List<PayrollRecord> findAllWithEmployee();

    @Query("SELECT p FROM PayrollRecord p JOIN FETCH p.employee WHERE p.id = :id")
    Optional<PayrollRecord> findByIdWithEmployee(Long id);
}
