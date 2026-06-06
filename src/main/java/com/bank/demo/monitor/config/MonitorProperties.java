package com.bank.demo.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the monitoring scheduler and output behaviour.
 *
 * NOTE: No @Component here — registration is handled by @EnableConfigurationProperties
 * in AppConfig. Having both causes a duplicate bean definition error at startup.
 */
@Data
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    /** Poll interval in milliseconds (default 5 minutes = 300000 ms) */
    private long pollIntervalMs = 300_000;

    /** Directory to save fix reports as text files */
    private String outputDir = "./fix-reports";

    /** When true, the app will attempt to push fixes to Git automatically */
    private boolean autoFixEnabled = false;
}
