package com.example.springaidemo.controller;

import com.example.springaidemo.model.ApiResponse;
import com.example.springaidemo.model.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DEMO MODULE 4: Retrieval Augmented Generation (RAG)
 *
 * RAG pattern: embed documents → store in vector DB → on query, retrieve
 * relevant chunks → inject into LLM context → LLM answers grounded in docs.
 *
 * Python equivalents:
 *   LangChain:  FAISS.from_documents(docs) + RetrievalQA.from_chain_type()
 *   LlamaIndex: VectorStoreIndex.from_documents(docs) + index.as_query_engine()
 *
 * Spring AI equivalent:
 *   SimpleVectorStore + QuestionAnswerAdvisor (one-liner RAG integration)
 *
 * The QuestionAnswerAdvisor is an "advisor" — it intercepts each ChatClient call,
 * runs a similarity search automatically, and stuffs the results into the prompt.
 * You don't manually build the RAG prompt — it's handled for you.
 */
@RestController
@RequestMapping("/api/rag")
@Tag(name = "3. RAG (Retrieval Augmented Generation)",
     description = "Load docs → embed → retrieve → answer — Python equivalent: FAISS + RetrievalQA")
public class RagController {

    private final ChatClient chatClient;
    private final SimpleVectorStore vectorStore;
    private boolean documentsLoaded = false;

    public RagController(ChatClient chatClient, SimpleVectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Auto-load the knowledge base on startup so the demo is ready immediately.
     *
     * Python equivalent:
     *   loader = TextLoader("knowledge-base.txt")
     *   docs = loader.load()
     *   text_splitter = RecursiveCharacterTextSplitter(chunk_size=500)
     *   chunks = text_splitter.split_documents(docs)
     *   vectorstore = FAISS.from_documents(chunks, embeddings)
     */
    @PostConstruct
    public void loadKnowledgeBase() throws IOException {
        var resource = new ClassPathResource("rag-documents/knowledge-base.txt");
        String fullText = resource.getContentAsString(StandardCharsets.UTF_8);

        // Split by section headers — a simple but effective chunking strategy
        // In production use: TokenTextSplitter or SemanticChunker
        List<Document> documents = chunkBySections(fullText);

        vectorStore.add(documents);
        documentsLoaded = true;

        System.out.printf("[RAG] Loaded %d document chunks into vector store%n", documents.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 4A: Ask the knowledge base
    //
    // QuestionAnswerAdvisor does all of this automatically:
    //   1. Embeds the user question using the embedding model
    //   2. Searches the vector store for the top-K most similar chunks
    //   3. Injects retrieved chunks into the system prompt as context
    //   4. LLM answers using that grounded context
    //
    // Try asking:
    //   "How do I request access to the AI Platform?"
    //   "What are the data classification rules?"
    //   "What is the rate limit for the LLM?"
    //   "Tell me about something not in the docs" ← LLM says it doesn't know
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/ask")
    @Operation(
        summary = "Ask questions answered from the knowledge base",
        description = "RAG pipeline: embed query → vector search → augment prompt → LLM answers from docs. " +
                      "Try: 'How do I get access to the AI Platform?'"
    )
    public ApiResponse<Map<String, Object>> askKnowledgeBase(@RequestBody ChatRequest request) {

        // QuestionAnswerAdvisor is the key — it's the RAG advisor.
        // It auto-embeds the user query, searches the vector store, and
        // injects the top matching chunks into the system prompt context.
        //
        // NOTE: In 1.0.0-M6, use SearchRequest.defaults() without chaining withTopK()
        // on the same line — the topK is set separately or left at the default (4).
        var ragAdvisor = new QuestionAnswerAdvisor(
                vectorStore,
                SearchRequest.defaults()
        );

        String answer = chatClient.prompt()
                .system("""
                        You are a helpful assistant that answers questions about company AI policies.
                        Answer ONLY based on the provided context.
                        If the answer is not in the context, say "I don't have information about that
                        in the knowledge base." Do not make up answers.
                        """)
                .user(request.message())
                .advisors(ragAdvisor)   // ← This is the entire RAG pipeline
                .call()
                .content();

        return ApiResponse.of(
                "RAG Query",
                Map.of(
                    "question", request.message(),
                    "answer",   answer,
                    "status",   documentsLoaded ? "Knowledge base loaded" : "No documents loaded"
                ),
                "RetrievalQA.from_chain_type(llm, retriever=vectorstore.as_retriever())"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 4B: Inspect what's in the vector store
    // Shows which chunks were created — useful for understanding chunking
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/search")
    @Operation(
        summary = "Raw vector similarity search (no LLM)",
        description = "Shows the raw retrieved chunks before the LLM sees them. " +
                      "Helps understand what context the LLM receives."
    )
    public ApiResponse<List<Map<String, String>>> similaritySearch(@RequestParam String query) {

        // In 1.0.0-M6: SearchRequest.query(text) sets the query string used for embedding.
        // withTopK(3) is available but keep it separate from the factory call if needed.
        List<Map<String, String>> results = vectorStore
                .similaritySearch(SearchRequest.query(query).withTopK(3))
                .stream()
                .map(doc -> Map.of(
                        "content",  doc.getContent().substring(0, Math.min(200, doc.getContent().length())) + "...",
                        "metadata", doc.getMetadata().toString()
                ))
                .toList();

        return ApiResponse.of(
                "Vector Similarity Search",
                results,
                "vectorstore.similarity_search(query, k=3)"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMO 4C: Load custom text at runtime
    // Shows how you'd add new documents dynamically (e.g., from an upload)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/load")
    @Operation(
        summary = "Dynamically add text to the knowledge base",
        description = "Embeds and stores text at runtime — simulates ingesting a new document"
    )
    public ApiResponse<String> loadDocument(@RequestBody ChatRequest request) {

        var document = new Document(
                request.message(),
                Map.of("source", "runtime-upload", "addedAt", java.time.Instant.now().toString())
        );
        vectorStore.add(List.of(document));

        return ApiResponse.of(
                "Document Ingestion",
                "Document embedded and stored. It's now searchable via /api/rag/ask",
                "vectorstore.add_documents([Document(page_content=text)])"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simple section-based chunking — splits by "== SECTION" headers
    // ─────────────────────────────────────────────────────────────────────────
    private List<Document> chunkBySections(String text) {
        List<Document> docs = new ArrayList<>();
        String[] sections = text.split("(?=SECTION \\d+:)");

        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            if (section.isBlank()) continue;

            // Extract a title from the first line
            String title = section.lines().findFirst().orElse("Section " + i);

            docs.add(new Document(
                    section,
                    Map.of("source", "knowledge-base.txt", "section", title.trim())
            ));
        }

        // Fallback: if no sections found, add the whole thing as one doc
        if (docs.isEmpty()) {
            docs.add(new Document(text, Map.of("source", "knowledge-base.txt")));
        }

        return docs;
    }
}
