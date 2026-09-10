package com.kfsc21c.groupware.admin;

import com.kfsc21c.groupware.payroll.PayrollRecord;
import com.kfsc21c.groupware.payroll.PayrollRecordForm;
import com.kfsc21c.groupware.payroll.PayrollRecordRepository;
import com.kfsc21c.groupware.staff.Employee;
import com.kfsc21c.groupware.staff.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * 전 직원 급여의 등록/수정/삭제. /admin/** 전체가 SecurityConfig에서
 * hasRole('ADMIN')으로 이미 막혀 있어 여기서 별도 권한 체크는 필요 없다.
 */
@Controller
@RequestMapping("/admin/payroll")
@RequiredArgsConstructor
public class AdminPayrollController {

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("records", payrollRecordRepository.findAllWithEmployee());
        return "admin/payroll/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        PayrollRecordForm form = new PayrollRecordForm();
        LocalDate now = LocalDate.now();
        form.setPayYear(now.getYear());
        form.setPayMonth(now.getMonthValue());
        model.addAttribute("form", form);
        model.addAttribute("employees", employeeRepository.findAll());
        return "admin/payroll/form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") PayrollRecordForm form, Model model) {
        Employee employee = form.getEmployeeId() != null
                ? employeeRepository.findById(form.getEmployeeId()).orElse(null) : null;
        if (employee == null) {
            model.addAttribute("error", "직원을 선택해주세요.");
            model.addAttribute("employees", employeeRepository.findAll());
            return "admin/payroll/form";
        }
        PayrollRecord record = new PayrollRecord();
        record.setEmployee(employee);
        form.applyTo(record);
        payrollRecordRepository.save(record);
        return "redirect:/admin/payroll";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PayrollRecord record = payrollRecordRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 내역을 찾을 수 없습니다: " + id));
        model.addAttribute("form", PayrollRecordForm.from(record));
        model.addAttribute("recordId", id);
        model.addAttribute("employees", employeeRepository.findAll());
        return "admin/payroll/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") PayrollRecordForm form, Model model) {
        PayrollRecord record = payrollRecordRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 내역을 찾을 수 없습니다: " + id));
        Employee employee = form.getEmployeeId() != null
                ? employeeRepository.findById(form.getEmployeeId()).orElse(null) : null;
        if (employee == null) {
            model.addAttribute("error", "직원을 선택해주세요.");
            model.addAttribute("recordId", id);
            model.addAttribute("employees", employeeRepository.findAll());
            return "admin/payroll/form";
        }
        record.setEmployee(employee);
        form.applyTo(record);
        payrollRecordRepository.save(record);
        return "redirect:/admin/payroll";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        payrollRecordRepository.deleteById(id);
        return "redirect:/admin/payroll";
    }
}
