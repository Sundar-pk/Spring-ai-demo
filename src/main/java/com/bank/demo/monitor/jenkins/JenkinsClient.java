package com.bank.demo.monitor.jenkins;

import com.bank.demo.monitor.config.JenkinsProperties;
import com.bank.demo.monitor.model.BuildInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Handles all communication with the Jenkins REST API.
 *
 * Python analogy: this is like a dedicated class (or module) using requests.Session
 * to talk to Jenkins — separates HTTP concerns from business logic.
 *
 * @Service — Spring manages this as a singleton; inject it wherever needed.
 * @RequiredArgsConstructor (Lombok) — auto-generates a constructor for all 'final' fields,
 *   which Spring uses for dependency injection (no @Autowired needed).
 * @Slf4j (Lombok) — gives us a 'log' variable (like Python's logging.getLogger(__name__))
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsClient {

    private final RestClient jenkinsRestClient;     // injected from AppConfig
    private final JenkinsProperties jenkinsProps;   // injected from application.yml

    /**
     * Fetches info about the last completed build for the configured job.
     *
     * Jenkins REST API endpoint:
     *   GET /job/{jobName}/lastBuild/api/json
     *
     * Returns null if the request fails (network error, job not found, etc.)
     */
    public BuildInfo getLastBuild() {
        String path = buildPath("lastBuild/api/json");
        log.debug("Fetching last build info from Jenkins: {}", path);

        try {
            BuildInfo build = jenkinsRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(BuildInfo.class);  // Spring auto-deserializes JSON → BuildInfo

            if (build != null) {
                log.info("Last build: #{} — result={}", build.getNumber(), build.getResult());
            }
            return build;

        } catch (RestClientException e) {
            log.error("Failed to fetch Jenkins build info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetches the console log for a specific build number.
     * Truncates to the configured max lines to respect LLM context window.
     *
     * Jenkins REST API endpoint:
     *   GET /job/{jobName}/{buildNumber}/consoleText
     */
    public String getConsoleLog(int buildNumber) {
        String path = buildPath(buildNumber + "/consoleText");
        log.debug("Fetching console log for build #{} from: {}", buildNumber, path);

        try {
            String fullLog = jenkinsRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);  // raw text response

            if (fullLog == null || fullLog.isBlank()) {
                return "No console output available.";
            }

            // Truncate to last N lines — the most relevant part for failure analysis
            // Python equivalent: '\n'.join(lines[-max_lines:])
            String[] lines = fullLog.split("\n");
            int maxLines = jenkinsProps.getLogMaxLines();

            if (lines.length <= maxLines) {
                return fullLog;
            }

            String truncated = Arrays.stream(lines)
                    .skip(lines.length - maxLines)
                    .collect(Collectors.joining("\n"));

            log.info("Console log truncated from {} to {} lines", lines.length, maxLines);
            return String.format("[Log truncated — showing last %d of %d lines]\n\n%s",
                    maxLines, lines.length, truncated);

        } catch (RestClientException e) {
            log.error("Failed to fetch console log for build #{}: {}", buildNumber, e.getMessage());
            return "ERROR: Could not retrieve console log — " + e.getMessage();
        }
    }

    /**
     * Builds the URI path for a Jenkins job, handling folder paths correctly.
     * "folder/job-name" → "/job/folder/job/job-name/..."
     *
     * Python analogy: a helper function that constructs the URL path.
     */
    private String buildPath(String suffix) {
        // Jenkins encodes nested jobs as /job/folder/job/jobname
        String jobPath = Arrays.stream(jenkinsProps.getJobName().split("/"))
                .map(segment -> "job/" + segment)
                .collect(Collectors.joining("/"));
        return "/" + jobPath + "/" + suffix;
    }
}
