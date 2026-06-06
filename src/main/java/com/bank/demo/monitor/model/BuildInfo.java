package com.bank.demo.monitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Jenkins build — mapped from Jenkins REST API JSON response.
 *
 * Python analogy: like a dataclass or Pydantic model with field mapping from JSON.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) — Jenkins returns many fields;
 * we only capture what we need and ignore the rest.
 *
 * @NoArgsConstructor — required by Jackson for deserialization (no-arg constructor
 * is needed when Jackson maps JSON → Java object).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildInfo {

    /** Build number, e.g. 42 */
    private int number;

    /**
     * Jenkins build result. Possible values:
     * "SUCCESS", "FAILURE", "UNSTABLE", "ABORTED", null (still running)
     */
    private String result;

    /** Build URL on Jenkins server */
    private String url;

    /** Build duration in milliseconds */
    private long duration;

    /** Build timestamp (Unix epoch ms) */
    private long timestamp;

    /** Display name, e.g. "#42" */
    private String displayName;

    /** Description set on the build (may be null) */
    private String description;

    /** Convenience method — Python equivalent: @property */
    public boolean isFailed() {
        return "FAILURE".equals(result);
    }

    public boolean isStillRunning() {
        return result == null;
    }
}
