# Desempenho e Arquitectura Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduzir consultas e payloads desnecessários no backend e o trabalho inicial do frontend, mantendo H2, as permissões e o comportamento funcional actual.

**Architecture:** O backend passa a separar entidades JPA de DTOs HTTP, usar consultas paginadas/agregadas e carregar relações apenas nos casos de uso que precisam delas. O frontend consome envelopes paginados e mantém o 3D hero prioritário, adiando trabalho decorativo. H2 continua a ser a base de dados do projecto.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Data JPA, H2, JUnit 5/MockMvc, Vite 8, JavaScript ES modules, model-viewer e GSAP.

**Spec:** `docs/superpowers/specs/2026-11-13-performance-architecture-design.md`

## Global Constraints

- H2 continua a ser a base de dados adoptada para desenvolvimento, testes e execução desta aplicação.
- Não introduzir migração para PostgreSQL, microserviços ou cache distribuído nesta fase.
- Preservar as rotas, permissões e regras de isolamento por cliente existentes.
- Não expor credenciais, hashes ou relações JPA internas nos DTOs.
- Impor tamanho máximo para páginas no servidor; não aceitar listagens ilimitadas no caminho HTTP.
- Preservar a exclusão de pedidos cancelados, o filtro de faturas finais e os desempates actuais do top-5.
- Não remover assets 3D sem medição que mostre que a experiência hero não piora.

## Review Focus

- Pedido com vários itens e um ID de produto inexistente deve falhar sem alterar o pedido nem criar factura parcial — testar em `PedidoService` e no fluxo HTTP.
- Revisão com produtos repetidos ou inexistentes deve preservar o pedido original — testar em `FacturaService`.
- Cliente autenticado nunca pode paginar ou obter pedidos de outro cliente alterando `page`, `size` ou `sort` — testar autorização e filtro no controller.
- Página com `size` negativo, zero ou acima do máximo deve ter resposta de validação determinística — testar os endpoints paginados.
- Dashboard com pedidos cancelados, faturas rascunho e empates deve manter exactamente as regras actuais — testar as consultas agregadas e a resposta final.

---

### Task 1: Definir DTOs de leitura e envelope paginado

**Files:**
- Create: `backend/src/main/java/com/goatilicious/dtos/PaginaResposta.java`
- Create: `backend/src/main/java/com/goatilicious/dtos/PedidoResumo.java`
- Create: `backend/src/main/java/com/goatilicious/dtos/ProdutoResumo.java`
- Create: `backend/src/main/java/com/goatilicious/dtos/FacturaResumo.java`
- Modify: `backend/src/main/java/com/goatilicious/dtos/ClienteResumo.java`
- Test: `backend/src/test/java/com/goatilicious/dtos/ReadDtoContractTest.java`

**Interfaces:**
- Produces `PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas)`.
- Produces read DTO factories that accept only the fields needed by each existing page.
- Establishes the DTO types that the paginated service/controller task will consume.

- [ ] **Step 1: Write failing DTO contract tests** for page metadata defaults and absence of entity-only fields.
- [ ] **Step 2: Run `mvn -f backend/pom.xml -Dtest=ReadDtoContractTest test`**; verify the new contract types are not implemented.
- [ ] **Step 3: Implement `PaginaResposta<T>` and the four read DTOs** with explicit fields and static mapping methods; do not include entity relationships or password data.
- [ ] **Step 4: Run the focused DTO tests** and verify all contract assertions pass.
- [ ] **Step 5: Commit** with `git add` limited to the DTO/test files and message `feat: define paginated read contracts`.

### Task 2: Batch product loading and lazy order relations

**Files:**
- Modify: `backend/src/main/java/com/goatilicious/models/Pedido.java`
- Modify: `backend/src/main/java/com/goatilicious/services/PedidoService.java`
- Modify: `backend/src/main/java/com/goatilicious/services/FacturaService.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/ProdutoRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/PedidoRepository.java`
- Test: `backend/src/test/java/com/goatilicious/services/PedidoServiceTest.java`
- Test: `backend/src/test/java/com/goatilicious/services/FacturaServiceTest.java`
- Test: `backend/src/test/java/com/goatilicious/integration/SeparacaoClienteStaffIntegracaoTest.java`

