# Nevis Search API

Spring Boot service exposing only the task-relevant client and document search APIs for a WealthTech workflow, backed by local Ollama/LocalAI by default.

## Approach
- Spring Boot provides the REST API layer and application wiring.
- Documents are stored through JPA and enriched with embeddings at write time.
- Search generates an embedding for the query, compares it with stored document embeddings using cosine similarity, and merges those results with client text search results.
- Document search uses hybrid ranking: embedding similarity plus lexical and synonym-aware matching for practical local behavior.
- The embedding provider is accessed through an abstraction so the implementation can be swapped between local and remote providers.
- API responses use explicit DTOs and centralized exception handling to keep the HTTP contract predictable.
- Ollama/LocalAI is used as the default embedding provider so the project can run locally without any paid API dependency.
- A deterministic fallback embedding mode is available for local Docker testing when Ollama is unreachable.
- H2 is used for simple local and Docker testing, while PostgreSQL configuration remains available for a persistent environment.
- The codebase has been trimmed to keep only the files and tests that support the current public API surface.

## Workflow
1. Start Ollama locally and pull a small embedding model such as `all-minilm`, or rely on Docker fallback mode for local smoke testing.
2. Start the API locally with Maven or in Docker with the provided compose file.
3. Create a client with `POST /clients`.
4. Add a document with `POST /clients/{id}/documents`.
5. Run `GET /search?q=...` and verify client/document matches.
6. Commit locally with Git and push to GitHub over HTTPS.

## Assumptions
- Local development should work without any paid external API dependency.
- Embeddings can be generated synchronously during document creation for the current scope.
- H2 is sufficient for local validation, while PostgreSQL is intended for more realistic persistent environments.
- The current dataset size is small enough that in-memory scoring over stored document embeddings is acceptable.
- Client search can be handled with basic text matching while document search uses semantic similarity.
- Related document terms such as `address proof` and `utility bill` should be treated as meaningfully connected during search.
- Consumers of the API benefit from stable, typed response payloads rather than loose object-shaped responses.

## Tradeoffs
- Embeddings are stored as JSON text rather than in a vector-native database, which keeps setup simple but does not scale well for large datasets.
- Semantic search is computed in application code, which is easy to understand and demo but less efficient than database-level vector indexing.
- H2 in Docker removes infrastructure friction, but behavior may differ slightly from PostgreSQL in production.
- Ollama removes API cost and key management, but model quality, model availability, and local machine resources become operational dependencies.
- Fallback embeddings keep the app testable when Ollama is unavailable, but semantic quality is much weaker than a real embedding model.
- Hybrid local scoring improves practical search behavior, but it is still a heuristic layer rather than a true vector-search engine.
- The current API now has explicit validation and structured error handling, but it still favors straightforward implementation over advanced resilience features such as retries, async embedding jobs, or caching.

## Future Improvements
- Replace JSON-stored embeddings with a vector-capable database such as PostgreSQL with `pgvector`.
- Move semantic similarity search closer to the database layer instead of scanning all documents in application memory.
- Externalize synonym/concept mapping instead of keeping it as application code.
- Introduce asynchronous document ingestion so embedding generation does not block write requests.
- Expand automated coverage further with repository integration tests and more failure-mode contract tests.
- Add production-ready health/readiness checks, metrics, and structured logging for easier operations.

## Endpoints
- `POST /clients` — creates a client with first name, last name, email, description, and social links.
- `POST /clients/{id}/documents` — stores a document for a specific client and generates embeddings from content.
- `GET /search?q=...` — searches across clients and documents, returning typed client/document results.

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
curl -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"first_name":"John","last_name":"Doe","email":"john.doe@neviswealth.com","description":"Advisor","social_links":["https://linkedin.com/in/johndoe"]}'

curl -X POST http://localhost:8080/clients/<CLIENT_ID>/documents \
  -H "Content-Type: application/json" \
  -d '{"title":"Residency","content":"utility bill"}'

curl "http://localhost:8080/search?q=address%20proof"
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
- The remaining tests focus on the current API contract and the search behavior behind it.
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
- Added validation, structured API error responses, typed search payloads, and focused unit/integration test coverage.
- Added deterministic fallback embeddings so Docker-based local testing does not hang when Ollama is unavailable.
- Improved local search behavior with hybrid lexical and synonym-aware document matching.
- Reduced the public API surface to essential task endpoints only: `/clients`, `/clients/{id}/documents`, and `/search`.
- Removed stale OpenAPI/embedding endpoint support and redundant tests that were no longer tied to the current API contract.

## GitHub HTTPS push
```bash
git add .
git commit -m "Update project"
git remote set-url origin https://github.com/subhayu89/nevis-search-api.git
git push -u origin main
```
