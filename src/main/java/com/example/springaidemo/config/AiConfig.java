package com.example.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DEMO SLIDE 1: Spring AI Configuration
 *
 * This is the only setup needed to wire up an LLM. Spring Boot auto-configures
 * the ChatModel and EmbeddingModel from application.yml — you just inject them.
 *
 * Python equivalent:
 *   llm = ChatOpenAI(base_url=os.getenv("LLM_URL"), api_key=os.getenv("API_KEY"))
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient is Spring AI's high-level LLM interface.
     * Think of it as a fluent builder around the raw ChatModel —
     * you can attach advisors (RAG, memory), tools, and system prompts to it.
     *
     * Python equivalent: a configured LangChain chain or LCEL expression
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant for our company's internal AI demo.")
                .build();
    }

    /**
     * In-memory conversation history.
     * A single shared instance — in production you'd scope this per session/user.
     *
     * Python equivalent: ConversationBufferMemory in LangChain
     */
    @Bean
    public InMemoryChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * SimpleVectorStore: in-memory vector store — no external DB needed for the demo.
     * EmbeddingModel is auto-configured from application.yml (same LLM server).
     *
     * Production alternatives: PgVector, Chroma, Pinecone, Weaviate, Redis
     *
     * Python equivalent: Chroma / FAISS in-memory from LangChain
     *
     * NOTE: SimpleVectorStore.builder() static factory was added after M6.
     * In 1.0.0-M6 use the constructor directly.
     */
    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }
}