**Interfaces:**
- `ProdutoRepository.findAllById(Iterable<Long>)` is the single batch lookup used by creation and review.
- `PedidoRepository` exposes explicit fetch methods for list/detail use cases; no caller relies on `Pedido.itens` being eager.
- `PedidoService.registrar` and `FacturaService.revisar` reject missing IDs before mutating entities.

- [ ] **Step 1: Add failing service tests** asserting multiple product IDs are loaded as one batch and missing IDs leave the transaction without a saved order/factura.
- [ ] **Step 2: Run the focused service tests** and verify the current per-item `findById` implementation fails the query interaction assertions.
- [ ] **Step 3: Change `Pedido.itens` to `FetchType.LAZY`** and add explicit repository fetch methods for the use cases that need items.
- [ ] **Step 4: Implement batch lookup preserving request order** by indexing the returned products by ID, validating the complete requested ID set, then constructing items.
- [ ] **Step 5: Run service and integration tests**, including client/staff isolation and the review edge cases.
- [ ] **Step 6: Commit** the model, repository, service, and test changes with `perf: batch order product loading`.

### Task 3: Paginate backend list use cases

**Files:**
- Modify: `backend/src/main/java/com/goatilicious/repositories/PedidoRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/ClienteRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/ProdutoRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/FacturaRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/services/PedidoService.java`
- Modify: `backend/src/main/java/com/goatilicious/services/ClienteService.java`
- Modify: `backend/src/main/java/com/goatilicious/services/FacturaService.java`
- Modify: `backend/src/main/java/com/goatilicious/controllers/PedidoController.java`
- Modify: `backend/src/main/java/com/goatilicious/controllers/ClienteController.java`
- Modify: `backend/src/main/java/com/goatilicious/controllers/ProdutoController.java`
- Modify: `backend/src/main/java/com/goatilicious/controllers/FacturaController.java`
- Test: `backend/src/test/java/com/goatilicious/integration/PaginacaoIntegracaoTest.java`

**Interfaces:**
- Services accept `Pageable` and return `Page<ReadDto>` or the equivalent `PaginaResposta<T>` mapping.
- Staff list endpoints use a deterministic default sort; client order endpoints always constrain by authenticated `clienteId`.
- No HTTP list endpoint calls unbounded `findAll()`.

- [ ] **Step 1: Add failing integration tests** for page metadata, default size, maximum size rejection, deterministic sorting, and client isolation across pages.
- [ ] **Step 2: Run `mvn -f backend/pom.xml -Dtest=PaginacaoIntegracaoTest test`** and record the expected failures against current list signatures.
- [ ] **Step 3: Replace unbounded repository calls** with `Pageable` repository methods and map pages to the read DTO envelope.
- [ ] **Step 4: Validate query parameters centrally** so invalid values return a clear 400 response rather than silently defaulting.
- [ ] **Step 5: Run the integration tests** and confirm no list endpoint returns an unbounded collection.
- [ ] **Step 6: Commit** with `feat: paginate backend list endpoints`.

### Task 4: Replace dashboard full scans with aggregate queries

**Files:**
- Modify: `backend/src/main/java/com/goatilicious/repositories/PedidoRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/repositories/FacturaRepository.java`
- Modify: `backend/src/main/java/com/goatilicious/services/DashboardService.java`
- Modify: `backend/src/main/java/com/goatilicious/dtos/DashboardResposta.java`
- Test: `backend/src/test/java/com/goatilicious/services/DashboardServiceTest.java`
- Test: `backend/src/test/java/com/goatilicious/integration/DashboardIntegracaoTest.java`

**Interfaces:**
- Repository aggregate methods return typed projections for revenue, order states, top products, and top clients.
- `DashboardService.resumo()` preserves the existing `DashboardResposta` shape and top-5 tie-breaking.
- Aggregate queries remain JPQL/H2-compatible and do not return full `Pedido` or `Factura` graphs.

