# Design Spec — auth-api-test
**Data:** 2026-04-21
**Projeto:** auth-api-test — ecossistema WhatsNext
**Status:** Aprovado

---

## 1. Visão Geral

Suíte de testes automatizados para validar os contratos públicos e fluxos de segurança da `auth-api`. Reside no mesmo repositório (`whatsnext-auth-api`) como dependências de escopo `test`, sem criar um projeto Maven separado.

Dois conjuntos de testes com responsabilidades distintas:

| Conjunto | Framework | Fase Maven | Banco | Propósito |
|---|---|---|---|---|
| `unit` | TestNG + MockMvc | `test` | H2 in-memory | Lógica de serviços e contratos HTTP via mock |
| `e2e` | TestNG + REST Assured | `verify` | H2 TCP | Contratos HTTP reais contra servidor em execução |

---

## 2. Stack

| Componente | Tecnologia |
|---|---|
| Framework de testes | TestNG 7.10.2 |
| HTTP client (E2E) | REST Assured 5.5.0 |
| Relatório | Allure 2.27.0 (allure-testng + allure-rest-assured) |
| Banco de testes (unit) | H2 in-memory com MODE=PostgreSQL |
| Banco de testes (E2E) | H2 TCP server (mesmo processo do app) |
| Setup de dados E2E | JDBC puro via `TestDataHelper` |
| Servidor E2E | Spring Boot Maven Plugin (start/stop) |
| Build | Maven (Surefire + Failsafe) |

JUnit 5 é removido do projeto — TestNG é o único framework de testes. A dependência transitiva do JUnit via `spring-boot-starter-test` é excluída explicitamente.

---

## 3. Estrutura de pacotes

```
src/test/java/com/whatsnext/authapi/
├── unit/
│   ├── service/
│   │   ├── JwtServiceTest.java
│   │   ├── PasswordValidatorTest.java
│   │   └── AuthServiceTest.java
│   └── controller/
│       ├── AuthControllerTest.java
│       └── UserControllerTest.java
└── e2e/
    ├── helper/
    │   ├── RequestSpecFactory.java
    │   ├── UserFactory.java
    │   ├── AuthHelper.java
    │   └── TestDataHelper.java
    ├── auth/
    │   ├── RegisterE2ETest.java
    │   ├── LoginE2ETest.java
    │   ├── RefreshE2ETest.java
    │   └── LogoutE2ETest.java
    └── user/
        └── UserE2ETest.java

src/test/resources/
├── testng-unit.xml
├── testng-e2e.xml
└── allure.properties

src/test/java/com/whatsnext/authapi/config/
└── H2ServerConfig.java         ← @Profile("test"), expõe H2 via TCP na porta 9092
```

---

## 4. Helpers E2E

### RequestSpecFactory
Ponto único de configuração da conexão REST Assured. URL configurável via variável de ambiente `API_BASE_URL` (default: `http://localhost:8080`).

```java
public class RequestSpecFactory {
    private static final String BASE_URL =
        System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
            .setBaseUri(BASE_URL)
            .setContentType(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();
    }

    public static RequestSpecification withBearer(String token) {
        return new RequestSpecBuilder()
            .addRequestSpecification(base())
            .addHeader("Authorization", "Bearer " + token)
            .build();
    }
}
```

### UserFactory
Gera `RegisterRequest` com email único por UUID. Garante isolamento entre testes sem dependência de estado externo.

```java
public class UserFactory {
    public static final String TEST_PASSWORD = "Test@1234!";

    public static String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@test.com";
    }
}
```

### TestDataHelper
Insere usuários diretamente no banco via JDBC. Sem dependência do endpoint `/register` — elimina cascata de falhas quando o endpoint de cadastro está quebrado.

Usa hash BCrypt **pré-computado** da senha padrão de testes (strength=12) para evitar custo de BCrypt em cada `@BeforeMethod`.

