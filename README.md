# Nevis Search API

Spring Boot service exposing semantic document search and an embedding endpoint backed by local Ollama/LocalAI by default.

## Endpoints
- `GET /api/health` — lightweight health check returning `{ "status": "ok" }`.
- `POST /api/embedding` — body `{ "text": "..." }` returns `{ "embedding": [ ... ] }`.
- `POST /api/documents` — body `{ "clientId": "...", "title": "...", "content": "..." }` stores a document and embeds its content.
- `GET /api/search?q=...` — semantic search combining clients and documents.
- Swagger UI available at `/swagger-ui.html`.

## Prerequisites
- Java 17
- Maven
- Ollama running locally for embeddings

## Local run
```bash
EMBEDDING_BASE_URL=http://localhost:11434/v1/embeddings \
EMBEDDING_MODEL=all-minilm \
SPRING_DATASOURCE_URL='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1' \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect \
SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
mvn spring-boot:run
```

## Docker
```bash
docker compose up --build
```
Compose starts the app on port 8080 with an in-memory H2 database and expects Ollama on the host at `http://localhost:11434`.

### Local build
1. `mvn clean package` (produces an executable Spring Boot jar)
2. `java -jar target/search-api-1.0.0.jar`

### Docker build & run
```bash
docker build -t nevis-search .
docker run -p 8080:8080 \
  -e EMBEDDING_BASE_URL=http://host.docker.internal:11434/v1/embeddings \
  -e EMBEDDING_MODEL=all-minilm \
  -e "SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
  -e SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
  nevis-search
```
[application.properties](src/main/resources/application.properties)
## Tests
- Unit/integration tests use an H2 in‑memory DB and a stubbed `EmbeddingService` (no real OpenAI calls).
```bash
mvn test
```

## Configuration
- `EMBEDDING_BASE_URL` (default `http://host.docker.internal:11434/v1/embeddings` for Ollama/LocalAI)
- `EMBEDDING_MODEL` (default `nomic-embed-text`; `all-minilm` is a lighter local option)
- `OPENAI_API_KEY` (only needed if you point `EMBEDDING_BASE_URL` to `https://api.openai.com/v1/embeddings`)
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` for PostgreSQL

## Using Ollama/LocalAI (free local embeddings)
1. Install and run Ollama locally; pull a model: `ollama pull all-minilm`
2. Start Ollama (listens on `http://localhost:11434`).
3. Run the app (or Docker) with no `OPENAI_API_KEY` required:
   - Host: `EMBEDDING_BASE_URL=http://localhost:11434/v1/embeddings`
   - Docker: `EMBEDDING_BASE_URL=http://host.docker.internal:11434/v1/embeddings`

## Recent changes
- Added semantic search pipeline (documents + clients) with embeddings.
- Dockerfile/compose aligned to local Docker + H2 + Ollama workflow.
- Test profile uses H2 and stub embeddings for offline testing.
