# Jamph-Rag-Api-Umami - API Usage Guide

## 🎯 Architecture Overview

```
Frontend (Umami)          Backend (RAG API)         LLM Service
localhost:5173     →     localhost:8004      →    localhost:11434
                                                    (Ollama)
    │                         │                         │
    │  POST /api/chat         │  Generate prompt       │
    ├──────────────────────►  ├────────────────────►  │
    │                         │                         │
    │                         │  ◄──── Response        │
    │  ◄────── JSON Response  │                         │
    │                         │                         │
```

## ✅ Infrastructure Status

Your current infrastructure **fully supports** the request flow:

- ✅ **CORS configured** for `localhost:5173`
- ✅ **OllamaClient** configured to connect to `http://localhost:11434`
- ✅ **Two API endpoints** ready: `/api/chat` and `/api/sql`
- ✅ **Error handling** with proper HTTP status codes
- ✅ **JSON serialization** with Gson

## 📡 API Endpoints

### Base URL
```
http://localhost:8004
```

---

## 1. Health Check

**Verify the API is running**

### Endpoint
```http
GET /health
```

### Response
```json
{
  "status": "healthy",
  "service": "rag-umami",
  "flavor": "umami"
}
```

### cURL Example
```bash
curl http://localhost:8004/health
```

---

## 2. Chat Endpoint

**Send natural language queries to Ollama LLM**

### Endpoint
```http
POST /api/chat
```

### Request Body
```json
{
  "message": "What is Umami Analytics?",
  "model": "qwen2.5-coder:7b"  // Optional: Override default model
}
```

**Parameters:**
- `message` (required): Your question or prompt
- `model` (optional): Ollama model to use (e.g., `qwen2.5-coder:7b`, `llama3.2:3b`). Defaults to configured model.

### Response (Success - 200 OK)
```json
{
  "response": "Umami Analytics er en open-source, personvernfokusert webanalyse-plattform..."
}
```

### Response (Error - 500)
```json
{
  "error": "Integrasjon til api rag virker, men ollama virker ikke (timeout)"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What is Umami Analytics?"}'

# With custom model
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Explain this code","model":"qwen2.5-coder:7b"}'
```

### JavaScript/Fetch Example
```javascript
// From your Umami frontend (localhost:5173)
async function askChatbot(userMessage, modelName = null) {
  try {
    const requestBody = { message: userMessage };
    if (modelName) {
      requestBody.model = modelName;  // e.g., "qwen2.5-coder:7b"
    }
    
    const response = await fetch('http://localhost:8004/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'API request failed');
    }

    const data = await response.json();
    return data.response;
  } catch (error) {
    console.error('Chat API error:', error);
    throw error;
  }
}

// Usage
const answer = await askChatbot('Show me website statistics');
console.log(answer);

// Or with custom model
const answer2 = await askChatbot('Explain this code', 'qwen2.5-coder:7b');
console.log(answer2);
```

### React Example
```typescript
import { useState } from 'react';

function ChatInterface() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const sendMessage = async () => {
    setLoading(true);
    setError('');
    
    try {
      const res = await fetch('http://localhost:8004/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message })
      });

      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.error);
      }

      const data = await res.json();
      setResponse(data.response);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <input 
        value={message} 
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Ask something..."
      />
      <button onClick={sendMessage} disabled={loading}>
        {loading ? 'Sending...' : 'Send'}
      </button>
      {response && <p>Response: {response}</p>}
      {error && <p style={{color: 'red'}}>Error: {error}</p>}
    </div>
  );
}
```

---

## 3. SQL Generation Endpoint

**Generate SQL queries from natural language**

### Endpoint
```http
POST /api/sql
```

### Request Body
```json
{
  "query": "Show total pageviews for all websites",
  "model": "qwen2.5-coder:7b"  // Optional: Override default model
}
```

**Parameters:**
- `query` (required): Natural language description of desired SQL query
- `model` (optional): Ollama model to use. Defaults to configured model.

### Response (Success - 200 OK)
```json
{
  "sql": "SELECT website_id, COUNT(*) as pageviews FROM pageview GROUP BY website_id;"
}
```

### Response (Error - 500)
```json
{
  "error": "Failed to generate SQL query"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8004/api/sql \
  -H "Content-Type: application/json" \
  -d '{"query":"Show total pageviews for all websites"}'
```

