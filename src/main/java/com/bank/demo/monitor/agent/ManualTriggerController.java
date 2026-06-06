package com.bank.demo.monitor.agent;

import com.bank.demo.monitor.jenkins.JenkinsClient;
import com.bank.demo.monitor.model.BuildAnalysisResult;
import com.bank.demo.monitor.model.BuildInfo;
import com.bank.demo.monitor.service.FixReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for manual triggering — useful for demos and testing.
 *
 * Python analogy: FastAPI/Flask endpoints.
 *
 * @RestController = @Controller + @ResponseBody (automatically serializes return values to JSON)
 * @RequestMapping = base URL path for all methods in this class
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class ManualTriggerController {

    private final JenkinsClient jenkinsClient;
    private final BuildAnalysisAgent buildAnalysisAgent;
    private final FixReportService fixReportService;

    /**
     * GET /api/monitor/status
     * Returns the last build status without triggering analysis.
     * Useful to check if Jenkins is reachable.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        BuildInfo lastBuild = jenkinsClient.getLastBuild();

        if (lastBuild == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Jenkins unreachable or job not found"));
        }

        return ResponseEntity.ok(Map.of(
                "buildNumber", lastBuild.getNumber(),
                "result",      lastBuild.getResult() != null ? lastBuild.getResult() : "RUNNING",
                "displayName", lastBuild.getDisplayName(),
                "url",         lastBuild.getUrl()
        ));
    }

    /**
     * POST /api/monitor/analyse
     * Manually trigger analysis of the last (or a specific) build.
     *
     * Request body (optional JSON):
     *   { "buildNumber": 42 }   — analyse a specific build
     *   {}                      — analyse the last build
     */
    @PostMapping("/analyse")
    public ResponseEntity<?> triggerAnalysis(
            @RequestBody(required = false) Map<String, Integer> body) {

        if (body != null && body.containsKey("buildNumber")) {
            int requestedBuild = body.get("buildNumber");
            log.info("Manual analysis triggered for build #{}", requestedBuild);

            String consoleLog = jenkinsClient.getConsoleLog(requestedBuild);
            BuildAnalysisResult result = buildAnalysisAgent.analyse(requestedBuild, consoleLog);
            fixReportService.publishReport(result);
            return ResponseEntity.ok(result);
        }

        // No build number specified — use last build
        var targetBuild = jenkinsClient.getLastBuild();
        if (targetBuild == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Jenkins unreachable"));
        }

        log.info("Manual analysis triggered for last build #{}", targetBuild.getNumber());
        String consoleLog = jenkinsClient.getConsoleLog(targetBuild.getNumber());
        BuildAnalysisResult result = buildAnalysisAgent.analyse(targetBuild.getNumber(), consoleLog);
        fixReportService.publishReport(result);

        return ResponseEntity.ok(result);
    }
}
