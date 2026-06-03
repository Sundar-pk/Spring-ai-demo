package com.bank.demo.monitor.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central Spring configuration class.
 *
 * Python analogy: like a module where you define your service instances
 * that get injected elsewhere (similar to FastAPI's Depends() or LangGraph's
 * compiled graph being passed around).
 *
 * @Configuration  = this class defines Spring beans (@Bean methods)
 * @Bean           = Spring manages this object's lifecycle (singleton by default)
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({JenkinsProperties.class, MonitorProperties.class, LlmProxyProperties.class})
public class AppConfig {

    private final LlmProxyProperties llmProxyProperties;

    public AppConfig(LlmProxyProperties llmProxyProperties) {
        this.llmProxyProperties = llmProxyProperties;
    }

    /**
     * Wires the corporate HTTP proxy at JVM level if enabled.
     *
     * Called once automatically after Spring constructs this bean (@PostConstruct).
     * Setting JVM system properties for http/https proxy is the standard Java way
     * to route all outbound HTTP — this covers Spring AI's LLM HTTP calls.
     *
     * Python analogy:
     *   os.environ["HTTPS_PROXY"] = "http://proxy.bank.internal:8080"
     */
    @PostConstruct
    public void configureProxy() {
        if (llmProxyProperties.isEnabled()) {
            String host = llmProxyProperties.getHost();
            String port = String.valueOf(llmProxyProperties.getPort());
            System.setProperty("http.proxyHost",  host);
            System.setProperty("http.proxyPort",  port);
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", port);
            log.info("Corporate HTTP proxy configured: {}:{}", host, port);
        } else {
            log.debug("LLM proxy disabled — direct connection will be used.");
        }
    }

    /**
     * RestClient is Spring's modern HTTP client (replaces old RestTemplate).
     * Python analogy: like requests.Session() — pre-configured HTTP client.
     *
     * We use this to call the Jenkins REST API (not the LLM — Spring AI handles that separately).
     */
    @Bean
    public RestClient jenkinsRestClient(JenkinsProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                // Jenkins uses HTTP Basic Auth: username + API token
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(props.getUsername(), props.getApiToken());
                    headers.set("Accept", "application/json");
                })
                .build();
    }

    /**
     * Ensures the fix-reports output directory exists at startup.
     * Python analogy: os.makedirs(output_dir, exist_ok=True)
     */
    @Bean
    public Path outputDirectory(MonitorProperties props) throws Exception {
        Path dir = Path.of(props.getOutputDir());
        Files.createDirectories(dir);
        log.info("Fix reports will be saved to: {}", dir.toAbsolutePath());
        return dir;
    }
}
