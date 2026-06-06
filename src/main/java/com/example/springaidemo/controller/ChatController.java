package com.example.springaidemo.controller;

import com.example.springaidemo.model.ApiResponse;
import com.example.springaidemo.model.ChatRequest;
import com.example.springaidemo.model.TemplateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * DEMO MODULE 1 & 2: Simple Chat + Prompt Templates
 *
 * Covers the fundamental Spring AI building blocks most engineers use daily.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "1. Chat & Prompts", description = "Core ChatClient API and Prompt Templates")
public class ChatController {

    private final ChatClient chatClient;
    private final InMemoryChatMemory chatMemory;

    public ChatController(ChatClient chatClient, InMemoryChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 1A: Simple Chat
    //
    // Python equivalent:
    //   response = chat_model.invoke([HumanMessage(content=message)])
    //   print(response.content)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/simple")
    @Operation(
        summary = "Simple one-shot chat",
        description = "Basic ChatClient call — Python equivalent: chat_model.invoke([HumanMessage(content=msg)])"
    )
    public ApiResponse<String> simpleChat(@RequestBody ChatRequest request) {

        // ChatClient fluent API: build prompt → call → extract content
        String response = chatClient.prompt()
                .user(request.message())   // user message
                .call()                    // synchronous call to LLM
                .content();                // extract String from response

        return ApiResponse.of(
                "Simple Chat",
                response,
                "chat_model.invoke([HumanMessage(content=msg)]).content"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 1B: Prompt Templates
    //
    // Structured prompts with named variables — same concept as LangChain
    // PromptTemplate("Explain {topic} to {audience}").format(...)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/template")
    @Operation(
        summary = "Prompt template with variables",
        description = "Structured prompts — Python equivalent: PromptTemplate('...{topic}...').format(topic=...)"
    )
    public ApiResponse<String> templateChat(@RequestBody TemplateRequest request) {

        // PromptTemplate uses {variable} placeholders — same as LangChain
        var template = new PromptTemplate("""
                You are an expert technical trainer.

                Explain the concept of "{topic}" to someone who is a {audience}.
                Present your explanation in {format}.
                Keep it concise and practical.
                """);

        // Render the template by filling in variable values
        var prompt = template.create(Map.of(
                "topic",    request.topic()    != null ? request.topic()    : "Spring AI",
                "audience", request.audience() != null ? request.audience() : "Java developer",
                "format",   request.format()   != null ? request.format()   : "3 bullet points"
        ));

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        return ApiResponse.of(
                "Prompt Template",
                response,
                "PromptTemplate(template).format(topic=..., audience=..., format=...)"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 1C: Streaming
    //
    // Returns Server-Sent Events (SSE) — tokens arrive as they're generated.
    // Python equivalent: chat_model.stream([HumanMessage(content=msg)])
    //
    // Test with: curl -N http://localhost:8080/api/chat/stream?message=Tell+me+a+joke
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Streaming response (SSE)",
        description = "Token-by-token streaming — Python equivalent: for chunk in chat_model.stream(messages)"
    )
    public Flux<String> streamChat(@RequestParam String message) {

        // .stream() returns a Flux<String> — each item is a token chunk
        return chatClient.prompt()
                .user(message)
                .stream()      // ← the only difference from .call()
                .content();    // Flux<String> instead of String
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 1D: Conversational Memory
    //
    // MessageChatMemoryAdvisor automatically injects past messages into each prompt.
    // Python equivalent: ConversationBufferMemory + LLMChain
    //
    // Try: Ask "My name is Alex" then ask "What is my name?" with the same conversationId
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/memory")
    @Operation(
        summary = "Multi-turn chat with memory",
        description = "Automatic conversation history — Python equivalent: ConversationBufferMemory + LLMChain"
    )
    public ApiResponse<String> chatWithMemory(@RequestBody ChatRequest request) {

        // MessageChatMemoryAdvisor intercepts each call:
        //   Before: retrieves history for this conversationId and prepends it
        //   After:  saves the new exchange back to memory
        //
        // NOTE: MessageChatMemoryAdvisor.builder() was added after M6.
        // In 1.0.0-M6 use the constructor: (chatMemory, conversationId, windowSize)
        // windowSize=10 means it keeps the last 10 message pairs in context.
        String response = chatClient.prompt()
                .user(request.message())
                .advisors(
                    new MessageChatMemoryAdvisor(chatMemory, request.conversationId(), 10)
                )
                .call()
                .content();

        return ApiResponse.of(
                "Conversational Memory",
                response,
                "ConversationBufferMemory + LLMChain (LangChain) | MemorySaver (LangGraph)"
        );
    }
}
