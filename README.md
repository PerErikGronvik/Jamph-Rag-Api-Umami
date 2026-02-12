# Jamph-Rag-Api-Umami

**Kotlin-based RAG API** that connects Umami Analytics frontend with Ollama LLM for natural language queries and SQL generation.

## 🏗️ Architecture

```
Frontend (Umami)          Backend (RAG API)         LLM Service
localhost:5173     →     localhost:8004      →    localhost:11434
                                                    (Ollama)
```

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Ollama running on `http://localhost:11434`

### 1. Start Ollama
```bash
ollama serve
# Or using Docker: docker compose --profile ollama up -d
```

### 2. Build and Run API

**Option A: Quick Start Script (Recommended for Windows)** ⭐
```powershell
.\start-api.ps1
```
*Automatically checks Ollama, builds, and runs the API*

**Option B: Manual**
```powershell
mvn clean package -DskipTests
java -jar target/api-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Option C: Using Maven exec**
```bash
mvn clean install
mvn exec:java -Dexec.mainClass="no.jamph.ragumami.ApplicationKt"
```

### 3. Verify
```bash
curl http://localhost:8004/health
```

## 📚 Documentation

- **[API Usage Guide](README_API.md)** - Complete API documentation with examples ⭐
- **[Maven Setup Guide](README.mavensetup.md)** - Detailed setup instructions
- **[Integration Guide](README.integrations.md)** - Configure Ollama and frontend connections

## 📡 API Endpoints

### Health Check
```bash
GET http://localhost:8004/health
```

### Chat Endpoint
```bash
POST http://localhost:8004/api/chat
Content-Type: application/json

{
  "message": "What is Umami Analytics?"
}
```

### SQL Generation
```bash
POST http://localhost:8004/api/sql
Content-Type: application/json

{
  "query": "Show total pageviews for all websites"
}
```

**See [README_API.md](README_API.md) for complete API documentation with JavaScript/React examples.**

## 🛠️ Tech Stack

- **Kotlin 2.1.0** - Programming language
- **Ktor 3.0.2** - Web framework
- **LangChain4j 0.36.2** - RAG orchestration
- **Ollama** - LLM integration (llama3.2:3b)
- **Maven** - Build tool
- **Docker** - Containerization

## 🔧 Configuration

Edit `src/main/resources/application.conf`:
```hocon
ollama {
    baseUrl = "http://localhost:11434"
    model = "llama3.2:3b"
}
```

Or use environment variables:
```bash
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="llama3.2:3b"
export API_PORT="8004"
```

## ✅ Current Status

**MVP Phase Complete:**
- ✅ Chat endpoint with Ollama integration
- ✅ SQL generation from natural language
- ✅ CORS configured for `localhost:5173`
- ✅ Error handling with retry logic (3 attempts, 30s timeout)
- ✅ Docker support
- ✅ NAIS deployment configuration

## 📁 Project Structure

```
├── pom.xml
├── src/
│   ├── main/
│   │   ├── kotlin/no/jamph/ragumami/
│   │   │   ├── Application.kt              # Main entry point
│   │   │   ├── core/
│   │   │   │   ├── llm/OllamaClient.kt    # LLM integration
│   │   │   │   └── rag/RAGOrchestrator.kt # RAG interface
│   │   │   └── umami/
│   │   │       └── domain/UmamiRAGService.kt
│   │   └── resources/
│   │       ├── application.conf            # Configuration
│   │       └── logback.xml                 # Logging
│   └── test/
│       └── kotlin/                         # WireMock tests
├── Dockerfile
├── docker-compose.dev.yml
└── .nais/nais-dev.yaml                     # NAIS deployment
```

## 🧪 Testing

```bash
# Run tests
mvn test -DskipTests=false

# Test API manually
curl http://localhost:8004/health
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello!"}'
```

## 🐳 Docker

```bash
# Build and run
docker compose -f docker-compose.dev.yml up

# Or build manually
docker build -t jamph-rag-api .
docker run -p 8004:8004 jamph-rag-api
```

## 🚢 Deployment

Deploy to NAIS (NAV's Kubernetes platform):
```bash
# Configuration in .nais/nais-dev.yaml
# Ingress: https://jamph-rag-api-umami.ekstern.dev.nav.no
```

---

## Henvendelser

Enten:
Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

Eller:
Spørsmål knyttet til koden eller repositoryet kan stilles til teamalias@nav.no (som evt må opprettes av noen™ Windows-mennesker) eller som issues her på GitHub (stryk det som ikke passer).

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #teamkanal.(teamreasarchobs)