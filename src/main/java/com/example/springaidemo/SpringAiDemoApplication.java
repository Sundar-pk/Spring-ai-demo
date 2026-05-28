package com.example.springaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Demo Application
 *
 * Demonstrates key Spring AI concepts mapped to Python equivalents:
 *
 *   Python (LangChain/LangGraph)      Spring AI
 *   ─────────────────────────────     ─────────────────────────────
 *   chat_model.invoke(prompt)    →    ChatClient.prompt().call()
 *   PromptTemplate               →    PromptTemplate
 *   tool / @tool decorator       →    @Bean + @Description function
 *   VectorStore + RAG chain      →    VectorStore + QuestionAnswerAdvisor
 *   ConversationBufferMemory     →    MessageChatMemoryAdvisor
 *   chain | chain streaming      →    ChatClient.stream()
 *
 * LLM Connection: Uses OpenAI-compatible HTTP API (works with any internal LLM)
 * Configure via environment variables: INTERNAL_LLM_URL, INTERNAL_LLM_API_KEY, INTERNAL_LLM_MODEL
 */
@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
