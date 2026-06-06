# Spring AI Demo — Internal LLM Integration

A 10-minute peer training demo for engineers familiar with Python AI libraries (LangChain, LangGraph, CrewAI).

---

## What is Spring AI?

Spring AI is Java/Spring's answer to LangChain — it provides:
- A unified abstraction over 20+ LLM providers
- Prompt templates, chat memory, tool calling, RAG
- Built on Spring Boot — familiar DI, configuration, testing patterns

| Python (LangChain / LangGraph) | Spring AI |
|---|---|
| `chat_model.invoke([HumanMessage(...)])` | `ChatClient.prompt().user(...).call().content()` |
| `PromptTemplate("{topic}")` | `new PromptTemplate("{topic}")` |
| `for chunk in model.stream(messages)` | `chatClient.prompt().stream().content()` (returns `Flux<String>`) |
| `ConversationBufferMemory` | `MessageChatMemoryAdvisor` |
| `@tool` decorator (LangGraph / CrewAI) | `@Bean @Description` Spring function |
| `FAISS.from_documents() + RetrievalQA` | `SimpleVectorStore + QuestionAnswerAdvisor` |

---

## Architecture

```
HTTP Request
     │
     ▼
REST Controller
     │
     ▼
ChatClient (fluent builder)
     │  ├── Advisors: QuestionAnswerAdvisor (RAG), MessageChatMemoryAdvisor (memory)
     │  ├── Functions: getCurrentTime, lookupEmployee, getProjectStatus (tool calling)
     │  └── PromptTemplate: structured prompts with variables
     │
     ▼
ChatModel (auto-configured by Spring Boot)
     │
     ▼  HTTP POST /v1/chat/completions  (OpenAI-compatible)
     ▼
Internal Company LLM Server
(vLLM / text-generation-inference / Azure OpenAI / Ollama)
```

---

## 10-Minute Demo Script

### Minute 0–1 | Setup & Configuration

Open `application.yml` — this is the **only place** you configure the LLM:

```yaml
spring.ai.openai:
  base-url: ${INTERNAL_LLM_URL}   # your internal server
  api-key:  ${INTERNAL_LLM_API_KEY}
  chat.options.model: ${INTERNAL_LLM_MODEL}
```

The `openai` starter works with **any OpenAI-compatible API** — not just OpenAI.
Most enterprise LLMs (vLLM, TGI, Azure) expose this same format.

Open `AiConfig.java` — just three beans: `ChatClient`, `InMemoryChatMemory`, `SimpleVectorStore`.

---

### Minute 1–3 | Demo 1: Simple Chat + Prompt Templates

**Endpoint:** `POST /api/chat/simple`

```bash
curl -X POST http://localhost:8080/api/chat/simple \
  -H "Content-Type: application/json" \
  -d '{"message": "What is Spring AI in one sentence?"}'
```

Core concept — `ChatClient` is the main interface:
```java
chatClient.prompt()
    .user(message)     // like HumanMessage
    .call()            // sync invoke
    .content()         // extract string
```

**Endpoint:** `POST /api/chat/template`

```bash
curl -X POST http://localhost:8080/api/chat/template \
  -H "Content-Type: application/json" \
  -d '{"topic": "RAG", "audience": "senior Java developer", "format": "3 bullet points"}'
```

**Endpoint:** `GET /api/chat/stream?message=Tell me a short story`

```bash
curl -N "http://localhost:8080/api/chat/stream?message=Tell+me+a+short+story"
```
Tokens stream back as SSE. Return type is `Flux<String>` — reactive, non-blocking.

**Endpoint:** `POST /api/chat/memory` — test memory with same `conversationId`:

```bash
# Turn 1
curl -X POST http://localhost:8080/api/chat/memory \
  -d '{"message": "My name is Sundar", "conversationId": "sess-1"}'

# Turn 2 — should remember "Sundar"
curl -X POST http://localhost:8080/api/chat/memory \
  -d '{"message": "What is my name?", "conversationId": "sess-1"}'
```

---

### Minute 3–6 | Demo 2: Tool / Function Calling

Open `CompanyTools.java` — tools are plain Spring `@Bean`s with `@Description`:

```java
@Bean
@Description("Look up an employee by ID or name")
public Function<EmployeeRequest, EmployeeResponse> lookupEmployee() {
    return request -> { /* call HR API here */ };
}
```

The `@Description` text is **what the LLM reads** to decide when to use the tool.
Input/output records define the **JSON schema** sent to the LLM automatically.

**Endpoint:** `POST /api/agent/multi-tool`

```bash
# LLM will call lookupEmployee + getProjectStatus tools
curl -X POST http://localhost:8080/api/agent/multi-tool \
  -H "Content-Type: application/json" \
  -d '{"message": "Who is EMP004 and what is the status of the AI Platform project?"}'

# LLM will call getCurrentTime + lookupEmployee
curl -X POST http://localhost:8080/api/agent/multi-tool \
  -H "Content-Type: application/json" \
  -d '{"message": "What time is it and who manages Alice Chen?"}'
```

