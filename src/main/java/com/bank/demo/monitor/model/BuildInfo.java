package com.bank.demo.monitor.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a Jenkins build — mapped from Jenkins REST API JSON response.
 *
 * Python analogy: like a dataclass or Pydantic model with field mapping from JSON.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) — Jenkins returns many fields;
 * we only capture what we need and ignore the rest.
 */
@Data
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

    /** Convenience method — Python equivalent: @property */
    public boolean isFailed() {
        return "FAILURE".equals(result);
    }

    public boolean isStillRunning() {
        return result == null;
    }
}
