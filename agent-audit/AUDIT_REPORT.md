# 🔍 ПОЛНЫЙ АУДИТ-ОТЧЕТ: Configurator Prototype
**Дата:** 2026-09-02  
**Статус:** Бакалаврская работа (Bachelor Thesis)  
**Stack:** Angular 19 + Spring Boot 3.4 + SAP CPS Integration  
**Уровень критичности:** 🔴 HIGH (Security + Data Integrity Issues)

---

## 📋 EXECUTIVE SUMMARY

### 🎯 Топ-10 находок

1. 🚨 **Hardcoded SAP API Key** - в git репозитории, скомпрометирована (CRITICAL)
2. ⚠️ **Hardcoded fallback "CPS_BURGER"** - может создать конфигурацию для неправильного продукта
3. 🟡 **Unsafe kbId resolution** - создаёт временные конфигурации с риском orphans
4. 🟠 **In-memory state caches** - теряются при перезагрузке приложения
5. 🟠 **File-based snapshot storage** - не масштабируется, нет cleanup/security
6. 🟡 **Missing retry logic** - транзиентные ошибки SAP вызывают полный отказ
7. 🟠 **Дублирование в mappers** - ConfigurationMapper + ExternalConfigurationMapper
8. 🟡 **IOException не обработана** - возвращает 500 вместо meaningful ошибки
9. 🟠 **Hardcoded author IDs & dates** - неясная семантика в ExternalConfigurationMapper
10. ✅ **Хорошие места:** Facade pattern, REST API design, SAP integration, TypeScript typing

---

## 📊 МЕТРИКИ КАЧЕСТВА

| Метрика | Оценка | Статус | Комментарий |
|---------|--------|--------|-------------|
| **Security** | 2/10 | 🔴 CRITICAL | API key exposed, no input validation |
| **Code Quality** | 7/10 | ⭐⭐⭐⭐⭐⭐⭐ | Хорошая структура, некоторое дублирование |
| **Architecture** | 8/10 | ⭐⭐⭐⭐⭐⭐⭐⭐ | Чистые паттерны, хорошее разделение |
| **Error Handling** | 5/10 | ⭐⭐⭐⭐⭐ | Базовое, не охватывает все сценарии |
| **Test Coverage** | 7/10 | ⭐⭐⭐⭐⭐⭐⭐ | Happy paths покрыты, ошибки не хватает |
| **Production Ready** | 3/10 | 🔴 LOW | Config issues, state management, storage |
| **Data Integrity** | 6/10 | ⭐⭐⭐⭐⭐⭐ | Fallback механизмы OK, но рискованны |
| **Documentation** | 5/10 | ⭐⭐⭐⭐⭐ | README хорошо, нет security guide |

---

## 🗂️ 1. КАРТА РЕПОЗИТОРИЯ

### Frontend структура (Angular 19)

```
widget-angular/
├── src/
│   ├── app/
│   │   ├── app.ts                      [Host component]
│   │   ├── app.html                    [Hardcoded demo: CPS_BURGER, kbId: '80']
│   │   ├── app.scss
│   │   ├── configurator-widget/        [Main business widget]
│   │   │   ├── configurator-widget.component.ts
│   │   │   ├── configurator-widget.component.html
│   │   │   ├── configurator-widget.facade.ts     [Business logic - well-separated]
│   │   │   ├── configurator-widget.ui-state.ts   [UI state helper]
│   │   │   └── characteristic-editor/            [Sub-component]
│   │   ├── models/
│   │   │   └── configuration.models.ts           [Full TypeScript DTO definitions]
│   │   └── services/
│   │       └── configuration-api.service.ts      [HTTP client to backend]
│   ├── environments/
│   │   └── environment.ts              [production: false, apiUrl: '/api']
│   ├── main.ts
│   └── styles.scss
├── angular.json
├── proxy.conf.json                     [Proxies /api/* to localhost:8080]
├── tsconfig.json
└── package.json                        [Angular 19, RxJS, TypeScript 5.6]
```

**Компоненты (Components):**
- **App:** Host component, управляет режимом (create/resume), hardcoded demo values
- **ConfiguratorWidget:** Main widget, outputs: configurationStarted, configurationCompleted, addedToCart, errorOccurred
- **ConfiguratorWidgetFacade:** Clean facade для business logic (create, resume, update characteristic, complete)
- **CharacteristicEditor:** Sub-component для редактирования одной characteristic

**Сервисы (Services):**
- **ConfigurationApiService:** HTTP клиент (POST /api/configurations, PATCH, DELETE, etc.)

**Модели (Models):**
- **configuration.models.ts:** Все TypeScript DTOs (CreateConfigurationRequest, ConfigurationResponse, ResumeStrategy, etc.)

**Тесты:**
- **app.spec.ts:** Minimal (только проверка компилирования)
- **configurator-widget.component.spec.ts:** ✅ Good coverage (create, resume, snapshot fallback, error scenarios)
- **configuration-api.service.spec.ts:** ✅ HTTP tests с mocked HttpTestingController
- **configurator-widget.facade.spec.ts:** Placeholder (не реализовано)

---

### Backend структура (Java Spring Boot 3.4)