- [ ] **Step 1: Add fixture-driven failing tests** covering cancelled orders, non-final invoices, paid/unpaid totals, and equal-count tie-breaks.
- [ ] **Step 2: Run the dashboard tests** and capture the current full-scan implementation as the baseline.
- [ ] **Step 3: Add typed projection records and repository aggregate queries** for each dashboard metric.
- [ ] **Step 4: Rewrite `DashboardService.resumo()`** to compose projections and retain the current response ordering and semantics.
- [ ] **Step 5: Add query-count instrumentation in the integration test** and assert the dashboard does not issue one query per order/item.
- [ ] **Step 6: Run dashboard unit/integration tests** and commit with `perf: aggregate dashboard metrics`.

### Task 5: Adapt frontend list consumers to pagination

**Files:**
- Modify: `frontend/shared/auth.js`
- Modify: `frontend/admin/clientes.js`
- Modify: `frontend/admin/gerir-pedidos.js`
- Modify: `frontend/admin/facturas.js`
- Modify: `frontend/admin/dashboard.js`
- Modify: `frontend/cliente/meus-pedidos.js`
- Modify: `frontend/cliente/pedido.js`
- Modify: relevant HTML files in `frontend/admin/` and `frontend/cliente/`
- Test: existing frontend build plus focused browser/manual request checks

**Interfaces:**
- Shared response handling exposes `itens`, `pagina`, `tamanho`, `total` and `totalPaginas`.
- Each list page owns its current page and renders next/previous controls without duplicating fetches.
- Existing session cookie and CSRF behavior remains unchanged.

- [ ] **Step 1: Add page-state and rendering tests/probes** for empty pages, first/last page controls, loading state, and API error state.
- [ ] **Step 2: Run the frontend build before implementation** to preserve the baseline artifact and identify the pages touched by the contract change.
- [ ] **Step 3: Update each list consumer** to send `page`, `size` and stable `sort`, render only `itens`, and disable controls at the correct boundaries.
- [ ] **Step 4: Ensure create/review flows refresh the current page once** and do not issue duplicate list requests.
- [ ] **Step 5: Run `npm run frontend:build`** and verify all HTML entrypoints bundle without warnings or missing imports.
- [ ] **Step 6: Commit** with `feat: consume paginated API responses`.

### Task 6: Protect the frontend critical path and measure

**Files:**
- Modify: `frontend/index.html`
- Modify: `frontend/src/main.js`
- Modify: `frontend/src/modules/scene.js`
- Modify: `frontend/src/modules/idle.js`
- Create: `docs/superpowers/measurements/2026-11-13-performance-baseline.md`
- Test: production build and browser performance probe

**Interfaces:**
- The hero model remains the first 3D asset requested.
- Decorative models and alternate flavor preloads start only after hero `load`/`error` and idle scheduling.
- The measurement records observed build asset sizes and request ordering; it does not invent a score.

- [ ] **Step 1: Capture the current production build asset sizes and request order** in the measurement file.
- [ ] **Step 2: Add a browser probe** that records first meaningful paint/LCP when available and the order of hero/decorative model requests.
- [ ] **Step 3: Adjust only preload priority, lazy/idle scheduling, or duplicate initialization** where the probe shows contention; keep the hero visual behavior unchanged.
- [ ] **Step 4: Run the browser probe** and compare before/after request ordering and measured values.
- [ ] **Step 5: Run `npm run frontend:build`** and verify the production bundle remains valid.
- [ ] **Step 6: Commit** with `perf: protect 3d critical path`.

### Task 7: Full regression and completion evidence

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/measurements/2026-11-13-performance-baseline.md`
- Test: backend and frontend validation commands

- [ ] **Step 1: Run `mvn -f backend/pom.xml test`** and record the complete test count, failures and errors.
- [ ] **Step 2: Run `npm run frontend:build`** and record the exit status and relevant bundle sizes.
- [ ] **Step 3: Run the focused integration flows** for authentication, pagination, order creation/review and dashboard semantics.
- [ ] **Step 4: Update README only with verified commands and configuration** if the API pagination behavior needs user-facing documentation.
- [ ] **Step 5: Review the diff** for accidental changes to H2, session security, API permissions or unrelated files.
- [ ] **Step 6: Commit the final documentation/evidence** with `docs: record performance validation`.