### JavaScript/Fetch Example
```javascript
async function generateSQL(naturalLanguageQuery) {
  try {
    const response = await fetch('http://localhost:8004/api/sql', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: naturalLanguageQuery
      })
    });

    if (!response.ok) {
      throw new Error('SQL generation failed');
    }

    const data = await response.json();
    return data.sql;
  } catch (error) {
    console.error('SQL API error:', error);
    throw error;
  }
}

// Usage
const sqlQuery = await generateSQL('Show top 10 visited pages this month');
console.log(sqlQuery);
```

---

## 🔧 Configuration

### Backend Configuration

**File:** `src/main/resources/application.conf`

```hocon
ollama {
    baseUrl = "http://localhost:11434"
    baseUrl = ${?OLLAMA_BASE_URL}
    model = "llama3.2:3b"
    model = ${?OLLAMA_MODEL}
}
```

### Environment Variables

You can override configuration using environment variables:

```bash
# Windows PowerShell
$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="llama3.2:3b"
$env:API_PORT="8004"

# Linux/Mac
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="llama3.2:3b"
export API_PORT="8004"
```

### CORS Configuration

**File:** `src/main/kotlin/no/jamph/ragumami/Application.kt`

Currently configured to allow:
- `localhost:3000`
- `localhost:5173` ← Your Umami frontend

To add more origins, edit the `configureCORS()` function:

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHost("localhost:5173")        // Your Umami frontend
        allowHost("your-domain.com", schemes = listOf("https"))
        allowCredentials = true
    }
}
```

---

## 🚀 Starting the Services

### 1. Start Ollama (LLM Service)

```bash
# Option A: Local Ollama installation
ollama serve

# Option B: Docker (if configured)
docker compose --profile ollama up -d

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

### 2. Start the Backend API

```bash
# Option A: Using PowerShell script (Recommended)
.\start-api.ps1

# Option B: Using Maven
mvn clean install
mvn exec:java -Dexec.mainClass="no.jamph.ragumami.ApplicationKt"

# Option C: Run the JAR
mvn clean package -DskipTests
java -jar target/api-1.0-SNAPSHOT-jar-with-dependencies.jar

# Option D: Using Docker
docker compose -f docker-compose.dev.yml up
```

**Verify API is running:**
```bash
curl http://localhost:8004/health
```

### 3. Start Your Umami Frontend

```bash
cd your-umami-project
npm run dev
# Should start on http://localhost:5173
```

---

## 🧪 Testing the Complete Flow

### Step-by-Step Test

1. **Check all services are running:**

```bash
# Test Ollama
curl http://localhost:11434/api/tags

# Test Backend API
curl http://localhost:8004/health

# Test Frontend
# Open browser: http://localhost:5173
```

2. **Test Chat Endpoint:**

```bash
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, can you help me?"}'
```

Expected response:
```json
{
  "response": "Ja, jeg kan hjelpe deg! Hva lurer du på?"
}
```

3. **Test SQL Generation:**

```bash
curl -X POST http://localhost:8004/api/sql \
  -H "Content-Type: application/json" \
  -d '{"query":"Show all websites with more than 1000 pageviews"}'
```

Expected response:
```json
{
  "sql": "SELECT * FROM website WHERE pageviews > 1000;"
}
```

---

## 📊 Response Times & Monitoring

### Expected Response Times

| Endpoint | Normal | Timeout |
|----------|--------|---------|
| `/health` | < 10ms | N/A |
| `/api/chat` | 1-5s | 30s |
| `/api/sql` | 1-5s | 30s |

### Monitoring Logs

The backend logs important events:

```bash
# Success
OLLAMA_SUCCESS: Generate completed in 2341ms

# Retry attempts
OLLAMA_RETRY: Retrying request to /api/generate due to status 503. Attempt: 2

# Timeout
OLLAMA_TIMEOUT: Request timed out after 30s for model: llama3.2:3b
```

---

## ⚠️ Error Handling

### Common Errors

#### 1. CORS Error (from Frontend)
```
Access to fetch at 'http://localhost:8004/api/chat' from origin 'http://localhost:5173' 
has been blocked by CORS policy
```

**Solution:** Verify `localhost:5173` is in the CORS configuration (it should be by default).

#### 2. Connection Refused (to Ollama)
```json
{
  "error": "Integrasjon til api rag virker, men ollama virker ikke"
}
```

**Solution:** 
```bash
# Start Ollama
ollama serve

# Or check if running
curl http://localhost:11434/api/tags
```

#### 3. Timeout Error
```json
{
  "error": "Integrasjon til api rag virker, men ollama virker ikke (timeout)"
}
```

**Solution:** 
- Ollama might be processing a large request
- Check Ollama logs: `docker compose logs ollama`
- Consider using a smaller/faster model

