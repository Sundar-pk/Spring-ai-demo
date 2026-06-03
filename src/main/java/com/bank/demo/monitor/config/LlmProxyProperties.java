package com.bank.demo.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the corporate HTTP proxy that routes LLM traffic.
 *
 * When your internally hosted LLM is behind a firewall and can only be reached
 * via an HTTP proxy, set these values in your .env file:
 *   LLM_PROXY_ENABLED=true
 *   LLM_PROXY_HOST=proxy.bank.internal
 *   LLM_PROXY_PORT=8080
 *
 * AppConfig reads this and sets JVM system properties so ALL outbound HTTP
 * from this application routes through the proxy — including Spring AI's
 * calls to the LLM endpoint.
 *
 * Python analogy:
 *   proxies = {"http": "http://proxy:8080", "https": "http://proxy:8080"}
 *   requests.get(url, proxies=proxies)
 */
@Data
@ConfigurationProperties(prefix = "llm.proxy")
public class LlmProxyProperties {

    private boolean enabled = false;
    private String host = "proxy.bank.internal";
    private int port = 8080;
}
