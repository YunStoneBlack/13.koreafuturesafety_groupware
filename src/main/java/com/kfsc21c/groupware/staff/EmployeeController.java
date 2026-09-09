package com.kfsc21c.groupware.staff;

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

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "staff/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "staff/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Employee employee, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "staff/form";
        }
        employeeRepository.save(employee);
        return "redirect:/staff";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다: " + id));
        model.addAttribute("employee", employee);
        return "staff/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Employee form, BindingResult result) {
        if (result.hasErrors()) {
            return "staff/form";
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다: " + id));
        employee.setName(form.getName());
        employee.setDepartment(form.getDepartment());
        employee.setPosition(form.getPosition());
        employee.setPhone(form.getPhone());
        employee.setEmail(form.getEmail());
        employeeRepository.save(employee);
        return "redirect:/staff";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return "redirect:/staff";
    }
}
