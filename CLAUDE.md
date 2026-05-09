# auth-api

API de autenticação do ecossistema WhatsNext.
Stack: Spring Boot 3 + Spring Security 6 + PostgreSQL + JWT (JJWT 0.12.x)

## Contexto do projeto

Serve dois propósitos simultâneos:
1. Backend de autenticação para https://kevinaryeldev.github.io/front-to-do
2. Alvo de testes automatizados com Rest Assured (projeto separado `auth-api-test`)

É um laboratório de portfólio — o deploy no Render é real mas não tem usuários de produção.
Decisões de design priorizam demonstração de boas práticas de segurança enterprise.

## Como rodar localmente

Pré-requisitos: Docker, JDK 21, Maven 3.9+

```bash
# Subir o banco
docker compose up -d

# Rodar a aplicação (perfil local)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Rodar testes internos (JUnit + MockMvc, perfil test com H2)
mvn test
```

API sobe em http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

## Perfis de ambiente

| Perfil | Banco                            | CORS origins                          | Uso                                   |
|--------|----------------------------------|---------------------------------------|---------------------------------------|
| local  | PostgreSQL Docker                | http://localhost:5173                 | Desenvolvimento                       |
| test   | H2 in-memory                     | `*`                                   | JUnit / `@SpringBootTest` (MockMvc)   |
| e2e    | H2 file + AUTO_SERVER=TRUE       | `*`                                   | Stack `test,e2e` no `spring-boot:start` |
| prod   | Render PostgreSQL                | https://kevinaryeldev.github.io       | Deploy no Render                      |

**localhost nunca é origin permitida em produção.**

Os YAMLs de profile (`application-{local,test,e2e,prod}.yml`) ficam em `src/main/resources/` —
o `spring-boot:start` carrega só do main classpath, então YAML em `src/test/resources/` é invisível
pra app iniciada via Maven (descoberta dolorosa via S-01 do audit antigo).

## Variáveis de ambiente

| Variável                        | Exemplo                              | Obrigatória em prod      |
|---------------------------------|--------------------------------------|--------------------------|
| DB_URL                          | jdbc:postgresql://host:5432/db       | Sim                      |
| DB_USERNAME                     | postgres                             | Sim                      |
| DB_PASSWORD                     | secret                               | Sim                      |
| JWT_SECRET                      | base64-string-min-32-chars           | Sim                      |
| JWT_ACCESS_EXPIRATION           | 900 (segundos)                       | Não (default: 900)       |
| JWT_REFRESH_EXPIRATION          | 604800 (segundos)                    | Não (default: 7d)        |
| CORS_ALLOWED_ORIGINS            | https://kevinaryeldev.github.io      | Não                      |
| SPRING_PROFILES_ACTIVE          | prod                                 | Sim                      |
| PORT                            | 10000 (Render injeta automaticamente)| Sim em PaaS              |
| RATE_LIMIT_LOGIN_CAPACITY       | 5 (req/min/IP)                       | Não (default: 5)         |
| RATE_LIMIT_REGISTER_CAPACITY    | 3 (req/min/IP)                       | Não (default: 3)         |
| RATE_LIMIT_REFRESH_CAPACITY     | 10 (req/min/IP)                      | Não (default: 10)        |
| LOGIN_ATTEMPT_MAX_ATTEMPTS      | 5 (falhas para travar email)         | Não (default: 5)         |
| LOGIN_ATTEMPT_LOCK_MINUTES      | 15 (duração do lock)                 | Não (default: 15)        |

JWT_SECRET deve ter mínimo 32 caracteres. Nunca commitar no repositório.

`DB_URL` deve estar **em formato JDBC com host/porta/db separados de credenciais** —
`jdbc:postgresql://HOST:PORT/DB`. Não embutir `user:pass@` na URL: o driver do PostgreSQL
não aceita user-info no URI JDBC e quebra com "JDBC URL invalid port number". Render
expõe `host`, `port`, `database` separados na página do Postgres; monte manualmente.

## Endpoints

