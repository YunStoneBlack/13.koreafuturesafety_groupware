package com.kfsc21c.groupware.admin;

import com.kfsc21c.groupware.staff.Employee;
import com.kfsc21c.groupware.staff.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 직원 정보(조직도) 자체의 생성/수정/삭제는 여기(관리자 메뉴)로만 몰아둔다.
 * staff.EmployeeController는 조회 전용.
 */
@Controller
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "admin/employees/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "admin/employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Employee employee, BindingResult result) {
        if (result.hasErrors()) {
            return "admin/employees/form";
        }
        employeeRepository.save(employee);
        return "redirect:/admin/employees";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다: " + id));
        model.addAttribute("employee", employee);
        return "admin/employees/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Employee form, BindingResult result) {
        if (result.hasErrors()) {
            return "admin/employees/form";
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다: " + id));
        employee.setName(form.getName());
        employee.setDepartment(form.getDepartment());
        employee.setPosition(form.getPosition());
        employee.setPhone(form.getPhone());
        employee.setEmail(form.getEmail());
        employeeRepository.save(employee);
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return "redirect:/admin/employees";
    }
}
