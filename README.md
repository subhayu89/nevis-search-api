# Nevis Search API

Spring Boot service exposing semantic document search and an embedding endpoint backed by local Ollama/LocalAI by default.

## Approach
- Spring Boot provides the REST API layer and application wiring.
- Documents are stored through JPA and enriched with embeddings at write time.
- Search generates an embedding for the query, compares it with stored document embeddings using cosine similarity, and merges those results with client text search results.
- The embedding provider is accessed through an abstraction so the implementation can be swapped between local and remote providers.
- API responses use explicit DTOs and centralized exception handling to keep the HTTP contract predictable.
- Ollama/LocalAI is used as the default embedding provider so the project can run locally without any paid API dependency.
- A deterministic fallback embedding mode is available for local Docker testing when Ollama is unreachable.
- H2 is used for simple local and Docker testing, while PostgreSQL configuration remains available for a persistent environment.

## Workflow
1. Start Ollama locally and pull a small embedding model such as `all-minilm`, or rely on Docker fallback mode for local smoke testing.
2. Start the API locally with Maven or in Docker with the provided compose file.
3. Verify the app with `GET /api/health`.
4. Test `POST /api/embedding`, `POST /api/documents`, and `GET /api/search`, including validation and failure paths.
5. Commit locally with Git and push to GitHub over HTTPS.

## Assumptions
- Local development should work without any paid external API dependency.
- Embeddings can be generated synchronously during document creation for the current scope.
- H2 is sufficient for local validation, while PostgreSQL is intended for more realistic persistent environments.
- The current dataset size is small enough that in-memory scoring over stored document embeddings is acceptable.
- Client search can be handled with basic text matching while document search uses semantic similarity.
- Consumers of the API benefit from stable, typed response payloads rather than loose object-shaped responses.

## Tradeoffs
- Embeddings are stored as JSON text rather than in a vector-native database, which keeps setup simple but does not scale well for large datasets.
- Semantic search is computed in application code, which is easy to understand and demo but less efficient than database-level vector indexing.
- H2 in Docker removes infrastructure friction, but behavior may differ slightly from PostgreSQL in production.
- Ollama removes API cost and key management, but model quality, model availability, and local machine resources become operational dependencies.
- Fallback embeddings keep the app testable when Ollama is unavailable, but semantic quality is much weaker than a real embedding model.
- The current API now has explicit validation and structured error handling, but it still favors straightforward implementation over advanced resilience features such as retries, async embedding jobs, or caching.

## Future Improvements
- Replace JSON-stored embeddings with a vector-capable database such as PostgreSQL with `pgvector`.
- Move semantic similarity search closer to the database layer instead of scanning all documents in application memory.
- Introduce asynchronous document ingestion so embedding generation does not block write requests.
- Expand automated coverage further with repository integration tests and more failure-mode contract tests.
- Add production-ready health/readiness checks, metrics, and structured logging for easier operations.

## Endpoints
- `GET /api/health` — lightweight health check returning `{ "status": "ok" }`.
- `POST /api/embedding` — body `{ "text": "..." }` returns `{ "embedding": [ ... ] }`.
- `POST /api/documents` — body `{ "clientId": "...", "title": "...", "content": "..." }` stores a document and embeds its content.
- `GET /api/search?q=...` — semantic search combining typed client and document results.
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
The compose file also enables deterministic fallback embeddings, so the app remains testable even if Ollama is unreachable from the container.

## API smoke test
```bash
curl http://localhost:8080/api/health

curl -X POST http://localhost:8080/api/embedding \
  -H "Content-Type: application/json" \
  -d '{"text":"hello world"}'

curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"clientId":null,"title":"Test doc","content":"address proof requirements"}'

curl "http://localhost:8080/api/search?q=address%20proof%20requirements"
```

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
- Unit/integration tests use an H2 in-memory DB and stubbed/mocked embedding provider behavior, so they do not depend on Ollama or OpenAI.
```bash
mvn test
```

## Configuration
- `EMBEDDING_BASE_URL` (default `http://host.docker.internal:11434/v1/embeddings` for Ollama/LocalAI)
- `EMBEDDING_MODEL` (default `nomic-embed-text`; `all-minilm` is a lighter local option)
- `EMBEDDING_FALLBACK_ENABLED` (enables deterministic local embeddings when the configured provider is unavailable)
- `EMBEDDING_FALLBACK_DIMENSIONS` (vector size used by the fallback embedding generator)
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
- Added validation, structured API error responses, typed search payloads, and broader unit/integration test coverage.
- Added deterministic fallback embeddings so Docker-based local testing does not hang when Ollama is unavailable.

## GitHub HTTPS push
```bash
git add .
git commit -m "Update project"
git remote set-url origin https://github.com/subhayu89/nevis-search-api.git
git push -u origin main
```