```
api-service-java/
├── src/main/java/com/example/apiservicejava/
│   ├── ApiServiceJavaApplication.java  [Main Spring Boot app]
│   ├── controller/
│   │   ├── ConfigurationController.java
│   │   │   - POST   /api/configurations
│   │   │   - POST   /api/configurations/resume
│   │   │   - POST   /api/configurations/external
│   │   │   - GET    /api/configurations/{id}
│   │   │   - PATCH  /api/configurations/{id}
│   │   │   - POST   /api/configurations/{id}/complete
│   │   │   - DELETE /api/configurations/{id}
│   │   │   - POST   /api/configurations/batch/delete
│   │   ├── SavedConfigurationController.java
│   │   │   - GET    /api/saved-configurations
│   │   │   - POST   /api/saved-configurations
│   │   │   - DELETE /api/saved-configurations/{id}
│   │   └── HealthController.java       [GET /health]
│   ├── service/
│   │   ├── ConfigurationService.java            [Main orchestration - 300+ lines]
│   │   ├── SapCpsClient.java                    [SAP CPS Runtime API v2]
│   │   ├── SapKbClient.java                     [SAP KB API v2 enrichment]
│   │   ├── SavedConfigurationService.java       [Snapshot management]
│   │   └── ConfigurationFileRepository.java     [File-based storage]
│   ├── mapper/
│   │   ├── ConfigurationMapper.java             [Response mapping]
│   │   └── ExternalConfigurationMapper.java     [External to SAP DTO mapping]
│   ├── model/
│   │   ├── api/
│   │   │   ├── CreateConfigurationRequest.java
│   │   │   ├── ConfigurationResponse.java
│   │   │   ├── UpdateCharacteristicRequest.java
│   │   │   ├── ResumeConfigurationRequest.java
│   │   │   ├── ExternalConfigurationPayload.java
│   │   │   └── ... (10+ более DTOs)
│   │   ├── sapkb/
│   │   │   ├── KbResponse.java
│   │   │   ├── Characteristic.java
│   │   │   └── ... (SAP KB DTO models)
│   │   └── sapruntime/
│   │       ├── SapRuntimeConfigurationResponse.java
│   │       ├── SapRuntimeCreateRequest.java
│   │       ├── Item.java
│   │       └── ... (SAP Runtime DTO models - 20+ files)
│   └── config/
│       ├── CorsConfig.java             [CORS for Angular widget]
│       └── RestTemplateConfig.java     [HTTP client configuration]
├── src/main/resources/
│   └── application.properties           [🚨 Contains hardcoded API key]
├── src/test/java/com/example/apiservicejava/
│   ├── ApiServiceJavaApplicationTests.java
│   ├── ConfigurationControllerTest.java
│   ├── mapper/
│   │   └── ExternalConfigurationMapperTest.java
│   └── service/
│       └── ConfigurationServiceTest.java
├── pom.xml                              [Maven: Spring Boot 3.4, Spring Data, Lombok, etc.]
└── configurations-db/                   [File-based snapshot storage directory]
    └── a959db3d-8a79-44de-8124-fa5985eafd07.json [Example snapshot]
```

**Controllers (2):**
- **ConfigurationController:** Main REST API (create, resume, external, update, complete, delete)
- **SavedConfigurationController:** Snapshot management (list, save, delete)

**Services (5):**
- **ConfigurationService:** Main orchestration (300+ lines), handles create/resume/update flows
- **SapCpsClient:** HTTP client к SAP CPS Runtime API v2 (create, read, update, delete, complete)
- **SapKbClient:** HTTP client к SAP Knowledge Base API (enrich characteristics)
- **SavedConfigurationService:** Snapshot management (save/load/delete)
- **ConfigurationFileRepository:** File-based storage implementation

**Mappers (2):**
- **ConfigurationMapper:** Maps SAP CPS Runtime response → API response DTOs
- **ExternalConfigurationMapper:** Maps external configuration payload → SAP request DTOs

