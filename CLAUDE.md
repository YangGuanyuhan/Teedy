# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Full build (skip tests)
mvn clean -DskipTests install

# Run development server (from docs-web/ directory)
mvn jetty:run
# Then access at http://localhost:8080/docs-web/src/

# Production WAR build
mvn -Pprod -DskipTests clean install

# Run all tests
mvn test

# Run a single test class
mvn test -pl docs-core -Dtest=TestJpa
mvn test -pl docs-web -Dtest=TestDocumentResource

# Frontend build (from docs-web/src/main/webapp/)
npm install
grunt

# Static analysis (PMD)
mvn pmd:check
```

## Architecture

Teedy is a Java 11 Maven multi-module document management system with an AngularJS frontend.

### Module Structure

- **`docs-core`** — Business logic JAR: JPA entity models, DAOs, services, async event listeners, file format handlers, Lucene indexing, encryption utilities
- **`docs-web-common`** — Shared web library: security filters (`TokenBasedSecurityFilter`, `JwtBasedSecurityFilter`, `HeaderBasedSecurityFilter`), principal classes, REST exception hierarchy, base test class for Jersey integration tests
- **`docs-web`** — WAR webapp: JAX-RS REST resources under `com.sismics.docs.rest.resource` + AngularJS SPA frontend under `src/main/webapp/src/`
- **`docs-importer`** — Standalone Node.js CLI tool for bulk file import (separate, not a Maven module)

### Backend Layers (docs-core)

```
resource (JAX-RS) → dao → model/jpa (Hibernate entities)
                   ↓
              event/listener (Guava EventBus, async)
```

- **DAO pattern**: All DAOs (`DocumentDao`, `UserDao`, etc.) use `ThreadLocalContext.get().getEntityManager()` to obtain the current JPA `EntityManager` — no injection framework
- **Model entities**: JPA entities under `model/jpa/` with soft-delete convention (`deleteDate` field)
- **Async event system**: Google Guava EventBus listeners under `listener/async/` handle file processing (OCR, thumbnail generation), Lucene index rebuilds, email sending, webhook triggers — all via `@Subscribe` annotated methods
- **Format handlers**: `util/format/` contains pluggable handlers for PDF, ODT, DOCX, PPTX, image, video, EML, text, and ZIP file processing
- **Full-text search**: Apache Lucene 8 for indexing and querying documents with highlighting and suggestions
- **File encryption**: 256-bit AES per-user encryption with keys stored in the database (BouncyCastle provider)

### REST API (docs-web)

- **JAX-RS resources**: Each resource (`DocumentResource`, `UserResource`, `FileResource`, etc.) extends `BaseResource` which provides `authenticate()`, role checks via `checkBaseFunction()`, and ACL target resolution
- **Auth flow**: `SecurityFilter` reads HTTP headers (`Bearer` token, `X-Auth-Token`, etc.), sets a `Principal` on the request. Resources call `authenticate()` to validate
- **Media types**: Resources consume `APPLICATION_FORM_URLENCODED`, produce `APPLICATION_JSON`

### Frontend (docs-web/src/main/webapp/)

- **AngularJS 1.x SPA** with UI-Router, angular-translate (i18n), Restangular for API calls
- **Two entry points**: `index.html` (main app) and `share.html` (public document sharing)
- **Grunt build**: `ngAnnotate` → `concat` (bundles JS into `docs.js` / `share.js`) → `uglify` → less compilation
- API calls go to relative path `../api/` which hits the Jersey servlet

### Testing

- **Unit/integration tests**: JUnit 4, H2 in-memory database (`jdbc:h2:mem:docs`), each test runs in a transaction that rolls back via `BaseTransactionalTest`
- **REST tests**: Extend `BaseJerseyTest` — starts an embedded Grizzly HTTP server + Wiser mock SMTP server, uses external test container factory
- Test files live alongside source in `src/test/`; shared test infrastructure in `docs-web-common/src/test/`

### Configuration

- `docs-core/src/main/resources/config.properties` — contains `db.version` for automatic schema migration
- `hibernate.properties` — separate for dev (`docs-web/src/dev/resources/`), prod (`docs-web/src/prod/resources/`), and test
- `docs-web/src/main/webapp/WEB-INF/web.xml` — servlet/filter chain: CORS → request context → auth filters → Jersey servlet mapping `/api/*`
- Environment variables for Docker: `DOCS_BASE_URL`, `DOCS_ADMIN_EMAIL_INIT`, `DOCS_ADMIN_PASSWORD_INIT` (bcrypt hash)

### Key Dependencies

- Hibernate 6.0, Jetty 11, Jersey 3.0, Lucene 8.7, PDFBox 2.0, BouncyCastle 1.70, FreeMarker 2.3
- Run-time native tools: Tesseract 4 (OCR), ffmpeg (video thumbnails), mediainfo
