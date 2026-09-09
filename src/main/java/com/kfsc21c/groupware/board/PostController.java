package com.kfsc21c.groupware.board;

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

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인한 사용자를 찾을 수 없습니다"));
    }

    private boolean canManage(Post post, UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin || post.getAuthor().getUsername().equals(principal.getUsername());
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        return "board/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("post", new Post());
        return "board/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Post post, BindingResult result,
                          @AuthenticationPrincipal UserDetails principal, Model model) {
        if (result.hasErrors()) {
            return "board/form";
        }
        post.setAuthor(currentUser(principal));
        postRepository.save(post);
        return "redirect:/board";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        Post post = postRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        model.addAttribute("post", post);
        model.addAttribute("canManage", canManage(post, principal));
        return "board/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
        if (!canManage(post, principal)) {
            throw new AccessDeniedException("이 게시글을 수정할 권한이 없습니다");
        }
        model.addAttribute("post", post);
        return "board/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Post form, BindingResult result,
                          @AuthenticationPrincipal UserDetails principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
        if (!canManage(post, principal)) {
            throw new AccessDeniedException("이 게시글을 수정할 권한이 없습니다");
        }
        if (result.hasErrors()) {
            return "board/form";
        }
        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        postRepository.save(post);
        return "redirect:/board/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
        if (!canManage(post, principal)) {
            throw new AccessDeniedException("이 게시글을 삭제할 권한이 없습니다");
        }
        postRepository.deleteById(id);
        return "redirect:/board";
    }
}
