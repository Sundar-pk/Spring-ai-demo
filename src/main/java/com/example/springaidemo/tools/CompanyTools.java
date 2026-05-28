package com.example.springaidemo.tools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

/**
 * DEMO MODULE 3: Tool / Function Calling
 *
 * In Spring AI, a "tool" is a plain Java function exposed as a Spring Bean
 * with the @Description annotation. The LLM decides when and whether to call
 * these tools based on the user's request.
 *
 * Python equivalents:
 *   @tool decorator in LangGraph / LangChain
 *   @tool in CrewAI
 *
 * How it works under the hood:
 *   1. ChatClient sends the tool schemas (name + description + input shape) to the LLM.
 *   2. LLM replies with a tool_call JSON if it needs a tool.
 *   3. Spring AI auto-invokes the matching bean.
 *   4. The tool result is sent back to the LLM in a follow-up request.
 *   5. LLM produces the final answer — all transparent to the caller.
 */
@Configuration
public class CompanyTools {

    // ─────────────────────────────────────────────────────────────────────────
    // Tool 1: Current Date & Time
    // The description IS the tool documentation the LLM reads to decide when to use it
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Description("Get the current date and time. Use this when the user asks about today's date or current time.")
    public Function<CurrentTimeRequest, CurrentTimeResponse> getCurrentTime() {
        return request -> {
            String formatted = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy, HH:mm:ss"));
            return new CurrentTimeResponse(formatted);
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tool 2: Employee Directory Lookup
    // Simulates a real enterprise integration (e.g., HR system API call)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Description("Look up an employee's information by their employee ID or name. Returns their department, role, and manager.")
    public Function<EmployeeRequest, EmployeeResponse> lookupEmployee() {
        // Simulated employee directory — in production this would call an HR API
        Map<String, EmployeeResponse> directory = Map.of(
            "EMP001", new EmployeeResponse("EMP001", "Alice Chen",     "Engineering",    "Senior Engineer",     "Bob Kumar"),
            "EMP002", new EmployeeResponse("EMP002", "Bob Kumar",      "Engineering",    "Engineering Manager", "Carol Smith"),
            "EMP003", new EmployeeResponse("EMP003", "Carol Smith",    "Technology",     "VP of Technology",    "CEO"),
            "EMP004", new EmployeeResponse("EMP004", "David Okonkwo",  "Data Platform",  "Data Architect",      "Carol Smith"),
            "EMP005", new EmployeeResponse("EMP005", "Eva Rodriguez",  "AI Platform",    "ML Engineer",         "Bob Kumar")
        );

        return request -> {
            // Search by ID or partial name match
            return directory.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(request.query()) ||
                                 e.getValue().name().toLowerCase().contains(request.query().toLowerCase()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(new EmployeeResponse("N/A", "Not found", "N/A", "N/A", "N/A"));
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tool 3: Project Status
    // Simulates a project tracking system integration
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Description("Get the current status of an internal project by its name or project code. Returns status, deadline, and team lead.")
    public Function<ProjectRequest, ProjectResponse> getProjectStatus() {
        Map<String, ProjectResponse> projects = Map.of(
            "AI-PLATFORM",   new ProjectResponse("AI-PLATFORM",   "Internal LLM Deployment",    "IN_PROGRESS", "2025-09-30", "Eva Rodriguez",  "On track"),
            "DATA-MESH",     new ProjectResponse("DATA-MESH",     "Data Mesh Migration",         "IN_PROGRESS", "2025-12-31", "David Okonkwo",  "At risk - 2 weeks behind"),
            "SPRING-DEMO",   new ProjectResponse("SPRING-DEMO",   "Spring AI Team Training",     "COMPLETED",   "2025-05-30", "Alice Chen",     "Delivered on time"),
            "SEC-HARDENING", new ProjectResponse("SEC-HARDENING", "Security Hardening Q3",       "PLANNED",     "2025-08-15", "Bob Kumar",      "Not yet started")
        );

        return request -> projects.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(request.projectName()) ||
                             e.getValue().fullName().toLowerCase().contains(request.projectName().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(new ProjectResponse("N/A", "Project not found", "UNKNOWN", "N/A", "N/A", "N/A"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tool input/output records — these define the JSON schema the LLM sees
    // ─────────────────────────────────────────────────────────────────────────

    public record CurrentTimeRequest(String timezone) {}
    public record CurrentTimeResponse(String currentDateTime) {}

    public record EmployeeRequest(
            @Description("Employee ID (e.g. EMP001) or partial name to search for")
            String query
    ) {}

    public record EmployeeResponse(
            String employeeId, String name, String department, String role, String manager
    ) {}

    public record ProjectRequest(
            @Description("Project code (e.g. AI-PLATFORM) or partial project name")
            String projectName
    ) {}

    public record ProjectResponse(
            String code, String fullName, String status,
            String deadline, String teamLead, String notes
    ) {}
}