In `AgentController.java`:
```java
chatClient.prompt()
    .user(message)
    .functions("getCurrentTime", "lookupEmployee", "getProjectStatus")  // register tools
    .call()
    .content()
// Spring AI handles the tool_call → invocation → result → final answer loop
```

**Key insight for the team:** You don't control *which* tool gets called — the LLM does.
This is the same as LangGraph's `create_react_agent` or CrewAI's agent loop.

---

### Minute 6–10 | Demo 3: RAG

The knowledge base (`rag-documents/knowledge-base.txt`) is auto-loaded on startup.

**Step 1 — See what's in the vector store:**

```bash
curl "http://localhost:8080/api/rag/search?query=how to get access"
```
Shows the raw chunks retrieved before the LLM sees them.

**Step 2 — Ask a question grounded in the docs:**

```bash
# Answered from the knowledge base
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"message": "What are the data classification rules for CONFIDENTIAL data?"}'

# Another one
curl -X POST http://localhost:8080/api/rag/ask \
  -d '{"message": "What is the rate limit for InternalLLM?"}'

# Question NOT in the docs — LLM should say it doesn't know
curl -X POST http://localhost:8080/api/rag/ask \
  -d '{"message": "What is the CEO salary?"}'
```

In `RagController.java` — the **entire RAG pipeline is one line**:
```java
chatClient.prompt()
    .user(question)
    .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(3)))
    .call()
    .content()
```

`QuestionAnswerAdvisor` auto-handles: embed query → similarity search → inject context → call LLM.

**Step 3 — Add a document at runtime:**
```bash
curl -X POST http://localhost:8080/api/rag/load \
  -H "Content-Type: application/json" \
  -d '{"message": "The new office in Singapore opens on July 1st 2025."}'

# Now ask about it
curl -X POST http://localhost:8080/api/rag/ask \
  -d '{"message": "When does the Singapore office open?"}'
```

---

## Running the Project

### Prerequisites
- Java 17+
- Maven 3.8+
- Access to an internal LLM with OpenAI-compatible API

### Quick Start

```bash
# 1. Set your LLM connection details
export INTERNAL_LLM_URL=http://your-llm-server/v1
export INTERNAL_LLM_API_KEY=your-key
export INTERNAL_LLM_MODEL=your-model-name

# 2. Run
./mvnw spring-boot:run

# 3. Open Swagger UI for interactive testing
open http://localhost:8080/swagger-ui.html
```

Or with a `.env` file:
```bash
cp .env.example .env
# Edit .env with your values
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

---

## Project Structure

```
src/main/java/com/example/springaidemo/
├── SpringAiDemoApplication.java   Main class + concept mapping table
├── config/
│   └── AiConfig.java              ChatClient, Memory, VectorStore beans
├── controller/
│   ├── ChatController.java        Demo 1: simple chat, templates, streaming, memory
│   ├── AgentController.java       Demo 2: tool/function calling
│   └── RagController.java         Demo 3: RAG pipeline
├── model/
│   ├── ChatRequest.java           Request DTO
│   ├── TemplateRequest.java       Template demo DTO
│   └── ApiResponse.java           Wrapper with pythonEquivalent field
└── tools/
    └── CompanyTools.java          Spring beans = LLM tools (employee, project, time)

src/main/resources/
├── application.yml                LLM config (base-url, api-key, model)
└── rag-documents/
    └── knowledge-base.txt         Company AI policy docs for RAG demo
```

---

## Key Talking Points for the Team

1. **OpenAI-compatible = universal adapter** — the same `spring-ai-openai-starter` works with your internal LLM as long as it exposes `/v1/chat/completions`. No vendor lock-in.

2. **Advisors = middleware for AI** — `QuestionAnswerAdvisor` (RAG) and `MessageChatMemoryAdvisor` (memory) are interceptors that enhance every call without changing your application code. Think of them like Spring AOP for AI.

3. **Tools are Spring beans** — your existing services, repositories, and HTTP clients are already injectable into tools. Zero extra framework to learn.

4. **Streaming is first-class** — just swap `.call()` for `.stream()` and return `Flux<String>`. Spring WebFlux handles the rest.

5. **Testability** — because everything is Spring beans, you can mock `ChatModel` in unit tests and test the full flow without a real LLM.

---

## Next Steps to Explore

- **Structured output** — `chatClient.prompt().call().entity(MyClass.class)` — LLM returns a Java object
- **Production vector stores** — swap `SimpleVectorStore` for PgVector (PostgreSQL extension)
- **Multimodal** — pass images alongside text with the same API
- **Evaluation** — Spring AI has built-in evaluation framework for measuring RAG quality
- **Observability** — integrates with Micrometer/OpenTelemetry for LLM metrics
