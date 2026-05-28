package com.toolshare.controller;

import com.toolshare.model.ApiResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DbVersionController {

    private final Flyway flyway;

    public DbVersionController(Flyway flyway) {
        this.flyway = flyway;
    }

    @GetMapping("/db/version")
    public ApiResponse<Map<String, Object>> version() {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        return ApiResponse.ok(Map.of(
                "currentVersion", current != null ? current.getVersion().toString() : "none",
                "currentDescription", current != null ? current.getDescription() : "none",
                "migrationsApplied", info.applied().length
        ));
    }
}