| Método | Path                       | Auth    | Descrição                                                          |
|--------|----------------------------|---------|--------------------------------------------------------------------|
| POST   | /api/v1/auth/register      | Público | Cadastro; 201; rate 3 req/min/IP                                   |
| POST   | /api/v1/auth/login         | Público | Login; 200; rate 5 req/min/IP + lockout 5 falhas/15min por email   |
| POST   | /api/v1/auth/refresh       | Público | Rotação de refresh token; rate 10 req/min/IP                       |
| POST   | /api/v1/auth/logout        | Bearer  | Logout (access blacklisted; refresh body opcional)                 |
| GET    | /api/v1/user               | Bearer  | Perfil do usuário autenticado                                      |
| GET    | /actuator/health           | Público | Health check para o Render                                         |

**Correlation ID:** toda resposta carrega `X-Request-Id` (UUID); respostas de erro também o repetem
em `meta.requestId`. Use esse id em logs/relatórios pra correlacionar.

**Cabeçalhos de segurança:** todas as respostas vêm com `X-Content-Type-Options: nosniff`,
`X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. Sobre HTTPS,
adiciona `Strict-Transport-Security: max-age=1y; includeSubDomains`.

## Estrutura de pacotes

```
com.whatsnext.authapi
├── config/          SecurityConfig, JwtConfig, OpenApiConfig, RateLimitConfig
├── controller/      AuthController, UserController
├── service/         AuthService, UserService, JwtService,
│                    TokenBlacklistService, TokenCleanupService,
│                    LoginAttemptService, PasswordValidator
├── repository/      UserRepository, RefreshTokenRepository, TokenBlacklistRepository
├── domain/
│   ├── entity/      User, RefreshToken, TokenBlacklist
│   └── enums/       Role
├── dto/
│   ├── request/     RegisterRequest, LoginRequest, RefreshRequest
│   └── response/    AuthResponse, UserProfileResponse, ErrorResponse
├── exception/       GlobalExceptionHandler + exceções de domínio
│                    (AccountLockedException, EmailAlreadyExistsException,
│                     InvalidTokenException, PasswordTooWeakException, TokenExpiredException)
└── filter/          JwtAuthenticationFilter, RateLimitFilter, RequestIdFilter
```

## Armadilhas conhecidas

- **`AuthenticationEntryPoint` obrigatório**: sem ele, requests sem token retornam 403 em vez de 401. O bean
  deve ser declarado em `SecurityConfig` e registrado via `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`.
- **Ordenação de filtros**: `RequestIdFilter` (HIGHEST_PRECEDENCE) antes de tudo, depois `RateLimitFilter`,
  depois `JwtAuthenticationFilter`. Não usar dois `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`
  — explicite a ordem entre filtros próprios.
- **BCrypt strength=12 deixa testes lentos**: `AuthControllerTest` leva ~52s — comportamento esperado.
- **Isolamento de testes**: usar `@DirtiesContext(ClassMode.AFTER_EACH_TEST_METHOD)` nas classes de integração
  para resetar o contexto Spring entre testes e evitar contaminação do H2 in-memory.
- **TestNG suite enumera classes**: ao adicionar uma classe de teste nova, registrar em
  `src/test/resources/suites/unitSuite.xml` ou ela é silenciosamente ignorada pelo Surefire.
- **Render `connectionString` é URI, não JDBC**: `postgres://user:pass@host/db` não casa com o driver JDBC
  do PostgreSQL (rejeita user-info no URI). Configurar `DB_URL` manualmente no formato
  `jdbc:postgresql://host:port/db` com `DB_USERNAME`/`DB_PASSWORD` separados.

## Decisões técnicas

- **Refresh token é UUID opaco** (não JWT) — permite revogação sem blacklist.
- **Rotação atômica de refresh token** (S-05): `refreshTokenRepository.deleteById(...)` em vez de
  `setUsed(true)+save`; remove a janela de race em refreshes concorrentes.
- **Blacklist guarda SHA-256** do token, não o token bruto.
- **Anti-enumeração**: login com email inexistente e senha errada retornam a mesma mensagem genérica.
- **BCrypt strength=12** — mais lento que o default (10), intencional.
- **JWT carrega claim `roles`** (S-04): `JwtAuthenticationFilter` constrói `GrantedAuthority` direto do token
  e não chama `UserDetailsService.loadUserByUsername` por request — economiza um hit no DB por chamada autenticada.
  `UserDetailsService` continua existindo só para o `DaoAuthenticationProvider` no fluxo de login com senha.
