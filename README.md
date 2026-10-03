# Goatilicious — Sorvetes Artesanais de Leite de Cabra 🍦🐐

Projeto prático desenvolvido para a cadeira de **Engenharia de Software** (ISUTC — 2º Semestre de 2026).

---

## 📌 Visão Geral

A **Goatilicious** é uma marca de sorvetes de luxo feitos artesanalmente com leite de cabra em Maputo, Moçambique. O sistema digitaliza as operações da marca através de duas interfaces integradas a uma API RESTful:
1. **Portal do Cliente:** Landing page interativa com visualização 3D de produtos, parallax, transição animada de sabores, navegação de cardápio e fluxo de registo.
2. **Painel do Staff / Administrativo:** Gestão de clientes cadastrados, catálogo de produtos, controlo de produção/estoque, ciclo de vida dos pedidos e faturação.

---

## 🛠️ Stack Tecnológica

### Backend
- **Java 17**
- **Spring Boot 3.2.5** (Spring Web, Spring Data JPA, Bean Validation)
- **H2 Database** (em memória para desenvolvimento e testes rápidos)
- **JUnit 5 & Mockito** (testes unitários e de integração de controllers e repositories)
- **Maven** (gestão de dependências e build)

### Frontend
- **Vite 8** (bundler moderno com HMR)
- **HTML5 & CSS3 Avançado** (glassmorphism, CSS Custom Properties, layout fluido e responsivo)
- **JavaScript ES6+**
- **@google/model-viewer & Three.js** (renderização WebGL 3D em tempo real com controle de câmera)
- **GSAP 3** (animações de física, parallax, repulsão de cursor e transições dinâmicas de temas)
- **@fontsource** (Galada, Inter e Manrope auto-hospedadas)

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java 17** ou superior instalado e configurado no `PATH`
- **Maven 3.8+**
- **Node.js v18+** e **npm**

---

### 1. Iniciar o Backend

Abra um terminal e execute:

```bash
cd backend
mvn clean spring-boot:run
```

- A API estará disponível em: `http://localhost:8080/api`
- Console do Banco H2: `http://localhost:8080/h2-console`
  - **JDBC URL:** `jdbc:h2:mem:goatilicious`
  - **User:** `sa`
  - **Password:** *(deixar em branco)*

Para rodar os testes automatizados do backend:
```bash
mvn test
```

---

### 2. Iniciar o Frontend

Em outro terminal, execute:

```bash
cd frontend
npm install
npm run dev
```

- A aplicação estará disponível em: `http://localhost:5173/`
- Para gerar o build de produção:
```bash
npm run build
```

---

## 🧭 Rotas e Páginas Principais

| Página | URL no Dev Server | Descrição |
| :--- | :--- | :--- |
| **Landing Page 3D** | `http://localhost:5173/` | Experiência visual interativa com modelo 3D, troca de sabores e CTAs |
| **Registo de Cliente** | `http://localhost:5173/cliente/cadastro.html` | Formulário integrado à API (`POST /api/clientes`) |
| **Painel de Clientes** | `http://localhost:5173/admin/clientes.html` | Listagem em tempo real de clientes (`GET /api/clientes`) |