```java
public class TestDataHelper {
    private static final String JDBC_URL =
        "jdbc:h2:tcp://localhost:9092/mem:testdb;MODE=PostgreSQL";

    // Hash pré-computado de "Test@1234!" com BCrypt strength=12.
    // Gerar uma vez: new BCryptPasswordEncoder(12).encode("Test@1234!")
    // e substituir o valor abaixo. BCrypt usa salt aleatório — gere uma vez e fixe.
    private static final String TEST_PASSWORD_HASH = "$2a$12$SUBSTITUIR_AO_IMPLEMENTAR";

    public static String insertUser(String email) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id, name, email, password_hash, role, created_at, updated_at)" +
                " VALUES (?, 'Test User', ?, ?, 'USER', NOW(), NOW())");
            ps.setString(1, id);
            ps.setString(2, email);
            ps.setString(3, TEST_PASSWORD_HASH);
            ps.executeUpdate();
        }
        return id;
    }
}
```

### AuthHelper
Obtém tokens via `/login` para testes que precisam de autenticação. Depende apenas de um usuário já existente no banco (inserido via `TestDataHelper`).

```java
public class AuthHelper {
    public static AuthResponse login(String email) {
        return given(RequestSpecFactory.base())
            .body(new LoginRequest(email, UserFactory.TEST_PASSWORD))
            .post("/api/v1/auth/login")
            .then().statusCode(200)
            .extract().as(AuthResponse.class);
    }

    public static String accessToken(String email) {
        return login(email).accessToken();
    }
}
```

---

## 5. H2 TCP Server

Para que o `TestDataHelper` acesse o banco do app via JDBC de um processo externo, o H2 precisa expor um servidor TCP.

### H2ServerConfig.java (perfil `test`)
```java
@Configuration
@Profile("test")
public class H2ServerConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        return Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }
}
```

### application-test.yml (ajuste na URL)
```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost:9092/mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

Com esse setup, o app sobe o H2 em TCP, e o processo dos testes E2E conecta na mesma instância para inserir dados de fixture.

---

## 6. Cobertura de testes E2E

Rate limiting **não é testado** via E2E — buckets in-memory resetam com reinício da instância; comportamento não-determinístico em testes.

### RegisterE2ETest (9 cenários)
| Cenário | Status esperado |
|---|---|
| Dados válidos | 201 + accessToken + refreshToken não nulos |
| Email duplicado | 409 |
| Email em formato inválido | 422 |
| Nome vazio | 422 |
| Nome menor que 2 caracteres | 422 |
| Senha sem maiúscula | 422 |
| Senha sem dígito | 422 |
| Senha sem caractere especial | 422 |
| Senha com menos de 8 caracteres | 422 |

### LoginE2ETest (4 cenários)
| Cenário | Status esperado |
|---|---|
| Credenciais válidas | 200 + tokens |
| Email inexistente | 401 mensagem genérica |
| Senha errada | 401 **mesma mensagem** (anti-enumeração) |
| Body vazio | 422 |

### RefreshE2ETest (4 cenários)
| Cenário | Status esperado |
|---|---|
| Refresh token válido | 200 + novo par de tokens |
| Token já utilizado (rotação) | 401 |
| Token inválido / inexistente | 401 |
| Token antigo após rotação | 401 |

### LogoutE2ETest (2 cenários)
| Cenário | Status esperado |
|---|---|
| Bearer válido | 204 |
| Sem token | 401 |

### UserE2ETest (4 cenários)
| Cenário | Status esperado |
|---|---|
| Bearer válido | 200 + id, name, email, role |
| Sem token | 401 |
| Token inválido | 401 |
| Token blacklisted (após logout) | 401 — valida fluxo completo de blacklist |

**Total: ~23 cenários E2E**

---

## 7. Anotações Allure

Cada classe de teste usa anotações de nível de suite:

```java
@Epic("Auth API")
@Feature("Register")
public class RegisterE2ETest { ... }
```

Cada método usa `@Story` e, quando relevante, `@Description`:

```java
@Test
@Story("Validação de senha fraca")
@Description("Senha sem maiúscula deve retornar 422")
public void register_passwordWithoutUppercase_returns422() { ... }
```

`allure-rest-assured` captura automaticamente request/response de cada chamada HTTP no relatório — sem anotações extras.

---

## 8. Configuração Maven

### Surefire (fase `test`) — testes unitários
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <suiteXmlFiles>
            <suiteXmlFile>src/test/resources/testng-unit.xml</suiteXmlFile>
        </suiteXmlFiles>
    </configuration>
</plugin>
```