- **Account lockout in-memory** (S-08): 5 falhas consecutivas travam o email por 15 min. Estado é
  `ConcurrentHashMap` por instância, reseta no restart — aceitável no Render free tier (single-instance).
  Distribuído precisaria de Redis.
- **Flyway para todas as migrations** — nunca `ddl-auto=create` em nenhum perfil.
- **Rate limiting in-memory (Bucket4j)** com buckets independentes para `/login`, `/register`, `/refresh`;
  buckets resetam no restart, comportamento documentado.
- **Bucket key por `request.getRemoteAddr()`** (S-01): `X-Forwarded-For` é ignorado porque é spoofable —
  cliente rotacionando o header ganharia bucket novo a cada request. Atrás de um proxy adicional,
  configurar `server.forward-headers-strategy=native` para que o RemoteIpValve reescreva `getRemoteAddr()`
  só vindo de CIDRs confiáveis.
- **Cabeçalhos de segurança** (S-03): HSTS (1 ano + includeSubDomains), `X-Frame-Options: DENY`,
  `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` em todas as respostas.
- **Correlation ID por request** (S-07): `RequestIdFilter` em precedência máxima injeta UUID em SLF4J MDC e
  no header `X-Request-Id` da resposta. `ErrorResponse.meta.requestId` propaga o id ao cliente; `@JsonInclude(NON_NULL)`
  evita poluir respostas montadas fora do contexto de filter (factory direta em testes).
- **`ErrorResponse.code` = HTTP status como String** (`"401"`, `"422"`, `"429"`...) — não SCREAMING_SNAKE.
  `title` é o problema geral (`Unauthorized`, `Validation Error`), `detail` a mensagem específica.
- **Logout fora de `permitAll`** (S-06): apenas `/register`, `/login`, `/refresh` são públicos no
  `SecurityConfig`. Header `Authorization` ausente ou sem prefixo `Bearer ` cai no `AuthenticationEntryPoint`
  e devolve 401 com `ErrorResponse` padrão.
- **Stack trace nunca exposta**: `GlobalExceptionHandler.handleGeneric` registra a exception via SLF4J e
  devolve mensagem sanitizada. Handlers específicos cobrem `HttpMessageNotReadableException`,
  `MissingRequestHeaderException`, `MethodArgumentTypeMismatchException`, `HttpRequestMethodNotSupportedException`,
  `NoResourceFoundException` para evitar fallback no genérico 500.
- **@Scheduled para limpeza** de tokens expirados (3h UTC) — só executa se a instância estiver acordada no Render free tier.

## Testes com Rest Assured (auth-api-test)

Projeto separado. Configure a URL base antes de rodar:

```bash
export API_BASE_URL=http://localhost:8080          # local
export API_BASE_URL=https://sua-app.onrender.com   # Render
```

Isolamento de dados: cada teste cria usuário com email único (`<uuid>@test.com`). Sem endpoint de reset.

## Deploy no Render

Serviço gerenciado **via dashboard** — `render.yaml` foi removido porque a infra real foi criada
manualmente e o YAML virava cópia desatualizada do estado verdadeiro.

- **Auto-deploy desligado**: deploy é manual no dashboard ("Deploy latest commit") ou via tag
  `v*.*.*` que dispara o webhook em `RENDER_DEPLOY_HOOK_URL` no job `deploy` do CI.
- **`PORT` é injetado pelo Render**; `application.yml` lê via `${PORT:8080}`.
- **`DB_URL` precisa ser construído manualmente** no formato JDBC (ver seção Variáveis de ambiente)
  — não usar a `connectionString` URI que o `fromDatabase` exporia, ela é incompatível com o driver JDBC.
- **Flyway** roda as migrations no primeiro deploy automaticamente.
- A instância free dorme após 15 min de inatividade — cold start de ~30s é esperado.

## Convenções de código

- Java 21, sem preview features
- Records para DTOs imutáveis
- Lombok apenas em entidades (`@Data` proibido — usar `@Getter`/`@Setter` explícitos)
- Sem comentários óbvios; comentar apenas decisões não-óbvias
- Mensagens de commit em inglês, imperativo: `Add rate limiting filter`
- Nunca expor `passwordHash`, `tokenHash` ou `used` em DTOs de resposta
