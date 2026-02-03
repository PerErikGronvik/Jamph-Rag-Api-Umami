# Integrasjonsguide - Jamph-Rag-Api-Umami

Denne guiden forklarer hvordan du konfigurerer integrasjonene mellom Jamph-Rag-API, Ollama LLM og Umami frontend.

## 📍 Konfigurasjonssteder

### 1. Ollama LLM Adresse

**Fil:** [`api/src/main/resources/application.conf`](api/src/main/resources/application.conf)

```properties
ollama {
    baseUrl = "http://localhost:11434"
    baseUrl = ${?OLLAMA_BASE_URL}
    model = "llama3.2:3b"
    model = ${?OLLAMA_MODEL}
}
```

**Konfigurasjonsalternativer:**

**Alternativ 1: Endre direkte i application.conf**
```properties
ollama {
    baseUrl = "http://din-ollama-server:11434"
    model = "llama3.2:3b"
}
```

**Alternativ 2: Bruk miljøvariabler (anbefalt for produksjon)**
```bash
# Windows PowerShell
$env:OLLAMA_BASE_URL="http://din-ollama-server:11434"
$env:OLLAMA_MODEL="llama3.2:3b"

# Linux/Mac
export OLLAMA_BASE_URL="http://din-ollama-server:11434"
export OLLAMA_MODEL="llama3.2:3b"
```

**Standard verdier:**
- URL: `http://localhost:11434`
- Modell: `llama3.2:3b`

---

### 2. Umami Frontend Adresse

**Fil:** [`api/src/main/kotlin/no/jamph/ragumami/Application.kt`](api/src/main/kotlin/no/jamph/ragumami/Application.kt)

Finn `configureCORS()` funksjonen og oppdater `allowHost()` med din frontend-adresse:

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost("localhost:3000")      // ← Legg til din Umami adresse her
        allowHost("localhost:5173")      // ← Vite dev server (standard)
        allowCredentials = true
    }
}
```

**Eksempler på CORS-konfigurasjoner:**

```kotlin
// Lokal utvikling
allowHost("localhost:3000")
allowHost("localhost:5173")

// Produksjon
allowHost("umami.dindomene.no")
allowHost("analytics.dindomene.no", schemes = listOf("https"))

// Tillat alle (IKKE ANBEFALT for produksjon)
anyHost()
```

---

## 🚀 Verifisering av integrasjoner

### Sjekk Ollama-tilkobling

1. **Start Ollama:**
```bash
# Hvis Ollama kjører lokalt
ollama serve
```

2. **Test manuelt:**
```bash
curl http://localhost:11434/api/tags
```

3. **Fra API:** Når applikasjonen kjører, vil den automatisk koble til Ollama ved første forespørsel.

### Sjekk API-tilgjengelighet

1. **Start API:**
```bash
cd api
mvn clean install
mvn exec:java -Dexec.mainClass="no.jamph.ragumami.ApplicationKt"
```

2. **Test health endpoint:**
```bash
curl http://localhost:8004/health
```

Forventet svar:
```json
{
  "status": "healthy",
  "service": "rag-umami",
  "flavor": "umami"
}
```

### Sjekk CORS fra frontend

Fra din Umami frontend, test et enkelt kall:

```javascript
// JavaScript/TypeScript
fetch('http://localhost:8004/health')
  .then(response => response.json())
  .then(data => console.log('API tilgjengelig:', data))
  .catch(error => console.error('CORS eller tilkoblingsfeil:', error));
```

Hvis du får CORS-feil, sjekk at frontend-adressen din er lagt til i `allowHost()` i Application.kt.

---

## 🔧 API Port-konfigurasjon

**Standard port:** `8004`

**Endre port:**

**Alternativ 1: application.conf**
```properties
ktor {
    deployment {
        port = 8080  # Din ønskede port
    }
}
```

**Alternativ 2: Miljøvariabel**
```bash
# Windows PowerShell
$env:API_PORT="8080"

# Linux/Mac
export API_PORT="8080"
```

**Alternativ 3: Ved oppstart**
```bash
java -DAPI_PORT=8080 -jar target/api-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 📝 Feilsøking

### Ollama-tilkoblingsfeil

**Feilmelding:** `Connection refused` eller `Timeout`

**Løsninger:**
1. Sjekk at Ollama kjører: `ollama list`
2. Verifiser URL i application.conf
3. Test direkte: `curl http://localhost:11434/api/tags`
4. Sjekk firewall-regler

### CORS-feil fra frontend

**Feilmelding:** `Access-Control-Allow-Origin` eller `CORS policy`

**Løsninger:**
1. Verifiser at frontend-URL er lagt til i `allowHost()`
2. Sjekk at både HTTP-metoden og headers er tillatt
3. Restart API etter endringer i Application.kt
4. Bruk nettleserens DevTools for å se nøyaktig feilmelding

### Modell ikke funnet

**Feilmelding:** `model not found: llama3.2:3b`

**Løsning:**
```bash
# Last ned modellen
ollama pull llama3.2:3b

# Eller bruk en annen modell
ollama list  # Se tilgjengelige modeller
```

Deretter oppdater `ollama.model` i application.conf til en modell du har.

---

## 🔗 Relaterte filer

- **Ollama-klient:** [OllamaClient.kt](api/src/main/kotlin/no/jamph/ragumami/core/llm/OllamaClieant.kt)
- **Hovedapplikasjon:** [Application.kt](api/src/main/kotlin/no/jamph/ragumami/Application.kt)
- **Konfigurasjon:** [application.conf](api/src/main/resources/application.conf)

---

## 📚 Mer informasjon

- **Maven Setup:** Se [README.mavensetup.md](README.mavensetup.md)
- **Hoveddokumentasjon:** Se [README.md](README.md)
