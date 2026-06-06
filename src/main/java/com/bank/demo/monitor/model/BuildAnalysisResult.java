package com.bank.demo.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Structured result returned by the AI analysis agent.
 *
 * Spring AI can map LLM responses directly into Java objects (Structured Output).
 * Python analogy: like Pydantic's model used as output_schema in LangChain/LangGraph.
 *
 * The LLM is instructed to return JSON matching this structure;
 * Spring AI deserializes it automatically.
 */
@Data
public class BuildAnalysisResult {

    /** Short one-line summary of what caused the failure */
    private String failureSummary;

    /** Root cause category: COMPILATION_ERROR, TEST_FAILURE, DEPENDENCY_ERROR,
     *  CONFIGURATION_ERROR, INFRASTRUCTURE_ERROR, UNKNOWN */
    private String rootCauseCategory;

    /** Confidence level of the analysis: HIGH, MEDIUM, LOW */
    private String confidence;

    /** Step-by-step fix instructions the developer should follow */
    private List<String> fixSteps;

    /**
     * If the fix involves code changes, this holds the suggested code snippet.
     * null if no code change is needed.
     */
    private String suggestedCodeFix;

    /** File path(s) that likely need to be changed */
    private List<String> affectedFiles;

    /** Additional context or warnings the AI wants to flag */
    private String additionalNotes;

    // --- fields populated by our app, not the LLM ---

    /** Jenkins build number this analysis is for */
    private int buildNumber;

    /** Jenkins job name */
    private String jobName;

    /**
     * When this analysis was generated.
     * @JsonFormat ensures this serializes as "2026-06-03T14:30:22" (ISO string),
     * not as a JSON array [2026,6,3,14,30,22] which is Jackson's default for LocalDateTime.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime analysedAt;
}