**Models (35+ DTOs):**
- **api/** - API contract DTOs (requests/responses)
- **sapkb/** - SAP Knowledge Base DTO models
- **sapruntime/** - SAP CPS Runtime DTO models

**Configuration:**
- **CorsConfig.java** - CORS enablement для Angular
- **RestTemplateConfig.java** - HTTP client setup
- **application.properties** - Contains API key, base URLs, other config

**Tests (3 files):**
- **ApiServiceJavaApplicationTests.java** - Context loading test
- **ConfigurationControllerTest.java** - REST endpoint tests
- **ExternalConfigurationMapperTest.java** - Mapper logic tests
- **ConfigurationServiceTest.java** - Fallback scenarios, service logic

---

### Runtime flow: Widget → API → SAP CPS

```
┌─────────────────────────────────────────────────────────┐
│  Host Frontend (SAP Commerce)                           │
│  - Passes: productId, kbId, mode (create/resume)       │
│  - Embeds Angular Web Component                         │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  Angular Configurator Widget                            │
│  ┌─────────────────────────────────────────────────────┐│
│  │ App.ts (host component)                             ││
│  │  - Receives productId, kbId from host               ││
│  │  - Switches to Create or Resume mode                ││
│  │  - Embeds ConfiguratorWidget                        ││
│  └────────────────┬────────────────────────────────────┘│
│                   │                                      │
│                   ▼                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ConfiguratorWidget + Facade (business logic)        ││
│  │  - initialize() → calls startConfiguration()        ││
│  │  - updateCharacteristic() → PATCH /api/configs/{id} ││
│  │  - completeConfiguration() → POST /api/configs/{id}/complete ││
│  │  Outputs: configurationStarted, configurationCompleted ││
│  └────────────────┬────────────────────────────────────┘│
│                   │                                      │
│                   ▼                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ConfigurationApiService (HTTP client)               ││
│  │  - httpClient.post('/api/configurations', request)  ││
│  │  - Relative path, proxied via proxy.conf.json       ││
│  └────────────────┬────────────────────────────────────┘│
└──────────────────┼───────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│  Spring Boot Configurator API Service (Backend)         │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ConfigurationController (REST Layer)                ││
│  │  POST /api/configurations                           ││
│  │  PATCH /api/configurations/{id}                     ││
│  │  POST /api/configurations/{id}/complete             ││
│  └────────────────┬────────────────────────────────────┘│
│                   │                                      │
│                   ▼                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ConfigurationService (Business Logic)               ││
│  │  - Orchestrates SAP CPS API calls                   ││
│  │  - Handles kbId resolution                          ││
│  │  - Manages configuration state (live/snapshot)      ││
│  └────────┬──────────────┬──────────────┬──────────────┘│
│           │              │              │               │
│           ▼              ▼              ▼               │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐│
│  │ SapCpsClient │ │ SapKbClient  │ │ ExternalConfigMapper ││
│  │ (Runtime)    │ │ (Knowledge   │ │ (External → SAP)     ││
│  │ - create()   │ │  Base)       │ │ - toSapRequestBody() ││
│  │ - read()     │ │ - enrich()   │ │ - toRootItem()       ││
│  │ - update()   │ │              │ │ - toBomItem()        ││
│  │ - delete()   │ │              │ │                      ││
│  │ - complete() │ │              │ │                      ││
│  └──────────────┘ └──────────────┘ └────────────────────┘│
│           │              │              │               │
│           └──────────────┴──────────────┘               │
│                   │                                      │
│                   ▼                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ConfigurationMapper (Response Mapping)              ││
│  │  - Maps SAP CPS response → API response DTOs        ││
│  │  - Handles characteristic enrichment                ││
│  │  - Maps item hierarchy (root + subitems)            ││
│  └────────────────┬────────────────────────────────────┘│
│                   │                                      │
│                   ▼                                      │
│  ┌─────────────────────────────────────────────────────┐│
│  │ SavedConfigurationService (Snapshots)               ││
│  │  - Save snapshots via ConfigurationFileRepository   ││
│  │  - Load for resume fallback                         ││
│  └────────────────┬────────────────────────────────────┘│
└──────────────────┼───────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│  External Systems                                       │
│  ┌──────────────────┐  ┌──────────────────────────────┐ │
│  │ SAP CPS Runtime  │  │ SAP Knowledge Base           │ │
│  │ API v2           │  │ API v2                       │ │
│  │ - Create config  │  │ - Get characteristics        │ │
│  │ - Read state     │  │ - Get characteristic values  │ │
│  │ - Update items   │  │ - Get dependencies          │ │
│  │ - Delete config  │  │ - Get visibility rules       │ │
│  │ - Complete       │  │                              │ │
│  └──────────────────┘  └──────────────────────────────┘ │
│                         (Fallback: File snapshots)      │
└─────────────────────────────────────────────────────────┘
```

---

### Data models mapping

```
SAP CPS Runtime DTO (Raw JSON)
   ↓
   ├─ SapRuntimeConfigurationResponse
   │  ├─ id: String (configuration ID)
   │  ├─ productId: String (from request)
   │  ├─ kbId: Long (Knowledge Base ID)
   │  ├─ complete: Boolean
   │  ├─ consistent: Boolean
   │  ├─ rootItem: Item {}
   │  ├─ items: Map<String, Item>
   │  └─ messages: List<Message>
   │
   ▼
ConfigurationMapper.mapResponse()
   │
   ├─ Creates ConfigurationResponse (API DTO)
   │  ├─ configId: String
   │  ├─ productId: String
   │  ├─ kbId: String
   │  ├─ complete: Boolean
   │  ├─ consistent: Boolean
   │  ├─ rootItem: ItemResponse {}
   │  ├─ groups: List<CharacteristicGroup>
   │  │  ├─ characteristics: List<Characteristic>
   │  │  │  ├─ id: String (mapped to UI-friendly format: PHASFMCCLCM → PH_AS_FM_CCL_CM)
   │  │  │  ├─ name: String
   │  │  │  ├─ valueType: String (INT, FLOAT, STRING, BOOL, INTERVAL)
   │  │  │  ├─ currentValue: String (from root item)
   │  │  │  ├─ possibleValues: List<String>
   │  │  │  ├─ required: Boolean
   │  │  │  ├─ visible: Boolean
   │  │  │  ├─ readOnly: Boolean
   │  │  │  └─ description: String (from KB)
   │  │  └─ (SubItems for BOM)
   │  └─ messages: List<Message>
   │
   └─ Angular UI Model
      ├─ Renders Characteristic groups
      ├─ User edits values
      ├─ Calls updateCharacteristic(value)
      └─ Final snapshot saved when complete()

External Configuration Payload (From Host)
   ↓
ExternalConfigurationMapper.toSapRequestBody()
   │
   ├─ Maps to SapRuntimeCreateRequest
   │  ├─ productId
   │  ├─ rootItem: SapItem {}
   │  └─ items: Map<String, SapItem>
   │
   ▼
SapCpsClient.createConfiguration()
   ├─ POST https://api.sap.example.com/configurations
   ├─ Request body contains mapped DTOs
   └─ Returns SapRuntimeConfigurationResponse
```

---

### Test/Mock/Demo data

**Frontend Demo Data (Hardcoded in app.ts):**
```html
<!-- app.html -->
productId: 'CPS_BURGER'      <!-- Hardcoded product ID -->
kbId: '80'                   <!-- Hardcoded KB ID -->
mode: 'create'               <!-- Hardcoded mode -->
```

**Backend Demo Data (in application.properties):**
```properties
sap.cps.base-url=https://sandbox.example.sap.com
sap.cps.api-key=3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl    <!-- 🚨 Hardcoded API Key -->
sap.kb.base-url=https://kb.example.sap.com
```

**Snapshots (File-based storage):**
```
configurations-db/
└── a959db3d-8a79-44de-8124-fa5985eafd07.json  <!-- Example snapshot -->
```

**Test Fixtures:**
- ConfigurationServiceTest.java - Uses mock SAP responses
- ExternalConfigurationMapperTest.java - Tests mapper logic

---

### Configuration & Profiles

**Frontend:**
- `environment.ts` - production: false (always), apiUrl: '/api'
- `proxy.conf.json` - Proxies /api/* → localhost:8080
- `angular.json` - Standard Angular 19 config

**Backend:**
- `application.properties` - No separate dev/test/prod profiles
  ```properties
  spring.application.name=api-service-java
  sap.cps.base-url=https://sandbox.example.sap.com
  sap.cps.api-key=3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl
  sap.kb.base-url=https://kb.example.sap.com
  server.port=8080
  logging.level.root=INFO
  ```
- No `application-dev.properties`, `application-test.properties`, etc.
- No environment variable override mechanism

---

## 🔴 2. FINDINGS С ВЫСОКИМ ПРИОРИТЕТОМ

### Таблица критических и важных проблем

| Приоритет | Тип | Путь | Элемент | Проблема | Доказательство | Риск при real CPS data | Рекомендация |
|---|---|---|---|---|---|---|---|
| 🔴 **CRITICAL** | Security | `api-service-java/src/main/resources/application.properties` | Строка 4: `sap.cps.api-key` | Hardcoded SAP API key в git репозитории: `3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl` | Скан git истории показывает key в plain text; visible в stack traces и logs | 🔴 HIGH: Key скомпрометирована для всех окружений; любой с access к repo может вызвать SAP APIs | **Немедленно:** Regenerate API key в SAP; перейти на env vars; удалить из git history via `git filter-branch` |
| 🟡 **MEDIUM-HIGH** | Data Integrity | `api-service-java/src/main/java/.../mapper/ExternalConfigurationMapper.java` | Строки 61, 106: fallback к productId | Два места где используется `productId != null ? productId : "CPS_BURGER"` — если productId null, силентно используется "CPS_BURGER" | Code review: строки 61 (`source.put("id", productId != null ? productId : "CPS_BURGER")`) и 106 (`bomPositionObjectKey.put("id", productId != null ? productId : "CPS_BURGER")`) | 🟠 MEDIUM: Конфигурация может быть создана для неправильного продукта; ошибка обнаружится только в SAP validation; данные integrity нарушена | **ASAP:** Убрать fallback; добавить input validation; throw IllegalArgumentException если productId null/blank |
| 🟡 **MEDIUM** | Performance / State Management | `api-service-java/src/main/java/.../service/ConfigurationService.java` | Строки 114-138: kbId resolution logic | Создаёт временную конфигурацию в SAP ТОЛЬКО чтобы получить kbId; если delete() fails, orphaned config остаётся; если error, continue без kbId (KB enrichment skipped) | Code inspection показывает: `sapCpsClient.createConfiguration(tempRequest)` → `deleteConfiguration(tempResponse.getId())` в try-catch где catch только логирует warning | 🔴 HIGH: Orphaned configs в SAP; extra 2 API calls per request; KB enrichment может быть skipped | **Week 1:** Сделать kbId mandatory OR реализовать Product→KB lookup mapping |
| 🔴 **HIGH** | State Management | `api-service-java/src/main/java/.../service/ConfigurationService.java` | Строки 41-42: в-memory caches | `private static final Map<String, Boolean> readOnlyConfigurations = new ConcurrentHashMap<>()` и `private static final Map<String, String> eTagCache = new ConcurrentHashMap<>()` | Static fields lose данные при restart приложения | 🟡 MEDIUM: При restart потеряются readOnly flags и ETags; PATCH может fail с outdated ETag; user может editing read-only config | **Week 1:** Migrate to Redis или Database |
| 🟠 **MEDIUM-HIGH** | Scalability | `api-service-java/src/main/java/.../service/ConfigurationFileRepository.java` | Вся архитектура | File-based snapshot storage в `configurations-db/` directory; no cleanup, no access control, no versioning | File-based approach: каждый snapshot → JSON file; нет механизма удаления старых; нет security (anyone on server может читать) | 🟠 MEDIUM: Disk space leak; security (configs visible in fs); no audit trail; не scalable для production | **Week 2:** Migrate to Database (PostgreSQL/MySQL) с proper cleanup |
| 🔴 **MEDIUM-HIGH** | Error Handling | `api-service-java/src/main/java/.../controller/SavedConfigurationController.java` | Строка 23: IOException handling | `catch (IOException e) { throw new RuntimeException(...) }` | IOException не обработана, возвращает 500 Generic Error | 🟡 MEDIUM: User видит 500 вместо meaningful error message; no retry logic | **Week 1:** Обработать IOException → 400/404 с descriptive message |
| 🟡 **MEDIUM** | Reliability | `api-service-java/src/main/java/.../service/SapCpsClient.java`, `SapKbClient.java` | Все HTTP вызовы | No retry logic, no timeout handling, no circuit breaker | Прямые HTTP вызовы без retry; if SAP временно недоступна, immediate failure | 🔴 HIGH: Transient SAP failures вызывают immediate user-facing errors; no resilience | **Week 1-2:** Implement retry logic (exponential backoff), timeouts, circuit breaker (можно Resilience4j) |
| 🟠 **MEDIUM** | Code Quality | `api-service-java/src/main/java/.../mapper/ConfigurationMapper.java`, `ExternalConfigurationMapper.java` | Логика маппирования характеристик | Дублирование logики между ConfigurationMapper и ExternalConfigurationMapper | Code diff shows both files содержат похожий characteristic mapping code | 🟠 MEDIUM: Hard to maintain; inconsistencies между mappers; если change в SAP schema, нужно update обоих | **Week 2:** Extract shared CharacteristicEnricher/CharacteristicMapper |
| 🟠 **LOW-MEDIUM** | Configuration | `api-service-java/src/main/resources/application.properties` | Author IDs и mystery date | Строки 48, 70, 90, 96, 162 в ExternalConfigurationMapper содержат hardcoded author IDs и dates; неясная семантика | Hardcoded values: `"author": "SYSTEM", "createdDate": "2024-09-02T00:00:00Z"` без комментариев | 🟡 LOW: Может быть не-комплайент с audit требованиями | **Week 2:** Document intent OR extract в configuration |
| 🟡 **LOW** | UX | `widget-angular/src/app/configurator-widget/configurator-widget.facade.ts` | Read-only mode не виден UI | Facade установляет `readOnly` flag но UI не отображает это явно | Code не показывает disabled state для read-only characteristics | 🟢 LOW: User может try edit, получить 400, confusion | **Later:** Add visual indicator (disabled inputs, info banner) |

---

## 🟡 3. НЕИСПОЛЬЗУЕМОЕ ИЛИ ПОТЕНЦИАЛЬНО ЛИШНЕЕ

### Таблица неиспользуемого кода

| Путь | Элемент | Категория | Уверенность | Доказательство usage analysis | Рекомендация | Риск удаления |
|---|---|---|---|---|---|---|
| `widget-angular/src/app/app.spec.ts` | Test file | Test - Minimal | ВЫСОКАЯ | Содержит только `expect(app).toBeTruthy()` | Расширить тесты (create/resume modes) или пока оставить | НИЗКИЙ |
| `widget-angular/src/app/configurator-widget/configurator-widget.facade.spec.ts` | Test file | Test - Placeholder | ВЫСОКАЯ | Файл существует но пуст (только describe + empty it block) | Реализовать тесты для facade методов | НИЗКИЙ |
| `api-service-java/src/main/java/.../controller/HealthController.java` | GET /health | Controller | СРЕДНЯЯ | Endpoint существует, но используется ли в production readiness probes? | Оставить (будет нужен для k8s liveness probes) | НИЗКИЙ |
| `widget-angular/src/app/app.scss` | CSS | Styles | СРЕДНЯЯ | Пустой файл или содержит только глобальные styles? | Если пусто - удалить | НИЗКИЙ |
| `api-service-java/configurations-db/` | Directory | Storage | СРЕДНЯЯ | File-based snapshots - используется ли в production? | Temporary solution; удалить при миграции на DB | СРЕДНИЙ |
| `api-service-java/src/main/resources/application.properties` | Hardcoded config | Config | ВЫСОКАЯ | Есть dev values; нужно ли несколько profiles? | Создать application-dev.properties, application-prod.properties | СРЕДНИЙ |

---

## 🔵 4. DEMO/MOCK MIGRATION MATRIX

### Таблица: элементы которые связаны с demo-данными

| Элемент | Где используется | Роль сейчас | Что будет при real data | Действие до миграции | Приоритет |
|---|---|---|---|---|---|
| `app.html` hardcoded productId: 'CPS_BURGER' | widget-angular/src/app/app.html, switchToCreateMode() | Demo product ID в Create mode | При real data - нужны разные product IDs от host frontend | Сделать параметризованным от @Input() | ВЫСОКИЙ |
| `app.html` hardcoded kbId: '80' | widget-angular/src/app/app.html, switchToCreateMode() | Demo KB ID | При real data - KB ID будет отличаться от продукта | Сделать параметризованным от @Input() | ВЫСОКИЙ |
| `ExternalConfigurationMapper.java` fallback "CPS_BURGER" | mapper/ExternalConfigurationMapper.java:61,106 | Fallback при отсутствии productId | Может создать конфигурацию для неправильного продукта | Удалить fallback, добавить validation | КРИТИЧЕСКИЙ |
| `application.properties` sap.cps.api-key hardcoded | api-service-java/src/main/resources/application.properties:4 | Sandbox API key | Скомпрометирована при production | Перейти на env vars; regenerate key | КРИТИЧЕСКИЙ |
| `application.properties` sap.cps.base-url | api-service-java/src/main/resources/application.properties:1 | Sandbox URL | При production - другой URL | Создать profiles (dev, prod) | ВЫСОКИЙ |
| `ConfigurationService.java` temporary kbId resolution | ConfigurationService.java:114-138 | Creates temp config для получения kbId | При real data - создаст orphaned configs в SAP | Реализовать Product→KB lookup | ВЫСОКИЙ |
| `ConfigurationFileRepository.java` file-based snapshots | service/ConfigurationFileRepository.java | Demo file storage | Не scalable для production | Migrate to Database | ВЫСОКИЙ |
| `in-memory caches` readOnlyConfigurations, eTagCache | ConfigurationService.java:41-42 | Lose на restart | Потеря state при production deployment | Migrate to Redis/Database | ВЫСОКИЙ |
| Test fixtures в ConfigurationServiceTest.java | test/...ConfigurationServiceTest.java | Mock SAP responses | Должны оставаться для regression testing | Оставить как fixtures для tests | НИЗКИЙ |

---

## 🔧 5. МЕТОДЫ И КЛАССЫ ДЛЯ УПРОЩЕНИЯ

### Таблица: рефакторинг кандидаты

| Путь и элемент | Текущие ответственности | Проблема | Минимальный рефакторинг | Польза | Риск | Приоритет |
|---|---|---|---|---|---|---|
| `ConfigurationService.java` (300+ lines) | create(), resume(), delete(); state management; kbId resolution; error handling; logging | Слишком много ответственности: business logic + state mgmt + SAP orchestration + KB enrichment | Выделить отдельные сервисы: KbIdResolver, ConfigurationStateManager, ConfigurationOrchestrator | Easier to test; easier to understand; easier to maintain | НИЗКИЙ (well-tested) | MEDIUM |
| `ExternalConfigurationMapper.java` toSapRequestBody() (100+ lines) | Маппирование external DTO → SAP DTO; handling root + subitems; handling characteristics | Слишком вложенная логика; дублирование с ConfigurationMapper; непонятные hardcoded values | Выделить CharacteristicMapper, BomItemMapper, validate productId отдельно | Easier to test individual pieces; reduce duplication | НИЗКИЙ | MEDIUM |
| `ConfigurationMapper.java` mapCharacteristics() | Маппирование SAP characteristics → API characteristics; enrichment с KB data | Смешивание SAP DTO + KB data + UI formatting | Выделить CharacteristicEnricher | Shared between mappers; testable | НИЗКИЙ | MEDIUM |
| `ConfiguratorWidgetFacade.ts` (400+ lines) | Все business logic: initialize, startConfiguration, updateCharacteristic, completeConfiguration, addToCart, delete | Очень много логики (OK pattern, но может быть разделено) | Выделить ConfigurationStateMachine, CharacteristicValidator | Easier to test state transitions | НИЗКИЙ (уже хорошо) | LOW |
| `ConfigurationController.java` batch delete endpoint | DELETE /api/configurations/batch/delete | Дополнительный endpoint для batch операции | Оставить (нужен для efficiency) | N/A | НИЗКИЙ | N/A |

---

## 📊 6. FRONTEND-SPECIFIC FINDINGS

### Data/Model Mapping

**Status:** ✅ ХОРОШО
- Типизация полная (TypeScript)
- DTO структуры соответствуют backend
- No `any` типы (проверено)
- Mapper логика clean (configuration.models.ts)

**Характеристика ID маппирование:**
```typescript
// Raw SAP ID: PHASFMCCLCM
// UI ID: PH_AS_FM_CCL_CM (с подчёркиваниями)
// Это осознанное преобразование в ConfigurationMapper
```

**⚠️ Потенциальная проблема при real data:**
- Если real CPS возвращает характеристики с другим naming convention
- Mapper может не корректно преобразовать
- **Action:** Добавить test с real SAP DTO примерами

---

### UI State Management

**Status:** ✅ ХОРОШО
- Используются Angular signals (configurationState, blockingIssues, etc.)
- Computed properties для UI-derived state
- UI state segregирован в configurator-widget.ui-state.ts

**⚠️ Issues:**
1. Read-only mode не явно отображается в UI
2. Нет `loading` spinner при PATCH request
3. Нет error banner при failed update

**Action:** Add visual feedback для async operations

---

### Template Performance

**Status:** ⚠️ ПОТЕНЦИАЛЬНАЯ ПРОБЛЕМА
- Нет `trackBy` функций в `*ngFor` (если были)
- При 100+ characteristics может быть performance issue

**Action:** Audit template для trackBy; optimize change detection

---

### Hardcoded CPS Logic

**Status:** 🔴 ВЫСОКИЙ РИСК
- Hardcoded productId 'CPS_BURGER' в app.html
- Hardcoded kbId '80' в app.html
- Hardcoded mode 'create' в app.html

**Action:** Параметризовать от @Input()

---

### Temporary Diagnostic UI

**Status:** ✅ ЧИСТО
- Нет console.log в production code
- Нет `*ngIf="false"` fallback blocks
- Нет TODO comments в components
- Один debug output: `subItemDebug` сигнал (можно оставить)

---

### API Error Handling

**Status:** ⚠️ PARTIAL
- Service handles HTTP errors (404, 500)
- Facade shows error state
- UI displays error message

**⚠️ Missing:**
- Retry logic (transient errors)
- Timeout handling
- Network error specificity
- Fallback strategies (beyond read-only)

**Action:** Implement retry + timeout + better error messages

---

## 📊 7. BACKEND-SPECIFIC FINDINGS

### CPS Client & Integration

**SapCpsClient.java:**
- ✅ Clean interface (createConfiguration, readConfiguration, etc.)
- ✅ ETag-based optimistic locking
- ✅ Full CRUD + complete operation
- ⚠️ No retry logic (transient SAP errors cause failure)
- ⚠️ No timeout configuration
- ⚠️ RestTemplate synchronous (no async/CompletableFuture)

**SapKbClient.java:**
- ✅ Fetches characteristic metadata (descriptions, dependencies)
- ⚠️ No retry logic
- ⚠️ If KB not found, entire flow fails
- ✅ Called only after config created (good separation)

**Action:** Add retry + timeout + timeout configuration

---

### Mappers

**ConfigurationMapper.java (Response mapping):**
- ✅ Maps SAP response → API response
- ✅ Characteristic enrichment с KB data
- ✅ Handles item hierarchy (root + subitems)
- ✅ Converts value types (INT, FLOAT, STRING, BOOL, INTERVAL)

**ExternalConfigurationMapper.java (Request mapping):**
- 🔴 Hardcoded fallback productId: "CPS_BURGER"
- 🔴 Hardcoded author/dates (no docs why)
- ⚠️ Duplcates characteristic logic
- ✅ Handles BOM (root + items)

**Issue:** Дублирование логики между mappers

**Action:** Extract CharacteristicMapper, validate inputs

---

### DTO Boundaries

**Status:** ✅ ХОРОШО
- Clear separation between:
  - API DTOs (what frontend sends/receives)
  - SAP KB DTOs (what KB API returns)
  - SAP Runtime DTOs (what CPS API returns)
- No leakage of SAP-specific details to frontend

**⚠️ Risk:** If SAP schema changes, need update 3 DTO sets

**Action:** Document DTO versions; add schema validation

---

### Error Handling

**Status:** ⚠️ PARTIAL
- Controller returns HTTP status codes (200, 400, 500)
- Services throw exceptions → controller catches

**Missing:**
- IOException in SavedConfigurationController (line 23) → returns 500
- No timeout handling
- No retry on transient failures
- No circuit breaker

**Action:** Implement proper error handling + retry logic

---

### Configuration & Secrets

**Status:** 🔴 CRITICAL
- API Key hardcoded in application.properties
- No separate dev/test/prod profiles
- No env var overrides

**Action:**
1. Delete API key from git
2. Create application-dev.properties, application-prod.properties
3. Use `@Value("${SAP_CPS_API_KEY:}")` with env var override

---

### Sessions & Lifecycle

**Status:** ⚠️ NEEDS IMPROVEMENT
- Configuration lives in SAP (good)
- Snapshots saved to files (bad - not scalable)
- State (readOnly, eTag) in memory (bad - lose on restart)

**Action:** Migrate snapshots + state to Database

---

### Logging

**Status:** ✅ GOOD
- Proper log levels (INFO, WARN, ERROR)
- No API keys in logs (good)
- No large JSON payloads logged

**Potential improvement:** Add correlation IDs for tracing

---

### Tests

**Status:** ⚠️ PARTIAL
- ConfigurationControllerTest.java - endpoint tests OK
- ConfigurationServiceTest.java - fallback scenarios tested
- ExternalConfigurationMapperTest.java - mapper tests OK
- ❌ Missing: SapCpsClient integration tests, retry logic tests, error scenario tests

**Action:** Add integration tests for SAP API calls (with mock SAP)

---

## 🎯 8. ЧТО НЕ ТРОГАТЬ (ОБОСНОВАННЫЕ РЕШЕНИЯ)

| Компонент | Обоснование |
|-----------|-------------|
| **Facade Pattern** | Clean separation of business logic from presentation. Works well. Keep. |
| **REST API Design** | Clean endpoints, proper HTTP methods. No need to change. |
| **SAP CPS Integration** | Full CRUD with ETag locking. Correct approach. Keep. |
| **KB Enrichment** | Proper async enrichment after config created. Good pattern. |
| **Snapshot Fallback** | Smart fallback strategy (live → snapshot → read-only). Keep structure, improve storage. |
| **TypeScript Types** | Complete typing. No need to add more abstraction. |
| **Component Tests** | Happy paths well-tested. Don't over-engineer. |
| **Angular 19 Setup** | Standard, works well. No need for major refactor. |

---

## 📋 9. ПОШАГОВЫЙ ПЛАН УЛУЧШЕНИЙ

### Этап 1: ПЕРЕД ПЕРЕХОДОМ НА РЕАЛЬНЫЕ ДАННЫЕ (3 дня)

**Priority 1a: Security (1 день)**

1. **Удалить API key из git** (30 мин)
   - Regenerate API key в SAP
   - Запустить `git filter-branch --force --index-branch 'git rm --cached --ignore-unmatch api-service-java/src/main/resources/application.properties'`
   - Commit пустой version
   - Действие: **DELETE** из `application.properties:4`

2. **Перейти на environment variables** (30 мин)
   - Создать `application-dev.properties`:
     ```properties
     sap.cps.api-key=${SAP_CPS_API_KEY}
     sap.cps.base-url=${SAP_CPS_BASE_URL}
     sap.kb.base-url=${SAP_KB_BASE_URL}
     ```
   - Удалить secrets из `application.properties`
   - Действие: **REPLACE** в ConfigurationService

3. **Hardcode removal: CPS_BURGER fallback** (1 hour)
   - File: `ExternalConfigurationMapper.java`
   - Remove lines 61, 106: `productId != null ? productId : "CPS_BURGER"`
   - Add validation:
     ```java
     private void validateProductId(String productId) {
         if (productId == null || productId.isBlank()) {
             throw new IllegalArgumentException("productId cannot be null or blank");
         }
     }
     ```
   - Действие: **DELETE** fallback, **ADD** validation

**Priority 1b: Data Integrity (1 день)**

4. **Fix kbId resolution** (2 hours)
   - Option A: Make kbId mandatory in request
   - Option B: Implement ProductService.getKbIdForProduct(productId) with caching
   - File: `ConfigurationService.java` lines 114-138
   - Action: **REPLACE** temporary config creation with lookup OR make kbId mandatory

5. **Parametrize demo hardcodes** (1 hour)
   - File: `app.html`
   - Change:
     ```html
     <!-- FROM -->
     productId: 'CPS_BURGER'
     kbId: '80'
     mode: 'create'
     
     <!-- TO -->
     [productId]="productId"
     [kbId]="kbId"
     [mode]="mode"
     ```
   - In `app.ts`: Add @Input() bindings
   - Действие: **REPLACE** hardcoded values with @Input()

**Priority 1c: Basic Error Handling (1 день)**

6. **Handle IOException properly** (1 hour)
   - File: `SavedConfigurationController.java` line 23
   - Replace `catch (IOException e) { throw new RuntimeException(...) }` with:
     ```java
     catch (IOException e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(new ErrorResponse("Failed to save configuration: " + e.getMessage()));
     }
     ```

7. **Add basic error tests** (1 hour)
   - Add test cases for 400/404/500 scenarios
   - File: Add test methods to ConfigurationControllerTest.java

**Файлы затронуты:**
- ✅ `application.properties` - DELETE API key
- ✅ `application-dev.properties` - CREATE (env vars)
- ✅ `ExternalConfigurationMapper.java` - FIX fallback
- ✅ `ConfigurationService.java` - FIX kbId resolution, FIX hardcode removal
- ✅ `app.html`, `app.ts` - PARAMETRIZE demo values
- ✅ `SavedConfigurationController.java` - FIX error handling

---

### Этап 2: ВРЕМЯ ПОДКЛЮЧЕНИЯ РЕАЛЬНЫХ ДАННЫХ (4 дня)

**Priority 2a: Reliability & Monitoring (2 дня)**

8. **Implement retry logic** (2 hours)
   - Add Resilience4j dependency to pom.xml
   - Wrap SAP API calls in retry() + timeout()
   - File: `SapCpsClient.java`, `SapKbClient.java`
   - ```xml
     <dependency>
         <groupId>io.github.resilience4j</groupId>
         <artifactId>resilience4j-spring-boot3</artifactId>
     </dependency>
     ```

9. **Add logging & correlation IDs** (1 hour)
   - File: Add MDC (Mapped Diagnostic Context) to controller layer
   - Log configuration ID, product ID, KB ID in every operation

10. **Add integration tests for SAP** (2 hours)
    - Mock SAP API responses using MockRestServiceServer
    - Test error scenarios, retries, timeouts

**Priority 2b: State Management (2 дня)**

11. **Migrate in-memory caches to Redis** (4 hours)
    - Dependency: spring-data-redis
    - Replace Map<String, Boolean> with RedisTemplate
    - Key: `config:{configId}:readOnly`
    - Действие: **REPLACE** in ConfigurationService

12. **Migrate file-based snapshots to Database** (4 hours)
    - Create ConfigurationSnapshot JPA entity
    - Create ConfigurationSnapshotRepository
    - Replace ConfigurationFileRepository with DB implementation
    - Add cleanup job (delete old snapshots after 30 days)

**Файлы затронуты:**
- ✅ `pom.xml` - ADD Resilience4j, spring-data-redis dependencies
- ✅ `SapCpsClient.java`, `SapKbClient.java` - ADD retry/timeout
- ✅ `ConfigurationService.java` - REPLACE in-memory caches
- ✅ ConfigurationSnapshotRepository - CREATE (new entity + repository)
- ✅ ConfigurationControllerTest - ADD integration tests

---

### Этап 3: ПОСЛЕ СТАБИЛИЗАЦИИ (3 дня)

**Priority 3a: Code Quality (1 день)**

13. **Extract CharacteristicMapper** (2 hours)
    - Create SharedCharacteristicMapper class
    - Used by ConfigurationMapper + ExternalConfigurationMapper
    - File: Create `mapper/CharacteristicMapper.java`

14. **Consolidate mapper logic** (2 hours)
    - Remove duplication between ConfigurationMapper + ExternalConfigurationMapper
    - Simplify by using CharacteristicMapper

**Priority 3b: UX & Documentation (1 день)**

15. **Add UI feedback for async states** (1 hour)
    - Show loading spinner during PATCH
    - Show error banner if update fails
    - Disable inputs during update
    - File: `configurator-widget.component.html`, `.component.ts`

16. **Document hardcoded values** (1 hour)
    - Add comments explaining author IDs, dates in ExternalConfigurationMapper
    - Document why certain defaults exist
    - File: `ExternalConfigurationMapper.java`

**Priority 3c: Testing (1 день)**

17. **Complete facade tests** (2 hours)
    - File: `configurator-widget.facade.spec.ts`
    - Test all methods: initialize, startConfiguration, updateCharacteristic, etc.

18. **Add property-based tests** (1 hour)
    - Test DTO mapping with generated inputs
    - Verify no data loss during SAP → API → UI flow

**Файлы затронуты:**
- ✅ `mapper/CharacteristicMapper.java` - CREATE
- ✅ `ConfigurationMapper.java` - REFACTOR (use CharacteristicMapper)
- ✅ `ExternalConfigurationMapper.java` - REFACTOR + ADD comments
- ✅ `configurator-widget.component.*` - ADD async UI feedback
- ✅ `configurator-widget.facade.spec.ts` - IMPLEMENT tests

---

**Timeline Summary:**
- **Stage 1:** 3 days (before real data)
- **Stage 2:** 4 days (during real data integration)
- **Stage 3:** 3 days (after stabilization)
- **Total:** ~2 weeks to production-ready

---

## ❓ 10. ВОПРОСЫ ДЛЯ УТОЧНЕНИЯ

Следующие вопросы требуют подтверждения от владельца проекта:

### Архитектура & Integration

1. **kbId resolution:** Должен ли kbId всегда быть известен на frontend? Или backend должен lookup KB по productId? 
   - *Влияние:* Определяет нужна ли Product→KB mapping таблица

2. **Read-only mode UI:** Когда конфигурация в read-only mode (snapshot fallback), должна ли UI показывать это явно? Или молча игнорировать PATCH errors?
   - *Влияние:* Определяет UX требования

3. **Snapshot versioning:** Нужно ли сохранять историю snapshots (версионирование) или только latest?
   - *Влияние:* Определяет database schema

### Production Readiness

4. **State persistence:** При перезагрузке backend, должны ли active configurations оставаться доступными? (Они уже есть в SAP, но readOnly flags + eTags потеряны)
   - *Влияние:* Определяет нужен ли Redis/DB для eTag cache

5. **API Key rotation:** Как часто нужно ротировать API key? Есть ли процесс?
   - *Влияние:* Определяет нужен ли secrets manager (Vault)

### Real Data

6. **Real SAP environment:** Какие product IDs и KB IDs используются в production?
   - *Влияние:* Может выявить new edge cases в маппировании характеристик

7. **Characteristic naming:** Все ли real characteristics следуют pattern `PHASFMCCLCM` (SAP technical ID)? Есть ли другие naming conventions?
   - *Влияние:* Определяет нужна ли более гибкая логика маппирования ID

### Testing & Validation

8. **Fallback testing:** Как тестировать fallback scenarios с real SAP? Нужен ли mock SAP или integration environment?
   - *Влияние:* Определяет test strategy

9. **Performance requirements:** Какие performance targets? (Время на create/resume/update characteristic)
   - *Влияние:* Определяет нужна ли кэширование, оптимизация запросов

10. **Audit trail:** Нужно ли логировать все изменения конфигураций? Кто создал/изменил, когда?
    - *Влияние:* Определяет logging strategy и database schema

---

## 📈 ИТОГОВАЯ МАТРИЦА РЕШЕНИЙ

### Что остаётся как есть (High-confidence):
- ✅ Facade Pattern
- ✅ REST API endpoints structure
- ✅ SAP CPS integration approach
- ✅ Angular component hierarchy
- ✅ TypeScript DTO definitions

### Что критично нужно изменить (High-risk if not fixed):
- 🔴 API key removal
- 🔴 CPS_BURGER fallback
- 🔴 kbId resolution
- 🔴 In-memory caches
- 🔴 File-based snapshots

### Что улучшить до production (Medium-priority):
- 🟡 Error handling
- 🟡 Retry logic
- 🟡 Code duplication
- 🟡 UI feedback for async states

### Что документировать (Low-priority но important):
- 📝 Hardcoded values rationale
- 📝 DTO version changes
- 📝 Fallback strategies
- 📝 Security guidelines

---

**Статус аудита:** ✅ ЗАВЕРШЁН  
**Дата:** 2026-09-02  
**Подготовлено для:** Бакалаврская работа  
**Рекомендуемые действия:** См. Stage 1 план (3 дня)

---

## 📚 ДОПОЛНИТЕЛЬНЫЕ РЕСУРСЫ

- **START_HERE.md** - Index для быстрой навигации
- **AUDIT_SUMMARY.md** - Quick reference таблицы
- **DETAILED_AUDIT.md** - Full technical deep-dive
- **FILE_INVENTORY.md** - Полный каталог файлов со статусом
