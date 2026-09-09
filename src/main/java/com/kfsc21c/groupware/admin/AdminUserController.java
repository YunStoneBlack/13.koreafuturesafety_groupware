package com.kfsc21c.groupware.admin;

import com.kfsc21c.groupware.auth.Role;
import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import com.kfsc21c.groupware.staff.Employee;
import com.kfsc21c.groupware.staff.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAllWithEmployee());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserAccountForm());
        model.addAttribute("roles", Role.values());
        model.addAttribute("employees", employeeRepository.findAll());
        return "admin/users/form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") UserAccountForm form, Model model) {
        if (form.getUsername() == null || form.getUsername().isBlank()
                || form.getPassword() == null || form.getPassword().isBlank()) {
            model.addAttribute("error", "아이디와 비밀번호는 필수입니다.");
            model.addAttribute("roles", Role.values());
            model.addAttribute("employees", employeeRepository.findAll());
            return "admin/users/form";
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            model.addAttribute("roles", Role.values());
            model.addAttribute("employees", employeeRepository.findAll());
            return "admin/users/form";
        }

        User user = User.builder()
                .username(form.getUsername())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .displayName(form.getDisplayName())
                .role(form.getRole())
                .enabled(form.isEnabled())
                .build();
        if (form.getEmployeeId() != null) {
            employeeRepository.findById(form.getEmployeeId()).ifPresent(user::setEmployee);
        }
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다: " + id));

        UserAccountForm form = new UserAccountForm();
        form.setUsername(user.getUsername());
        form.setDisplayName(user.getDisplayName());
        form.setRole(user.getRole());
        form.setEnabled(user.isEnabled());
        form.setEmployeeId(user.getEmployee() != null ? user.getEmployee().getId() : null);

        model.addAttribute("form", form);
        model.addAttribute("userId", id);
        model.addAttribute("roles", Role.values());
        model.addAttribute("employees", employeeRepository.findAll());
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") UserAccountForm form) {
        User user = userRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다: " + id));

        user.setDisplayName(form.getDisplayName());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        if (form.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(form.getEmployeeId()).orElse(null);
            user.setEmployee(employee);
        } else {
            user.setEmployee(null);
        }
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다: " + id));
        if (user.getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("본인 계정은 삭제할 수 없습니다");
        }
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }
}
