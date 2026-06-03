# Jenkins AI Monitor — Spring AI Demo

> **A hands-on Spring AI showcase for teams upskilling from Python / LangGraph.**
> Watch Jenkins. Detect failures. Analyse logs with your hosted LLM. Get fix suggestions — automatically.

---

## Table of Contents

1. [What This Project Does](#what-this-project-does)
2. [Architecture at a Glance](#architecture-at-a-glance)
3. [Tech Stack](#tech-stack)
4. [Prerequisites](#prerequisites)
5. [Project Structure](#project-structure)
6. [Configuration Guide](#configuration-guide)
7. [Running the Application](#running-the-application)
8. [Using the REST API](#using-the-rest-api)
9. [Understanding the Output](#understanding-the-output)
10. [Spring AI Concepts Explained](#spring-ai-concepts-explained)
11. [Python → Spring AI Cheat Sheet](#python--spring-ai-cheat-sheet)
12. [Troubleshooting](#troubleshooting)
13. [What's Coming Next](#whats-coming-next)

---

## What This Project Does

This application is an **AI-powered CI/CD monitor** built with **Spring AI** — Anthropic's and the Java ecosystem's answer to Python frameworks like LangChain and LangGraph.

Every **5 minutes** the application:

```
1. Polls Jenkins REST API  →  checks if the last build FAILED
2. Fetches the console log  →  captures the failure output
3. Sends the log to your LLM  →  your internally hosted model analyses it
4. Prints a Fix Report  →  structured steps to resolve the failure
5. Saves the report to disk  →  keeps a history of all failures and fixes
```

It is designed as a **demo project** — every file is heavily commented to explain Java/Spring concepts in terms that Python developers already understand.

---

## Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Jenkins AI Monitor                              │
│                                                                         │
│  ┌──────────────┐    ┌─────────────────┐    ┌──────────────────────┐   │
│  │  Scheduler   │───►│  JenkinsClient  │───►│  BuildAnalysisAgent  │   │
│  │  (every 5m)  │    │  REST API calls  │    │  ★ SPRING AI CORE ★  │   │
│  └──────────────┘    └─────────────────┘    └──────────┬───────────┘   │
│                                                         │               │
│  ┌──────────────────────────────────────────────────────▼───────────┐   │
│  │                      FixReportService                            │   │
│  │           Console output  +  ./fix-reports/*.txt                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │         ManualTriggerController  (REST API for demos)            │   │
│  │   GET /api/monitor/status    POST /api/monitor/analyse           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
          │                                          │
          ▼                                          ▼
  ┌───────────────┐                    ┌─────────────────────────┐
  │    Jenkins    │                    │   Your Hosted LLM       │
  │  REST API     │                    │   (OpenAI-compatible)   │
  │  /job/.../    │                    │   via HTTP proxy        │
  └───────────────┘                    └─────────────────────────┘
```

---

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | **17** (LTS) |
| Build Tool | Apache Maven | **3.6.3+** |
| Framework | Spring Boot | 3.3.5 |
| AI Framework | **Spring AI** | 1.0.0 |
| LLM Protocol | OpenAI-compatible HTTP API | — |
| HTTP Client | Spring `RestClient` | (built-in) |
| Scheduler | Spring `@Scheduled` | (built-in) |
| Boilerplate reduction | Lombok | (latest) |

---

## Prerequisites

Before you start, ensure the following are installed and working on your machine.

### 1. Java 17

This project **requires Java 17** exactly. The build will fail with a clear error if you use a different version.

**Check your version:**
```powershell
java -version
# Expected output: openjdk version "17.x.x" ...
```

**Install Java 17 (if not present):**
- Download from [Adoptium (Eclipse Temurin)](https://adoptium.net/temurin/releases/?version=17) — choose **JDK 17, Windows x64**
- Run the installer
- After install, verify: `java -version`

**If you have multiple Java versions installed**, set `JAVA_HOME` to the 17 installation:
```powershell
# Temporary (current PowerShell session only)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version   # confirm it shows 17
```

---

### 2. Apache Maven 3.x

**Check your version:**
```powershell
mvn -version
# Expected: Apache Maven 3.x.x
```

**Install Maven (if not present):**
- Download from [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) — choose the **Binary zip archive**
- Extract to e.g. `C:\tools\maven`
- Add `C:\tools\maven\bin` to your system `PATH`
- Verify: `mvn -version`

---

### 3. Jenkins Access

You need:
- The **base URL** of your Jenkins server (e.g. `http://jenkins.bank.internal:8080`)
- A **Jenkins job name** to monitor
- A Jenkins **username** and **API Token** (not your password — see below)

**How to generate a Jenkins API Token:**
1. Log into Jenkins
2. Click your username (top-right) → **Configure**
3. Under **API Token** → click **Add new Token**
4. Give it a name → click **Generate**
5. **Copy the token now** — it won't be shown again

---

### 4. LLM Access

You need:
- The **proxy URL** through which your hosted LLM is accessible (e.g. `http://llm-proxy.bank.internal:8080/v1`)
- The **model name** your team has deployed
- An **API key** for authentication

Your LLM must expose an **OpenAI-compatible REST API** — Spring AI will send standard OpenAI-format requests to whatever base URL you configure. No code changes needed.

---

## Project Structure

```
jenkins-ai-monitor/
│
├── pom.xml                                    # Maven build config (Java 17, Spring AI deps)
├── .env.example                               # Template for your secrets — copy to .env
├── .gitignore                                 # Excludes .env and build outputs
├── run.ps1                                    # One-click startup script (Windows PowerShell)
│
└── src/main/
    ├── resources/
    │   └── application.yml                    # All app configuration with comments
    │
    └── java/com/bank/demo/monitor/
        │
        ├── JenkinsAiMonitorApplication.java   # ★ Entry point — starts everything
        │
        ├── config/                            # Configuration beans
        │   ├── JenkinsProperties.java         # Jenkins settings → typed Java object
        │   ├── MonitorProperties.java         # Scheduler settings → typed Java object
        │   └── AppConfig.java                 # Creates HTTP client, output directory
        │
        ├── model/                             # Data classes (like Python dataclasses)
        │   ├── BuildInfo.java                 # Jenkins API response model
        │   └── BuildAnalysisResult.java       # ★ Structured output from LLM
        │
        ├── jenkins/
        │   └── JenkinsClient.java             # All REST calls to Jenkins API
        │
        ├── agent/
        │   ├── BuildAnalysisAgent.java        # ★★ THE SPRING AI AGENT (ChatClient)
        │   └── ManualTriggerController.java   # REST API for demo/manual testing
        │
        └── service/
            ├── JenkinsMonitorService.java     # ★ @Scheduled polling loop
            └── FixReportService.java          # Formats + saves fix reports
```

> **Files marked ★ are the most important to understand** for the demo.
> Start with `BuildAnalysisAgent.java` — that is where Spring AI does its work.

---

## Configuration Guide

### Step 1 — Create your `.env` file

```powershell
Copy-Item .env.example .env
```

Open `.env` in any text editor and fill in your values:

```dotenv
# ── Jenkins ─────────────────────────────────────────────────────────────
JENKINS_BASE_URL=http://jenkins.bank.internal:8080
JENKINS_JOB_NAME=payments/nightly-build          # or just: my-job
JENKINS_USERNAME=your.name
JENKINS_API_TOKEN=11abc123def456...               # from Jenkins → Configure → API Token

# ── LLM (OpenAI-compatible proxy) ───────────────────────────────────────
SPRING_AI_OPENAI_BASE_URL=http://llm-proxy.bank.internal:8080/v1
SPRING_AI_OPENAI_API_KEY=sk-your-api-key-here
LLM_MODEL_NAME=your-model-name                   # e.g. gpt-4o or llama3

# ── Tuning (optional — defaults shown) ──────────────────────────────────
MONITOR_POLL_INTERVAL_MS=300000                   # 5 minutes in milliseconds
JENKINS_LOG_MAX_LINES=200                         # how much log to send to LLM
MONITOR_OUTPUT_DIR=./fix-reports                  # where to save reports
```

> **Security note:** The `.env` file is listed in `.gitignore` and will never be committed to Git.
> Never hardcode secrets in `application.yml` or Java files.

---

### Configuration Reference

Every setting can be controlled via environment variable — no code changes needed.

| Environment Variable | Description | Default |
|---|---|---|
| `JENKINS_BASE_URL` | Jenkins server base URL | `http://localhost:8080` |
| `JENKINS_JOB_NAME` | Job name or folder path | `my-pipeline-job` |
| `JENKINS_USERNAME` | Jenkins username | `admin` |
| `JENKINS_API_TOKEN` | Jenkins API token | *(required)* |
| `JENKINS_LOG_MAX_LINES` | Max log lines sent to LLM | `200` |
| `SPRING_AI_OPENAI_BASE_URL` | LLM proxy base URL (must end in `/v1`) | `http://localhost:11434/v1` |
| `SPRING_AI_OPENAI_API_KEY` | LLM API key | *(required)* |
| `LLM_MODEL_NAME` | Model name to use | `gpt-4o` |
| `MONITOR_POLL_INTERVAL_MS` | How often to poll Jenkins (ms) | `300000` (5 min) |
| `MONITOR_OUTPUT_DIR` | Directory to save fix reports | `./fix-reports` |
| `MONITOR_AUTO_FIX_ENABLED` | Enable automatic Git fix (future) | `false` |

---

## Running the Application

### Option A — Using the provided script (recommended)

```powershell
.\run.ps1
```

This script reads your `.env` file, sets environment variables, then starts the app.

---

### Option B — Maven directly

If you've already set your environment variables another way:

```powershell
mvn spring-boot:run
```

---

### Option C — Build a JAR and run it

```powershell
# Build
mvn clean package -DskipTests

# Run
java -jar target/jenkins-ai-monitor-1.0.0.jar
```

To pass environment variables inline with the JAR:

```powershell
$env:JENKINS_BASE_URL="http://jenkins.bank.internal:8080"
$env:SPRING_AI_OPENAI_API_KEY="sk-your-key"
java -jar target/jenkins-ai-monitor-1.0.0.jar
```

---

### Expected startup output

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.3.5)

INFO  - Starting JenkinsAiMonitorApplication
INFO  - Fix reports will be saved to: D:\Spring AI Demo\fix-reports
INFO  - --- Polling Jenkins for job: payments/nightly-build ---
INFO  - Last build: #47 — result=FAILURE
WARN  - Build #47 FAILED — starting AI analysis...
INFO  - Sending build #47 log to LLM for analysis (3842 chars)...
INFO  - Analysis complete. Category: TEST_FAILURE, Confidence: HIGH
INFO  - Fix report saved: fix-reports/fix_payments_nightly-build_build47_20260603_143022.txt
```

---

## Using the REST API

The app exposes two endpoints for demo and testing purposes — no need to wait for the 5-minute timer.

### Check Jenkins status

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/status" -Method GET
```

Or with curl:
```bash
curl http://localhost:8080/api/monitor/status
```

**Response:**
```json
{
  "buildNumber": 47,
  "result": "FAILURE",
  "displayName": "#47",
  "url": "http://jenkins.bank.internal:8080/job/payments/nightly-build/47/"
}
```

---

### Trigger analysis immediately (great for demos)

```powershell
# Analyse the last build
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/analyse" -Method POST

# Analyse a specific build number
$body = '{"buildNumber": 42}'
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/analyse" -Method POST `
  -ContentType "application/json" -Body $body
```

**Response** — the full structured analysis:
```json
{
  "failureSummary": "NullPointerException in PaymentServiceTest at line 87",
  "rootCauseCategory": "TEST_FAILURE",
  "confidence": "HIGH",
  "fixSteps": [
    "Open src/test/java/com/bank/PaymentServiceTest.java",
    "Check line 87 — the mock for PaymentRepository is not initialised",
    "Add @MockBean annotation or call MockitoAnnotations.openMocks(this) in @BeforeEach"
  ],
  "suggestedCodeFix": "@BeforeEach\nvoid setUp() {\n    MockitoAnnotations.openMocks(this);\n}",
  "affectedFiles": ["src/test/java/com/bank/PaymentServiceTest.java"],
  "additionalNotes": "This often happens after upgrading Spring Boot — MockitoAnnotations must be initialised explicitly in newer versions.",
  "buildNumber": 47,
  "jobName": "payments/nightly-build",
  "analysedAt": "2026-06-03T14:30:22"
}
```

---

## Understanding the Output

Every analysis also prints to console and saves a `.txt` report in `./fix-reports/`:

```
================================================================================
JENKINS BUILD FAILURE ANALYSIS REPORT
================================================================================

Job          : payments/nightly-build
Build #      : 47
Analysed at  : 2026-06-03 14:30:22
Confidence   : HIGH
Root Cause   : TEST_FAILURE

FAILURE SUMMARY
----------------------------------------
NullPointerException in PaymentServiceTest at line 87

FIX STEPS
----------------------------------------
  1. Open src/test/java/com/bank/PaymentServiceTest.java
  2. Check line 87 — the mock for PaymentRepository is not initialised
  3. Add @MockBean annotation or call MockitoAnnotations.openMocks(this) in @BeforeEach

AFFECTED FILES
----------------------------------------
  - src/test/java/com/bank/PaymentServiceTest.java

SUGGESTED CODE FIX
----------------------------------------
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}

ADDITIONAL NOTES
----------------------------------------
This often happens after upgrading Spring Boot — MockitoAnnotations must be
initialised explicitly in newer versions.
================================================================================
```

Reports are saved as: `fix-reports/fix_{jobName}_build{N}_{timestamp}.txt`

---

## Spring AI Concepts Explained

This section is written for the upskilling session. Use it as talking notes.

---

### Concept 1 — ChatClient (the LLM connection)

In Python with LangChain you write:
```python
from langchain_openai import ChatOpenAI
llm = ChatOpenAI(model="gpt-4o", base_url="http://proxy/v1", api_key="sk-...")
```

In Spring AI, **you don't instantiate the LLM client yourself**. Spring reads `application.yml` and creates it for you:

```yaml
# application.yml
spring:
  ai:
    openai:
      base-url: http://llm-proxy.bank.internal:8080/v1
      api-key: ${SPRING_AI_OPENAI_API_KEY}
      chat:
        options:
          model: your-model-name
          temperature: 0.2
```

Then in your class:
```java
// Spring injects this automatically — no manual setup needed
private final ChatClient.Builder chatClientBuilder;

ChatClient client = chatClientBuilder.build();
```

> **Key insight:** Configuration drives the LLM connection, not code. Perfect for enterprise environments where proxy URLs and API keys change between environments.

---

### Concept 2 — Structured Output

This is the most powerful Spring AI feature shown in this demo.

**Python equivalent (LangGraph/LangChain):**
```python
from pydantic import BaseModel

class AnalysisResult(BaseModel):
    failure_summary: str
    fix_steps: list[str]
    confidence: str

chain = prompt | llm.with_structured_output(AnalysisResult)
result = chain.invoke({"log": console_log})
print(result.failure_summary)  # typed access
```

**Spring AI equivalent:**
```java
// 1. Define a regular Java class (Lombok @Data adds getters/setters)
@Data
public class BuildAnalysisResult {
    private String failureSummary;
    private List<String> fixSteps;
    private String confidence;
}

// 2. Use .entity() — Spring AI does the rest
BuildAnalysisResult result = chatClient
    .prompt(new Prompt(systemMessage, userMessage))
    .call()
    .entity(BuildAnalysisResult.class);  // ← auto JSON schema + deserialization

System.out.println(result.getFailureSummary());  // typed access
```

Spring AI automatically:
- Generates a JSON Schema from your Java class
- Appends it to the prompt so the LLM knows what to return
- Parses the LLM's JSON response back into your Java object

---

### Concept 3 — @Scheduled (the agentic loop)

**Python equivalent:**
```python
from apscheduler.schedulers.background import BackgroundScheduler

scheduler = BackgroundScheduler()
scheduler.add_job(poll_jenkins, 'interval', minutes=5)
scheduler.start()
```

**Spring equivalent:**
```java
@Scheduled(fixedDelayString = "#{@monitorProperties.pollIntervalMs}", initialDelay = 5000)
public void pollJenkins() {
    // called automatically every N milliseconds
}
```

No scheduler setup code. Just annotate the method and Spring handles the rest.

---

### Concept 4 — Dependency Injection

**Python:** You create objects and pass them explicitly, or use a DI framework like `dependency-injector`.

**Spring:** You declare what you need, Spring wires it automatically:

```java
@Service                       // "Spring, manage this class"
@RequiredArgsConstructor       // "generate a constructor for all final fields"
public class JenkinsMonitorService {

    private final JenkinsClient jenkinsClient;        // ← Spring injects this
    private final BuildAnalysisAgent buildAnalysisAgent;  // ← and this
    private final FixReportService fixReportService;  // ← and this

    // No need to new() any of these — Spring finds them and wires them in
}
```

---

### Concept 5 — Configuration Properties

**Python equivalent:** Pydantic `BaseSettings` reading from `.env`:
```python
from pydantic_settings import BaseSettings

class JenkinsSettings(BaseSettings):
    base_url: str
    job_name: str
    api_token: str

    class Config:
        env_prefix = "JENKINS_"
```

**Spring equivalent:** `@ConfigurationProperties`:
```java
@Data
@Component
@ConfigurationProperties(prefix = "jenkins")   // reads jenkins.* from application.yml
public class JenkinsProperties {
    private String baseUrl;    // ← maps to JENKINS_BASE_URL env var via yaml
    private String jobName;
    private String apiToken;
}
```

---

## Python → Spring AI Cheat Sheet

| Python / LangGraph | Spring AI / Spring Boot |
|---|---|
| `pip install langchain-openai` | `<artifactId>spring-ai-openai-spring-boot-starter</artifactId>` in pom.xml |
| `ChatOpenAI(model=..., base_url=...)` | `spring.ai.openai.*` in application.yml |
| `llm.with_structured_output(PydanticModel)` | `.call().entity(MyClass.class)` |
| `SystemMessage / HumanMessage` | `SystemMessage / UserMessage` |
| `chain = prompt \| llm \| parser` | `chatClient.prompt(...).call().entity(...)` |
| `@app.on_event("startup") + APScheduler` | `@Scheduled(fixedDelayString = "...")` |
| `requests.Session(auth=(...))` | `RestClient.builder().defaultHeaders(...)` |
| `pydantic.BaseSettings` | `@ConfigurationProperties(prefix="...")` |
| `os.environ["KEY"]` or `.env` | `${ENV_VAR_NAME}` in application.yml |
| `@dataclass` / `pydantic.BaseModel` | Java class + Lombok `@Data` |
| `logging.getLogger(__name__)` | Lombok `@Slf4j` → `log.info(...)` |
| `if __name__ == "__main__"` | `SpringApplication.run(...)` in `main()` |
| `FastAPI @app.get("/path")` | `@GetMapping("/path")` in `@RestController` |

---

## Troubleshooting

### Build fails: "requires Java 17"

```
[ERROR] requireJavaVersion: This project requires Java 17.
```

**Fix:** Set `JAVA_HOME` to your JDK 17 installation:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

---

### Jenkins returns 401 Unauthorized

```
[ERROR] Failed to fetch Jenkins build info: 401 Unauthorized
```

**Fix:** Check your `JENKINS_USERNAME` and `JENKINS_API_TOKEN` in `.env`.
Make sure you are using an **API Token**, not your Jenkins login password.

---

### Jenkins returns 404 Not Found

```
[ERROR] Failed to fetch Jenkins build info: 404 Not Found
```

**Fix:** Check `JENKINS_JOB_NAME`. For jobs inside folders, use `/` as separator:
```
JENKINS_JOB_NAME=my-folder/my-job
# NOT: my-folder%2Fmy-job (don't URL-encode)
```

---

### LLM returns connection refused

```
[ERROR] LLM analysis failed: Connection refused: llm-proxy.bank.internal/8080
```

**Fix:**
1. Verify `SPRING_AI_OPENAI_BASE_URL` points to your proxy and ends in `/v1`
2. Test reachability: `curl http://llm-proxy.bank.internal:8080/v1/models`
3. Check if you need to be on VPN

---

### LLM returns 401 / 403

```
[ERROR] LLM analysis failed: 401 Unauthorized
```

**Fix:** Check `SPRING_AI_OPENAI_API_KEY` in your `.env` file.

---

### App starts but no analysis runs

The scheduler runs **5 seconds after startup**, then every 5 minutes.
If you don't want to wait, use the manual trigger:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/analyse" -Method POST
```

---

### Log is truncated and analysis is poor quality

Increase the log lines sent to the LLM (within your model's context limit):
```
JENKINS_LOG_MAX_LINES=500
```

---

## What's Coming Next

This project is designed to grow. Here's the roadmap:

### Phase 2 — Git Auto-Fix

When `MONITOR_AUTO_FIX_ENABLED=true`, the app will:
1. Parse the `suggestedCodeFix` from the analysis
2. Identify the `affectedFiles`
3. Apply the fix via the GitHub/GitLab API or a local `git` command
4. Open a Pull Request with the fix

You will provide:
- Git repository URL
- Personal Access Token or SSH key
- Target branch name

### Phase 3 — Notification

- Send analysis report to Slack / MS Teams webhook
- Email report via SMTP

### Phase 4 — Web Dashboard

- Simple Angular frontend to view all past reports
- Real-time streaming of LLM analysis via Server-Sent Events (Spring AI supports streaming natively)

---

## Contributing

This is an internal demo project. To suggest changes:
1. Fork the repo internally
2. Make your changes on a feature branch
3. Open a Pull Request with a clear description

---

*Built with Spring AI 1.0.0 · Spring Boot 3.3.5 · Java 17*
