package com.bank.demo.monitor.agent;

import com.bank.demo.monitor.config.JenkinsProperties;
import com.bank.demo.monitor.model.BuildAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ============================================================
 * THE CORE SPRING AI AGENT
 * ============================================================
 *
 * This is the heart of the demo — this is where Spring AI shines.
 *
 * Python / LangGraph analogy mapping:
 * ┌──────────────────────────────┬──────────────────────────────────────┐
 * │ Python / LangGraph           │ Spring AI equivalent                 │
 * ├──────────────────────────────┼──────────────────────────────────────┤
 * │ ChatOpenAI(...)              │ ChatClient (auto-configured by       │
 * │                              │ Spring AI from application.yml)      │
 * ├──────────────────────────────┼──────────────────────────────────────┤
 * │ SystemMessage / HumanMessage │ SystemMessage / UserMessage          │
 * ├──────────────────────────────┼──────────────────────────────────────┤
 * │ structured output (Pydantic) │ .entity(BuildAnalysisResult.class)   │
 * │                              │ (Spring AI auto-generates JSON schema│
 * │                              │ and instructs the LLM)               │
 * ├──────────────────────────────┼──────────────────────────────────────┤
 * │ chain = prompt | llm | parser│ ChatClient fluent builder chain      │
 * └──────────────────────────────┴──────────────────────────────────────┘
 *
 * Spring AI key concept: ChatClient is the main entry point.
 * It is auto-configured from application.yml (spring.ai.openai.*).
 * You inject ChatClient.Builder and call .build() to get a configured client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildAnalysisAgent {

    /**
     * ChatClient.Builder is injected by Spring AI automatically.
     * Think of it as a factory that creates configured ChatClient instances.
     *
     * Python analogy: like having a pre-configured LLM instance handed to you
     * by your DI container — no manual instantiation needed.
     */
    private final ChatClient.Builder chatClientBuilder;
    private final JenkinsProperties jenkinsProperties;

    // System prompt — the "persona" and instructions for the LLM
    // Python analogy: the system message in your LangChain/LangGraph agent
    private static final String SYSTEM_PROMPT = """
            You are an expert DevOps engineer and software architect specializing in CI/CD pipeline troubleshooting.
            Your task is to analyze Jenkins build failure logs and provide actionable fixes.

            Guidelines:
            - Be precise and specific — avoid vague suggestions
            - Identify the ROOT CAUSE, not just symptoms
            - If you see a stack trace, identify the exact failing line
            - Suggest concrete code changes where possible
            - Consider common causes: missing dependencies, config errors, test failures, network issues
            - If the log is unclear, say so honestly (confidence: LOW)

            You MUST respond with valid JSON only. No markdown, no explanation outside JSON.
            """;

    /**
     * Analyses a Jenkins build failure log using the configured LLM.
     *
     * @param buildNumber  Jenkins build number (for metadata)
     * @param consoleLog   The raw Jenkins console output
     * @return             Structured analysis with fix suggestions
     */
    public BuildAnalysisResult analyse(int buildNumber, String consoleLog) {
        log.info("Sending build #{} log to LLM for analysis ({} chars)...",
                buildNumber, consoleLog.length());

        // Build the user message — what we're asking the LLM to do
        // Python analogy: HumanMessage(content=f"Analyse this log: {log}")
        String userPrompt = String.format("""
                Analyse this Jenkins build failure for job '%s', build #%d.

                Provide your analysis in the required JSON format with:
                - failureSummary: one-line description of what failed
                - rootCauseCategory: one of [COMPILATION_ERROR, TEST_FAILURE, DEPENDENCY_ERROR,
                  CONFIGURATION_ERROR, INFRASTRUCTURE_ERROR, UNKNOWN]
                - confidence: one of [HIGH, MEDIUM, LOW]
                - fixSteps: ordered list of steps to fix the issue
                - suggestedCodeFix: code snippet if code changes are needed (null otherwise)
                - affectedFiles: list of file paths that need changes (empty list if none)
                - additionalNotes: any warnings or extra context

                CONSOLE LOG:
                ---
                %s
                ---
                """,
                jenkinsProperties.getJobName(),
                buildNumber,
                consoleLog
        );

        try {
            /*
             * ============================================================
             * SPRING AI MAGIC — this is what to highlight to your team
             * ============================================================
             *
             * .prompt(new Prompt(...))   — builds the message list (system + user)
             * .call()                    — sends HTTP request to your LLM
             * .entity(Class)             — Spring AI auto-generates JSON schema
             *                             from the class, adds it to the prompt,
             *                             then deserializes the response into the object
             *
             * The entire prompt → LLM → structured object pipeline is ONE chain.
             * No manual JSON parsing. No schema writing. Spring AI does it all.
             *
             * Python equivalent:
             *   chain = prompt_template | llm.with_structured_output(BuildAnalysisResult)
             *   result = chain.invoke({"log": console_log})
             */
            ChatClient chatClient = chatClientBuilder.build();

            BuildAnalysisResult result = chatClient
                    .prompt(new Prompt(
                            new SystemMessage(SYSTEM_PROMPT),
                            new UserMessage(userPrompt)
                    ))
                    .call()
                    .entity(BuildAnalysisResult.class);  // ← Structured Output

            // Populate app-level metadata (not from LLM)
            if (result != null) {
                result.setBuildNumber(buildNumber);
                result.setJobName(jenkinsProperties.getJobName());
                result.setAnalysedAt(LocalDateTime.now());
                log.info("Analysis complete. Category: {}, Confidence: {}",
                        result.getRootCauseCategory(), result.getConfidence());
            }

            return result;

        } catch (Exception e) {
            log.error("LLM analysis failed for build #{}: {}", buildNumber, e.getMessage(), e);

            // Return a fallback result so the app doesn't crash
            BuildAnalysisResult fallback = new BuildAnalysisResult();
            fallback.setBuildNumber(buildNumber);
            fallback.setJobName(jenkinsProperties.getJobName());
            fallback.setAnalysedAt(LocalDateTime.now());
            fallback.setFailureSummary("LLM analysis failed: " + e.getMessage());
            fallback.setRootCauseCategory("UNKNOWN");
            fallback.setConfidence("LOW");
            return fallback;
        }
    }
}
