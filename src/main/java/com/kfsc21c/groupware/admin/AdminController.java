package com.kfsc21c.groupware.admin;

import com.kfsc21c.groupware.config.BackupScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final BackupScheduler backupScheduler;

    @GetMapping("/admin")
    public String index() {
        return "admin/index";
    }

    @PostMapping("/admin/backup")
    public String backupNow() {
        backupScheduler.backupAndCleanup();
        return "redirect:/admin?backedUp";
    }
}
