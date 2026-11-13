# Desenho de desempenho e arquitectura

## Contexto e objectivo

O sistema deve continuar simples para desenvolvimento local, mas preparar as
rotas principais para crescimento sem carregar o dataset inteiro em memória.
Esta fase cobre backend e frontend, preserva o comportamento funcional actual
e reduz três falhas observadas: consultas N+1, listagens ilimitadas e
acoplamento entre entidades JPA e contratos HTTP.

O alvo não é introduzir microserviços, cache distribuído ou trocar H2 nesta
fase. A solução deve ser incremental e compatível com a API existente sempre
que o frontend já dependa do formato actual.

## Evidência actual

- `DashboardService.resumo()` usa `findAll()` para pedidos e faturas e faz
  agregação em memória.
- `PedidoService.registrar()` procura cada produto individualmente.
- `FacturaService.revisar()` procura cada produto individualmente.
- `Pedido.itens` está configurado como `FetchType.EAGER`.
- Controllers como `PedidoController` e `ClienteController` devolvem entidades
  JPA directamente.
- Listagens administrativas e de pedidos não têm paginação.
- O frontend já faz import dinâmico do runtime 3D, mas o modelo hero é eager e
  os modelos decorativos devem continuar fora do caminho crítico.

## Arquitectura alvo da fase

### Fronteiras backend

Controllers recebem requests e devolvem DTOs. Services mantêm regras de
negócio e transacções. Repositories expõem consultas específicas para cada
caso de uso, em vez de obrigar o service a carregar grafos JPA completos.

As entidades permanecem internas ao backend. Os DTOs de leitura devem
explicitar os campos necessários para cada página, evitando serialização
acidental de relações, hashes ou colecções não solicitadas.

### Persistência e consultas

1. Alterar a colecção de itens de pedido para carregamento lazy.
2. Para cada leitura que precisa de itens, usar `@EntityGraph`, `join fetch`
   ou projecção explícita dentro de uma consulta transaccional.
3. Na criação e revisão de pedidos, recolher todos os IDs de produto,
   executar `findAllById` e validar o conjunto completo antes de alterar a
   entidade. A ordem do request continua a determinar a ordem dos itens.
4. Adicionar paginação com parâmetros `page`, `size` e `sort`, impondo um
   tamanho máximo no servidor. A resposta deve conter itens e metadados de
   paginação; o frontend será adaptado para esse envelope.
5. Criar consultas agregadas para totais do dashboard, estados de pedidos,
   produtos mais pedidos e clientes com mais faturas. As regras actuais
   permanecem: excluir pedidos cancelados quando aplicável, considerar apenas
   faturas finais e manter o desempate actual do top-5.

### Frontend

As páginas administrativas e de pedidos passam a consumir o envelope
paginado e a controlar `page`, `size`, carregamento, erro e navegação. A
camada partilhada de autenticação/request continua responsável por cookies,
CSRF e respostas de erro.

O modelo 3D hero mantém prioridade visual. Modelos decorativos e pré-cargas
alternativas só podem iniciar depois do carregamento do hero e em idle. Não
serão removidos assets sem uma medição que mostre que a experiência principal
não piora.

## Compatibilidade e contratos

- Rotas e permissões existentes permanecem.
- Os dados expostos pelos DTOs não incluem credenciais, hashes ou relações
  internas.
- Clientes podem solicitar uma página explicitamente; valores inválidos ou
  excessivos recebem validação determinística.
- O filtro por cliente continua a ocorrer no backend e não pode ser delegado
  ao frontend.
- A alteração do envelope de paginação será aplicada simultaneamente nos
  consumidores frontend existentes; não haverá fallback silencioso para
  respostas incompletas.

## Sequência de implementação

1. Definir DTOs de leitura e contratos de página a partir dos formatos actuais.
2. Implementar consultas batch de produtos e testes de IDs ausentes.
3. Remover `EAGER` e adicionar consultas de leitura explícitas.
4. Paginar listagens backend e adaptar páginas frontend.
5. Substituir a agregação completa do dashboard por consultas agregadas.
6. Medir queries e carregamento inicial, corrigindo regressões.

## Critérios de aceitação

- Nenhuma listagem administrativa ou de pedidos executa `findAll()` sem
  paginação no caminho HTTP.
- Criar ou rever um pedido com vários itens não executa uma consulta individual
  por produto.
- O dashboard devolve os mesmos resultados funcionais sem carregar todos os
  pedidos/facturas como entidades para agregação.
- A API não serializa relações JPA por acidente.
- Os testes cobrem autorização por cliente, paginação, batch-loading e
  preservação das regras do dashboard.
- `mvn test` e `npm run frontend:build` terminam com sucesso.
- Uma medição comparativa documenta queries observadas e tamanho do build
  relevante para o caminho inicial.

## Não objectivos

- Migração imediata para PostgreSQL.
- Cache distribuído ou processamento assíncrono do dashboard.
- Separação em microserviços.
- Alterações cosméticas não relacionadas com desempenho.
