# Jamph-Rag-Api-Umami

Kotlin RAG API that connects Umami Analytics with an Ollama LLM for natural language queries and SQL generation.

## Requirements

- Java 21+
- Maven 3.9+
- Ollama (optional — only needed for `/api/chat` and `/api/sql`)

## Start the API

**Windows:**
```powershell
.\start-api.ps1
```

**Mac/Linux:**
```bash
mvn clean package -Dmaven.test.skip=true
java -jar target/api-1.0-SNAPSHOT-jar-with-dependencies.jar
```

API runs on `http://localhost:8004`.

## Verify

```bash
curl http://localhost:8004/health
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/api/chat` | Natural language query via Ollama |
| POST | `/api/sql` | Generate SQL from natural language |

**Chat example:**
```bash
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is Umami Analytics?"}'
```

**SQL example:**
```bash
curl -X POST http://localhost:8004/api/sql \
  -H "Content-Type: application/json" \
  -d '{"query": "Show total pageviews for all websites"}'
```

## Configuration

Edit `src/main/resources/application.conf` or use environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `API_PORT` | `8004` | API port |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama URL |
| `OLLAMA_MODEL` | `qwen2.5-coder:7b` | Model to use |

## Ollama (optional)

Only needed if you want LLM features. Start it separately in any order — the API will work without it.

```bash
ollama serve
ollama pull qwen2.5-coder:7b
```

## Docker

```bash
docker compose -f docker-compose.dev.yml up
```

---

## Henvendelser

Enten:
Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

Eller:
Spørsmål knyttet til koden eller repositoryet kan stilles til teamalias@nav.no (som evt må opprettes av noen™ Windows-mennesker) eller som issues her på GitHub (stryk det som ikke passer).

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #teamkanal.(teamreasarchobs)