### Spring Boot Maven Plugin — start/stop do servidor para E2E
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>pre-integration-test</id>
            <goals><goal>start</goal></goals>
            <configuration>
                <profiles>test</profiles>
            </configuration>
        </execution>
        <execution>
            <id>post-integration-test</id>
            <goals><goal>stop</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Failsafe (fase `verify`) — testes E2E
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <suiteXmlFiles>
            <suiteXmlFile>src/test/resources/testng-e2e.xml</suiteXmlFile>
        </suiteXmlFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Allure Maven Plugin
```xml
<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.12.0</version>
</plugin>
```

---

## 9. Pipeline CI/CD

Três jobs sequenciais no `.github/workflows/ci.yml`:

```
unit-test → e2e-test → deploy
```

### job: unit-test
1. Checkout + JDK 21 Temurin
2. `mvn test`
3. Upload `target/allure-results` como artefato `allure-unit-results`
4. Gerar relatório Allure e upload como artefato `allure-unit-report`

### job: e2e-test (needs: unit-test)
1. Checkout + JDK 21 Temurin
2. `mvn verify` (Surefire + Spring Boot Plugin start/stop + Failsafe)
3. Upload `target/allure-results` como artefato `allure-e2e-results`
4. Gerar relatório Allure e upload como artefato `allure-e2e-report`

### job: deploy (needs: e2e-test)
1. `curl -X POST ${{ secrets.RENDER_DEPLOY_HOOK }}`

`RENDER_DEPLOY_HOOK` é configurado no dashboard do Render (Settings → Deploy Hook) e armazenado como secret no GitHub. Deploy só ocorre se unit **e** E2E passarem. Nenhuma credencial no código.

---

## 10. Conversão dos testes existentes para TestNG

Os 5 arquivos de teste existentes (JUnit5) são convertidos para TestNG:

| JUnit5 | TestNG |
|---|---|
| `@Test` (org.junit.jupiter) | `@Test` (org.testng) |
| `@BeforeEach` | `@BeforeMethod` |
| `@AfterEach` | `@AfterMethod` |
| `@BeforeAll` | `@BeforeClass` |
| `extends` nada | `extends AbstractTestNGSpringContextTests` |
| `Assertions.assertEquals` | `Assert.assertEquals` (TestNG) ou manter AssertJ |
| `@ExtendWith(SpringExtension)` | removido (AbstractTestNGSpringContextTests já inclui) |

AssertJ (`assertThat`) pode ser mantido nas assertions — é agnóstico de framework.

---

## 11. Ordem de execução

> **Update 2026-04-28:** Estrutura revisada com base no que foi implementado.
> Divergências em relação à spec original estão marcadas com ⚠️.

