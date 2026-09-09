package com.kfsc21c.groupware.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 매일 새벽 H2 DB를 통째로 백업 zip으로 떠서 회사 PC 등으로 가져갈 수 있게 남겨둔다.
 * H2의 BACKUP TO는 운영 중인 DB에 대고 그대로 돌려도 안전한(트랜잭션 일관성 있는) 방식이라,
 * .mv.db 파일을 직접 cp하는 것보다 안전하다 - 파일 복사 도중 쓰기가 겹쳐 깨질 위험이 없다.
 */
@Slf4j
@Component
public class BackupScheduler {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int RETENTION_DAYS = 14;

    private final DataSource dataSource;
    private final Path backupDir;

    public BackupScheduler(DataSource dataSource,
                            @Value("${GROUPWARE_BACKUP_DIR:./backup}") String backupDirPath) {
        this.dataSource = dataSource;
        this.backupDir = Path.of(backupDirPath);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void backupAndCleanup() {
        try {
            Files.createDirectories(backupDir);
            String filename = "groupware-" + LocalDate.now().format(FILE_DATE) + ".zip";
            Path target = backupDir.resolve(filename);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("BACKUP TO '" + target.toAbsolutePath() + "'");
            }
            log.info("DB 백업 완료: {}", target.toAbsolutePath());

            cleanupOld();
        } catch (SQLException | IOException e) {
            log.error("DB 백업 실패", e);
        }
    }

    private void cleanupOld() throws IOException {
        long cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 60 * 60 * 1000;
        try (Stream<Path> files = Files.list(backupDir)) {
            files.filter(p -> p.toString().endsWith(".zip"))
                    .filter(p -> p.toFile().lastModified() < cutoff)
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .forEach(p -> {
                        if (p.toFile().delete()) {
                            log.info("오래된 백업 삭제: {}", p);
                        }
                    });
        }
    }
}