#### 4. Port Already in Use
```
Address already in use: bind
```

**Solution:**
```powershell
# Windows: Find and kill process on port 8004
netstat -ano | findstr :8004
taskkill /PID <PID> /F
```

---

## 🔐 Security Considerations

### Development (Current Setup)
- ✅ CORS restricted to specific origins
- ⚠️ No authentication (OK for local development)
- ⚠️ HTTP only (OK for local development)

### Production Recommendations
1. **Add API Key Authentication:**
   ```kotlin
   // Add to Application.kt
   install(Authentication) {
       bearer("auth-bearer") {
           authenticate { credential ->
               if (credential.token == System.getenv("API_KEY")) {
                   UserIdPrincipal("api-user")
               } else null
           }
       }
   }
   ```

2. **Use HTTPS:** Deploy behind a reverse proxy (NAIS/nginx)

3. **Rate Limiting:** Prevent abuse

4. **Input Validation:** Sanitize user inputs before sending to LLM

---

## 📝 API Request Examples

### PowerShell
```powershell
# Chat request
$body = @{
    message = "What is Umami Analytics?"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8004/api/chat" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

# SQL request
$sqlBody = @{
    query = "Show top 10 pages"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8004/api/sql" `
    -Method POST `
    -ContentType "application/json" `
    -Body $sqlBody
```

### Python
```python
import requests

# Chat request
response = requests.post(
    'http://localhost:8004/api/chat',
    json={'message': 'What is Umami Analytics?'}
)
print(response.json())

# SQL request
response = requests.post(
    'http://localhost:8004/api/sql',
    json={'query': 'Show all websites'}
)
print(response.json())
```

### Node.js
```javascript
const axios = require('axios');

// Chat request
async function chat(message) {
  const response = await axios.post('http://localhost:8004/api/chat', {
    message: message
  });
  return response.data;
}

// SQL request
async function generateSQL(query) {
  const response = await axios.post('http://localhost:8004/api/sql', {
    query: query
  });
  return response.data;
}

// Usage
chat('What is Umami Analytics?').then(console.log);
generateSQL('Show all websites').then(console.log);
```

---

## 🐛 Debugging

### Enable Debug Logging

```bash
# Set environment variable
export LOG_LEVEL=DEBUG  # Linux/Mac
$env:LOG_LEVEL="DEBUG"  # Windows PowerShell

# Restart the API
mvn exec:java -Dexec.mainClass="no.jamph.ragumami.ApplicationKt"
```

### Check Logs

```bash
# API logs (console output)
# Look for:
# - OLLAMA_SUCCESS: Generate completed in XXXms
# - OLLAMA_RETRY: Retrying request...
# - OLLAMA_TIMEOUT: Request timed out...

# Ollama logs (if using Docker)
docker compose logs -f ollama
```

### Test Ollama Directly

```bash
# Test Ollama generate endpoint
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2:3b",
  "prompt": "Hello, who are you?",
  "stream": false
}'
```

---

## 📚 Additional Resources

- **Main Setup Guide:** [README.md](README.md)
- **Maven Setup:** [README.mavensetup.md](README.mavensetup.md)
- **Integration Guide:** [README.integrations.md](README.integrations.md)
- **Ktor Documentation:** https://ktor.io/docs
- **Ollama API:** https://github.com/ollama/ollama/blob/main/docs/api.md

---

## 💡 Next Steps

1. **Integrate into your Umami Frontend:**
   - Add a chat interface component
   - Implement SQL query generation UI
   - Handle loading states and errors

2. **Enhance the API:**
   - Add authentication middleware
   - Implement rate limiting
   - Add request/response logging
   - Create OpenAPI/Swagger documentation

3. **Production Deployment:**
   - Deploy to NAIS (configuration already in `.nais/nais-dev.yaml`)
   - Set up monitoring with Grafana
   - Configure production Ollama endpoint

---

## ✅ Quick Verification Checklist

- [ ] Ollama running on `http://localhost:11434`
- [ ] Backend API running on `http://localhost:8004`
- [ ] Frontend running on `http://localhost:5173`
- [ ] Health check passes: `curl http://localhost:8004/health`
- [ ] Chat endpoint works: `curl -X POST http://localhost:8004/api/chat -H "Content-Type: application/json" -d '{"message":"test"}'`
- [ ] Frontend can make requests (check browser console for CORS errors)

---

**Your infrastructure is ready! 🚀**

The complete flow works:
```
Frontend (5173) → POST /api/chat → Backend (8004) → Ollama (11434) → Response
```