```
Fase 1 — Infraestrutura de testes                                    STATUS
  1.1 pom.xml migrado (TestNG, REST Assured, Allure, Failsafe)        ✅ FEITO
  1.2 H2ServerConfig.java (@Profile("test"), TCP porta 9092)          ✅ FEITO
  1.3 application-test.yml movido para src/test/resources             ✅ FEITO
      URL atualizada para H2 TCP                                      ✅ FEITO
  1.4 suites/unitSuite.xml + suites/e2eSuite.xml criados              ✅ FEITO
      ⚠️ pom.xml ainda aponta para testng-unit.xml / testng-e2e.xml   ✅ CORRIGIDO
  1.5 allure.properties criado                                        ✅ FEITO
  1.6 config-local.properties em src/test/resources (no .gitignore)   ✅ FEITO
  1.7 ConfigReader criado em com.whatsnext.authapi.config              ✅ FEITO
      ⚠️ spec previa E2EConfig — adotado ConfigReader como canônico

Fase 2 — Helpers E2E                                                 STATUS
  ⚠️ Pacote adotado: e2e/data/factory/ e e2e/data/utils/
     Spec previa: e2e/helper/
  2.1 RequestSpecFactory (unauthenticatedSpec / authenticatedSpec)     ✅ FEITO
  2.2 UserDbRecord (record de dados de teste)                          ✅ FEITO
      ⚠️ Spec previa UserFactory com strings soltas
  2.3 UserFactory (AtomicInteger counter, retorna UserDbRecord)        ✅ FEITO
      ⚠️ Hash BCrypt gerado em tempo de execução (não pré-computado)
  2.4 UserDataPreload (insert via JDBC, BCrypt em runtime)             ✅ FEITO
      ⚠️ Spec previa TestDataHelper — adotado UserDataPreload
  2.5 AuthDataPreload (login via API, retorna tokens)                  ✅ FEITO
      ⚠️ Spec previa AuthHelper — adotado AuthDataPreload
      ⚠️ Limitação mapeada: se /login quebrar, testes dependentes falham
  2.6 AuthClient (encapsula todas as chamadas HTTP, retorna Response)  ✅ FEITO
      ⚠️ Não previsto na spec original

Fase 3 — Contratos e Schemas                                         STATUS
  3.1 auth-response-schema.json (padrão OAuth2/RFC6749)               ✅ FEITO
  3.2 error-response-schema.json (padrão OPF: errors[]+meta)          ✅ FEITO
  3.3 RegisterDataProvider (invalidFields, invalidPasswords)           ✅ FEITO
      LoginDataProvider                                                ⏳ PENDENTE
      RefreshDataProvider                                              ⏳ PENDENTE

Fase 4 — Atualização da API para padrão OPF                         STATUS
  4.1 ErrorResponse → substituir por errors[{code,title,detail}]+meta  ✅ FEITO
  4.2 GlobalExceptionHandler → mapear codes OPF por exceção            ✅ FEITO
      EMAIL_ALREADY_EXISTS → 409
      VALIDATION_ERROR → 422 (Bean Validation)
      PASSWORD_TOO_WEAK → 422 (PasswordTooWeakException)
      UNAUTHORIZED → 401
  4.3 CORS local e test → allowed-origins: "*"                         ✅ FEITO

Fase 5 — Testes E2E                                                  STATUS
  5.1 RegisterE2ETest (cenários válido + 409 + providers 422)         ✅ FEITO
  5.2 LoginE2ETest                                                     ⏳ PENDENTE
  5.3 RefreshE2ETest                                                    ⏳ PENDENTE
  5.4 LogoutE2ETest                                                     ⏳ PENDENTE
  5.5 UserE2ETest                                                       ⏳ PENDENTE

Fase 6 — CI/CD                                                       STATUS
  6.1 ci.yml: jobs unit → e2e → deploy                                ⏳ PENDENTE
  6.2 README: seção Running Tests atualizada                           ⏳ PENDENTE
  6.3 Verificar mvn verify completo (23+ cenários passando)            ⏳ PENDENTE
```

---

## 12. Decisões técnicas adotadas (divergências da spec)

| Spec original | Decisão adotada | Motivo |
|---|---|---|
| `e2e/helper/` | `e2e/data/factory/` e `e2e/data/utils/` | Melhor separação de responsabilidades |
| `AuthHelper` | `AuthDataPreload` | Nomenclatura mais expressiva |
| `TestDataHelper` | `UserDataPreload` | Nomenclatura mais expressiva |
| Hash BCrypt pré-computado | Hash gerado em runtime via `BCryptPasswordEncoder` | Evita string hardcoded no repositório |
| `UserFactory` com strings soltas | `UserFactory` retorna `UserDbRecord` | Factory real, isolamento dos dados |
| `E2EConfig` | `ConfigReader` | Mais genérico, reutilizável |
| `testng-unit.xml` na raiz | `suites/unitSuite.xml` em subpasta | Organização dos recursos de teste |
| `ErrorResponse` flat | OPF `errors[{code,title,detail}]+meta` | Padronização com ecossistema OPF |
| Sem `AuthClient` | `AuthClient` encapsula todas as chamadas HTTP | Separação entre setup e assertion |

---

## 13. Limitações conhecidas

- **`AuthDataPreload` depende de `/login`**: se o endpoint de login estiver com bug, todos os testes que dependem de autenticação prévia falharão em cascata. Solução futura: `AuthTokenPreload` que gera JWT direto via `JwtService`, usado apenas quando `/login` está sob investigação.
- **Rate limiting não testado via E2E**: buckets in-memory resetam com reinício; comportamento não-determinístico.
- **BCrypt strength=12 em runtime**: `UserDataPreload` computa o hash uma vez na inicialização da classe (campo estático). Em execuções com muitos testes paralelos, isso não é problema — mas adiciona ~1s de overhead na primeira execução.
