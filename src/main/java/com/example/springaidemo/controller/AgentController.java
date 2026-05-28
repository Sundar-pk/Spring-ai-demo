package com.example.springaidemo.controller;

import com.example.springaidemo.model.ApiResponse;
import com.example.springaidemo.model.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * DEMO MODULE 3: Tool / Function Calling (Agent-style interactions)
 *
 * This shows how Spring AI enables the LLM to call Java functions when needed.
 * The LLM reads tool descriptions and decides which tools to invoke — you
 * don't hardcode the tool selection.
 *
 * Python equivalents:
 *   LangGraph: define tools → bind to model → create_react_agent(model, tools)
 *   CrewAI:    @tool decorator + Agent(tools=[...])
 *   LangChain: tool = StructuredTool.from_function(fn) + AgentExecutor
 *
 * Key difference from Python: tools are Spring beans — they're injectable,
 * testable, and can use @Autowired dependencies (DB, HTTP clients, etc.)
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "2. Tool Calling (Agents)", description = "Function calling — LLM decides which Java methods to invoke")
public class AgentController {

    private final ChatClient chatClient;

    public AgentController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 3A: Single tool
    //
    // Try asking: "What time is it right now?"
    // The LLM will invoke the getCurrentTime bean automatically.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/time")
    @Operation(
        summary = "Chat with time tool",
        description = "LLM calls getCurrentTime() tool when needed. Try: 'What time is it?'"
    )
    public ApiResponse<String> chatWithTimeTool(@RequestBody ChatRequest request) {

        String response = chatClient.prompt()
                .user(request.message())
                // Register the Spring bean by name — Spring AI fetches its @Description
                // and sends the schema to the LLM automatically
                .functions("getCurrentTime")
                .call()
                .content();

        return ApiResponse.of(
                "Single Tool Call",
                response,
                "@tool def get_current_time() + bind_tools([get_current_time])"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 3B: Multiple tools — LLM picks the right one(s)
    //
    // Try asking:
    //   "Who is Alice Chen and what projects is she working on?"
    //   "What's the status of the AI Platform project and who leads it?"
    //   "What time is it and what's the status of the Data Mesh project?"
    //
    // The LLM may call multiple tools in sequence or parallel depending on the question.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/multi-tool")
    @Operation(
        summary = "Chat with multiple tools (LLM chooses)",
        description = "LLM picks from: getCurrentTime, lookupEmployee, getProjectStatus. " +
                      "Try: 'What is EMP004 working on and what is the current time?'"
    )
    public ApiResponse<String> chatWithMultipleTools(@RequestBody ChatRequest request) {

        String response = chatClient.prompt()
                .system("""
                        You are a helpful company assistant with access to our internal systems.
                        Use the available tools to answer questions accurately.
                        Always mention which tools you used in your response.
                        """)
                .user(request.message())
                // All three tools are available — LLM decides which to call
                .functions("getCurrentTime", "lookupEmployee", "getProjectStatus")
                .call()
                .content();

        return ApiResponse.of(
                "Multi-Tool Agent",
                response,
                "create_react_agent(model, [time_tool, employee_tool, project_tool])"
        );
    }
}
