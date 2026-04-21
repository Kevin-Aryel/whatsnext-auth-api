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

| Perfil | Banco              | CORS origins                          | Uso                     |
|--------|--------------------|---------------------------------------|-------------------------|
| local  | PostgreSQL Docker  | http://localhost:5173                 | Desenvolvimento         |
| test   | H2 in-memory       | nenhum (MockMvc)                      | JUnit / testes internos |
| prod   | Render PostgreSQL  | https://kevinaryeldev.github.io       | Deploy no Render        |

**localhost nunca é origin permitida em produção.**

## Variáveis de ambiente

| Variável                   | Exemplo                          | Obrigatória em prod |
|----------------------------|----------------------------------|---------------------|
| DB_URL                     | jdbc:postgresql://host/db        | Sim                 |
| DB_USERNAME                | postgres                         | Sim                 |
| DB_PASSWORD                | secret                           | Sim                 |
| JWT_SECRET                 | base64-string-min-32-chars       | Sim                 |
| JWT_ACCESS_EXPIRATION      | 900 (segundos)                   | Não (default: 900)  |
| JWT_REFRESH_EXPIRATION     | 604800 (segundos)                | Não (default: 7d)   |
| CORS_ALLOWED_ORIGINS       | https://kevinaryeldev.github.io  | Não                 |
| SPRING_PROFILES_ACTIVE     | prod                             | Sim                 |

JWT_SECRET deve ter mínimo 32 caracteres. Nunca commitar no repositório.

## Endpoints

| Método | Path                       | Auth    | Descrição                      |
|--------|----------------------------|---------|--------------------------------|
| POST   | /api/v1/auth/register      | Público | Cadastro (rate: 3 req/min/IP)  |
| POST   | /api/v1/auth/login         | Público | Login (rate: 5 req/min/IP)     |
| POST   | /api/v1/auth/refresh       | Público | Rotação de refresh token       |
| POST   | /api/v1/auth/logout        | Bearer  | Logout com blacklist           |
| GET    | /api/v1/user               | Bearer  | Perfil do usuário autenticado  |
| GET    | /actuator/health           | Público | Health check para o Render     |

## Estrutura de pacotes

```
com.whatsnext.authapi
├── config/          SecurityConfig, JwtConfig, OpenApiConfig, RateLimitConfig
├── controller/      AuthController, UserController
├── service/         AuthService, UserService, JwtService,
│                    TokenBlacklistService, TokenCleanupService
├── repository/      UserRepository, RefreshTokenRepository, TokenBlacklistRepository
├── domain/
│   ├── entity/      User, RefreshToken, TokenBlacklist
│   └── enums/       Role
├── dto/
│   ├── request/     RegisterRequest, LoginRequest, RefreshRequest
│   └── response/    AuthResponse, UserProfileResponse, ErrorResponse
├── exception/       GlobalExceptionHandler + exceções de domínio
└── filter/          JwtAuthenticationFilter, RateLimitFilter
```

## Armadilhas conhecidas

- **Circular dependency**: `SecurityConfig` define `UserDetailsService` como `@Bean`; `JwtAuthenticationFilter`
  depende dele via construtor — Spring detecta ciclo. Solução: `@Lazy` no parâmetro `UserDetailsService` do
  construtor de `JwtAuthenticationFilter`.
- **`AuthenticationEntryPoint` obrigatório**: sem ele, requests sem token retornam 403 em vez de 401. O bean
  deve ser declarado em `SecurityConfig` e registrado via `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`.
- **Ordenação de filtros**: não registrar dois filtros com `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.
  Usar `addFilterBefore(jwtFilter, RateLimitFilter.class)` para tornar a ordem explícita e não dependente
  da ordem de registro.
- **BCrypt strength=12 deixa testes lentos**: `AuthControllerTest` leva ~52s — comportamento esperado.
- **Isolamento de testes**: usar `@DirtiesContext(ClassMode.AFTER_EACH_TEST_METHOD)` nas classes de integração
  para resetar o contexto Spring entre testes e evitar contaminação do H2 in-memory.

## Decisões técnicas

- **Refresh token é UUID opaco** (não JWT) — permite revogação sem blacklist
- **Blacklist guarda SHA-256** do token, não o token bruto
- **Anti-enumeração**: login com email inexistente e senha errada retornam a mesma mensagem genérica
- **BCrypt strength=12** — mais lento que o default (10), intencional
- **Flyway para todas as migrations** — nunca `ddl-auto=create` em nenhum perfil
- **Rate limiting in-memory (Bucket4j)** — buckets resetam com reinício da instância; comportamento documentado
- **@Scheduled para limpeza** de tokens expirados (3h UTC) — só executa se a instância estiver acordada no Render free tier

## Testes com Rest Assured (auth-api-test)

Projeto separado. Configure a URL base antes de rodar:

```bash
export API_BASE_URL=http://localhost:8080          # local
export API_BASE_URL=https://sua-app.onrender.com   # Render
```

Isolamento de dados: cada teste cria usuário com email único (`<uuid>@test.com`). Sem endpoint de reset.

## Deploy no Render

O arquivo `render.yaml` na raiz define toda a infraestrutura.
Primeiro deploy: Flyway roda as migrations automaticamente.
A instância free dorme após 15 min de inatividade — cold start de ~30s é esperado.

## Convenções de código

- Java 21, sem preview features
- Records para DTOs imutáveis
- Lombok apenas em entidades (`@Data` proibido — usar `@Getter`/`@Setter` explícitos)
- Sem comentários óbvios; comentar apenas decisões não-óbvias
- Mensagens de commit em inglês, imperativo: `Add rate limiting filter`
- Nunca expor `passwordHash`, `tokenHash` ou `used` em DTOs de resposta
