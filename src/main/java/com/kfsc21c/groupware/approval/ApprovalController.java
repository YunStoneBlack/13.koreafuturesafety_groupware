package com.kfsc21c.groupware.approval;

import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalService approvalService;
    private final UserRepository userRepository;

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인한 사용자를 찾을 수 없습니다"));
    }

    private boolean isAdmin(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("requests", approvalRequestRepository.findAllWithUsers());
        return "approval/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("request", new ApprovalRequest());
        model.addAttribute("types", ApprovalType.values());
        return "approval/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") ApprovalRequest form, BindingResult result,
                          @AuthenticationPrincipal UserDetails principal, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("types", ApprovalType.values());
            return "approval/form";
        }
        form.setRequester(currentUser(principal));
        approvalRequestRepository.save(form);
        return "redirect:/approval";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        ApprovalRequest req = approvalRequestRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new IllegalArgumentException("결재 요청을 찾을 수 없습니다: " + id));
        boolean isOwner = req.getRequester().getUsername().equals(principal.getUsername());
        model.addAttribute("req", req);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin(principal));
        return "approval/view";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        approvalService.submit(id, currentUser(principal));
        return "redirect:/approval/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String comment,
                           @AuthenticationPrincipal UserDetails principal) {
        if (!isAdmin(principal)) {
            throw new AccessDeniedException("승인 권한이 없습니다");
        }
        approvalService.decide(id, currentUser(principal), ApprovalStatus.APPROVED, comment);
        return "redirect:/approval/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String comment,
                          @AuthenticationPrincipal UserDetails principal) {
        if (!isAdmin(principal)) {
            throw new AccessDeniedException("반려 권한이 없습니다");
        }
        approvalService.decide(id, currentUser(principal), ApprovalStatus.REJECTED, comment);
        return "redirect:/approval/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        approvalService.deleteDraft(id, currentUser(principal));
        return "redirect:/approval";
    }
}
