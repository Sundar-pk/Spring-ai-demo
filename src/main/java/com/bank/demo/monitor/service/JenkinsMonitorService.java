package com.bank.demo.monitor.service;

import com.bank.demo.monitor.agent.BuildAnalysisAgent;
import com.bank.demo.monitor.config.JenkinsProperties;
import com.bank.demo.monitor.config.MonitorProperties;
import com.bank.demo.monitor.jenkins.JenkinsClient;
import com.bank.demo.monitor.model.BuildAnalysisResult;
import com.bank.demo.monitor.model.BuildInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * THE SCHEDULER — polls Jenkins every N minutes
 * ============================================================
 *
 * This is the "orchestrator" of the agentic loop:
 *
 *   [Trigger] → Poll Jenkins → Failed? → Fetch Log → Analyse → Report
 *
 * Python analogy:
 *   @app.on_event("startup")
 *   async def start_scheduler():
 *       scheduler.add_job(poll_jenkins, 'interval', minutes=5)
 *       scheduler.start()
 *
 * Spring equivalent: @Scheduled(fixedDelayString = ...) on a method.
 * Spring calls this automatically at the configured interval.
 *
 * Key difference from LangGraph:
 *   - LangGraph = graph of nodes with state machine flow
 *   - Spring AI agentic = services calling each other with a scheduler trigger
 *   Both are valid agent patterns; Spring's is simpler for linear pipelines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsMonitorService {

    private final JenkinsClient jenkinsClient;
    private final BuildAnalysisAgent buildAnalysisAgent;
    private final FixReportService fixReportService;
    private final JenkinsProperties jenkinsProperties;
    private final MonitorProperties monitorProperties;

    // Track the last analysed build to avoid duplicate analysis
    private int lastAnalysedBuildNumber = -1;

    /**
     * Main polling loop.
     *
     * @Scheduled(fixedDelayString = "${monitor.poll-interval-ms}")
     *   - fixedDelay = wait N ms AFTER the previous execution finishes before running again
     *   - ${...} is a Spring property placeholder — reads from application.yml / env vars
     *   - Python analogy: time.sleep(interval) at the end of your while True loop
     *
     * IMPORTANT: fixedDelayString only supports ${property.key} placeholders.
     *   SpEL expressions #{...} are NOT supported here and cause a startup crash.
     *
     * initialDelayString = "5000" — wait 5 seconds after startup before first poll
     *   (gives the app time to fully initialize)
     */
    @Scheduled(
            fixedDelayString = "${monitor.poll-interval-ms}",
            initialDelayString = "5000"
    )
    public void pollJenkins() {
        log.info("--- Polling Jenkins for job: {} ---", jenkinsProperties.getJobName());

        // Step 1: Get last build info from Jenkins
        BuildInfo lastBuild = jenkinsClient.getLastBuild();

        if (lastBuild == null) {
            log.warn("Could not retrieve build info — Jenkins may be unreachable.");
            return;
        }

        if (lastBuild.isStillRunning()) {
            log.info("Build #{} is still running — skipping analysis.", lastBuild.getNumber());
            return;
        }

        log.info("Last build: #{} — Result: {}", lastBuild.getNumber(), lastBuild.getResult());

        // Step 2: Check if it failed
        if (!lastBuild.isFailed()) {
            log.info("Build #{} result is {} — no action needed.",
                    lastBuild.getNumber(), lastBuild.getResult());
            lastAnalysedBuildNumber = lastBuild.getNumber();
            return;
        }

        // Step 3: Avoid re-analysing the same build
        if (lastBuild.getNumber() == lastAnalysedBuildNumber) {
            log.info("Build #{} was already analysed — waiting for a new build.",
                    lastBuild.getNumber());
            return;
        }

        log.warn("Build #{} FAILED — starting AI analysis...", lastBuild.getNumber());

        // Step 4: Fetch the console log
        String consoleLog = jenkinsClient.getConsoleLog(lastBuild.getNumber());

        // Step 5: Send to AI agent for analysis
        BuildAnalysisResult analysis = buildAnalysisAgent.analyse(
                lastBuild.getNumber(), consoleLog);

        // Step 6: Publish the fix report
        fixReportService.publishReport(analysis);

        // Step 7: Mark as analysed
        lastAnalysedBuildNumber = lastBuild.getNumber();

        // Step 8 (future): Auto-fix via Git
        if (monitorProperties.isAutoFixEnabled()) {
            log.info("Auto-fix is enabled — Git integration will run here (not yet implemented).");
            // gitFixAgent.applyFix(analysis);
        }
    }
}
