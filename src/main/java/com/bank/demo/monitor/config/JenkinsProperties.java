package com.bank.demo.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed config bean — Spring reads application.yml jenkins.* block into this class.
 *
 * Python analogy: like a Pydantic BaseSettings model that reads from env vars / config files.
 *
 * @ConfigurationProperties(prefix = "jenkins") means Spring maps:
 *   jenkins.base-url  →  this.baseUrl
 *   jenkins.job-name  →  this.jobName
 *   ... etc.
 *
 * @Data (Lombok) auto-generates getters, setters, toString, equals, hashCode.
 *
 * NOTE: No @Component here — registration is handled by @EnableConfigurationProperties
 * in AppConfig. Having both causes a duplicate bean definition error at startup.
 */
@Data
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsProperties {

    /** Jenkins server base URL, e.g. http://jenkins.bank.internal:8080 */
    private String baseUrl;

    /** Job name or folder path, e.g. "payments/nightly-build" */
    private String jobName;

    /** Jenkins username */
    private String username;

    /** Jenkins API token (NOT your password — generate from Jenkins UI) */
    private String apiToken;

    /** Max console log lines to send to LLM to avoid exceeding context window */
    private int logMaxLines = 200;
}
