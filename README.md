# whatsnext-auth-api

![CI](https://github.com/kevinaryeldev/whatsnext-auth-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)

API REST de autenticação do ecossistema WhatsNext. Alimenta o login do [front-to-do](https://kevinaryeldev.github.io/front-to-do) e serve como alvo de testes automatizados com REST Assured.

---

# 🧪 Testes E2E

> O principal propósito deste projeto é demonstrar uma suíte de testes E2E numa API de autenticação real.
Diferente de outros projetos construidos por mim, esse tem algumas particularidades, como a presença dos testes com restAssured no próprio projeto da API, os testes rodando por um banco H2 (eu sei, eu sei) na falta de um outro ambiente de deploy (HML) para configurar CI

Os testes rodam contra uma instância real do Spring Boot com banco H2 via TCP, iniciada e derrubada automaticamente pelo Maven Failsafe Plugin. Cada classe de teste isola seus próprios dados inserindo usuários diretamente via JDBC — sem dependência do endpoint `/register`.

## Executando os testes

```bash
mvn test          # testes unitários (TestNG + MockMvc, H2 in-memory)
mvn verify        # unitários + E2E (REST Assured contra servidor real, H2 TCP)
mvn allure:report # relatório Allure → target/site/allure-maven-plugin/index.html
```

Para rodar E2E localmente, crie o arquivo `src/test/resources/config-local.properties` (gitignored):

```properties
api.base.url=http://localhost:8080
h2.jdbc.url=jdbc:h2:tcp://localhost:9092/mem:testdb;MODE=PostgreSQL
USER_PASS=Algo para rodar os testes.
```

## Cobertura

| Classe | Cenários |
|---|---|
| `RegisterE2ETest` | Cadastro válido, email duplicado, campos inválidos, senhas fracas |
| `LoginE2ETest` | Login válido, anti-enumeração (email inexistente = senha errada), campos inválidos |
| `RefreshE2ETest` | Rotação válida, reuso de token rotacionado, token inválido e malformado |
| `LogoutE2ETest` | Logout válido, requisição sem token |
| `UserE2ETest` | Perfil válido, sem token, token inválido, token blacklisted após logout |

Todas as respostas com body são validadas contra JSON Schemas, o contrato de erro é "emprestado" do padrão utilizado no OPF. (`errors[{code, title, detail}] + meta`).

## Arquitetura

```
e2e/
├── client/          AuthClient, UserClient
├── data/
│   ├── factory/     UserCredentialRecord, UserFactory, RequestSpecFactory
│   ├── provider/    RegisterDataProvider, LoginDataProvider, RefreshDataProvider
│   └── utils/       UserDataPreload, AuthDataPreload
└── auth/            RegisterE2ETest, LoginE2ETest, RefreshE2ETest, LogoutE2ETest
user/                UserE2ETest

resources/
├── schemas/         auth-response-schema.json, error-response-schema.json, user-profile-schema.json
└── suites/          unitSuite.xml, e2eSuite.xml
```

## Roadmap E2E

- Asserções via JDBC: buscar dados persistidos no H2 e comparar com o que a API retorna
- Cenário de refresh token expirado: inserir token vencido via JDBC e validar 401
- Configuração avançada de tags Allure: severity, owner e layer por classe de teste

---

## Stack

Spring Boot 3 · Spring Security 6 · PostgreSQL · Flyway · JJWT 0.12 · Bucket4j · SpringDoc

## Como rodar a API

**Pré-requisitos:** Docker, JDK 21, Maven 3.9+

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints

| Método | Path | Auth | Descrição |
|--------|------|------|-----------|
| POST | /api/v1/auth/register | Público | Cadastro (3 req/min/IP) |
| POST | /api/v1/auth/login | Público | Login (5 req/min/IP) |
| POST | /api/v1/auth/refresh | Público | Rotação de refresh token |
| POST | /api/v1/auth/logout | Bearer | Logout + blacklist |
| GET | /api/v1/user | Bearer | Perfil autenticado |
| GET | /actuator/health | Público | Health check |

## Variáveis de ambiente

| Variável | Obrigatória em prod | Padrão |
|----------|---------------------|--------|
| DB_URL | Sim | — |
| DB_USERNAME | Sim | — |
| DB_PASSWORD | Sim | — |
| JWT_SECRET | Sim | — |
| CORS_ALLOWED_ORIGINS | Não | - |
| JWT_ACCESS_EXPIRATION | Não | 900 (segundos) |
| JWT_REFRESH_EXPIRATION | Não | 604800 (segundos) |