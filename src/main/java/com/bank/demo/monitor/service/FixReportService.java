package com.bank.demo.monitor.service;

import com.bank.demo.monitor.model.BuildAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders and saves the AI analysis result as a human-readable report.
 *
 * Python analogy: a utility module that formats output and writes files.
 * Like having a separate reporter.py that formats your dict/dataclass to text.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixReportService {

    private final Path outputDirectory;  // injected from AppConfig bean

    private static final DateTimeFormatter FILE_TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Prints the fix report to console AND saves it to a file.
     */
    public void publishReport(BuildAnalysisResult result) {
        String report = formatReport(result);

        // 1. Print to console — visible immediately
        System.out.println("\n" + "=".repeat(80));
        System.out.println(report);
        System.out.println("=".repeat(80) + "\n");

        // 2. Save to file for record-keeping
        saveToFile(result, report);
    }

    /**
     * Formats the analysis result as a readable text report.
     * Python analogy: an f-string or template render function.
     */
    private String formatReport(BuildAnalysisResult r) {
        StringBuilder sb = new StringBuilder();

        sb.append("JENKINS BUILD FAILURE ANALYSIS REPORT\n");
        sb.append("=".repeat(80)).append("\n\n");

        sb.append(String.format("Job          : %s\n", r.getJobName()));
        sb.append(String.format("Build #      : %d\n", r.getBuildNumber()));
        sb.append(String.format("Analysed at  : %s\n",
                r.getAnalysedAt() != null ? r.getAnalysedAt().format(DISPLAY_FORMAT) : "N/A"));
        sb.append(String.format("Confidence   : %s\n", r.getConfidence()));
        sb.append(String.format("Root Cause   : %s\n", r.getRootCauseCategory()));
        sb.append("\n");

        sb.append("FAILURE SUMMARY\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append(r.getFailureSummary()).append("\n\n");

        // Fix steps — numbered list
        if (r.getFixSteps() != null && !r.getFixSteps().isEmpty()) {
            sb.append("FIX STEPS\n");
            sb.append("-".repeat(40)).append("\n");
            List<String> steps = r.getFixSteps();
            for (int i = 0; i < steps.size(); i++) {
                sb.append(String.format("  %d. %s\n", i + 1, steps.get(i)));
            }
            sb.append("\n");
        }

        // Affected files
        if (r.getAffectedFiles() != null && !r.getAffectedFiles().isEmpty()) {
            sb.append("AFFECTED FILES\n");
            sb.append("-".repeat(40)).append("\n");
            r.getAffectedFiles().forEach(f -> sb.append("  - ").append(f).append("\n"));
            sb.append("\n");
        }

        // Code fix suggestion
        if (r.getSuggestedCodeFix() != null && !r.getSuggestedCodeFix().isBlank()) {
            sb.append("SUGGESTED CODE FIX\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(r.getSuggestedCodeFix()).append("\n\n");
        }

        // Additional notes
        if (r.getAdditionalNotes() != null && !r.getAdditionalNotes().isBlank()) {
            sb.append("ADDITIONAL NOTES\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(r.getAdditionalNotes()).append("\n");
        }

        return sb.toString();
    }

    private void saveToFile(BuildAnalysisResult result, String content) {
        try {
            String timestamp = result.getAnalysedAt() != null
                    ? result.getAnalysedAt().format(FILE_TS_FORMAT)
                    : "unknown";

            // Sanitize job name for filename (replace / and spaces)
            String safeJobName = result.getJobName()
                    .replaceAll("[/\\\\\\s]", "_")
                    .replaceAll("[^a-zA-Z0-9_-]", "");

            String fileName = String.format("fix_%s_build%d_%s.txt",
                    safeJobName, result.getBuildNumber(), timestamp);

            Path reportFile = outputDirectory.resolve(fileName);
            Files.writeString(reportFile, content);
            log.info("Fix report saved: {}", reportFile.toAbsolutePath());

        } catch (IOException e) {
            log.error("Could not save fix report to file: {}", e.getMessage());
        }
    }
}
