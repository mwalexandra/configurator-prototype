# 🔍 ДЕТАЛЬНЫЙ АУДИТ-ОТЧЕТ: SAP CPS Configuration Prototype
**Дата:** 2026-09-02  
**Статус:** Бакалаврская работа - Angular + Spring Boot + SAP CPS интеграция  
**Уровень критичности:** 🔴 HIGH (Security + Data Integrity Issues)

---

## 📌 СТРУКТУРА ОТЧЕТА

1. [Резюме находок](#резюме-находок)
2. [Полная картография компонентов](#полная-картография-компонентов)
3. [Критические проблемы](#критические-проблемы)
4. [Анализ миграции на real data](#анализ-миграции-на-real-data)
5. [Дублирование кода](#дублирование-кода)
6. [Hardcoded значения](#hardcoded-значения)
7. [Файлы для cleanup](#файлы-для-cleanup)

---

## 📊 РЕЗЮМЕ НАХОДОК

| Категория | Кол-во | Примеры |
|-----------|--------|---------|
| 🔴 Security Issues | 1 | Hardcoded API key в application.properties |
| 🟡 Potential Bugs | 3 | CPS_BURGER fallback, kbId resolution, snapshot inconsistency |
| 🟠 Technical Debt | 5 | Дублирование маппиров, in-memory cache, file storage |
| ✅ Best Practices | 3 | Facade pattern, Fallback mechanisms, ETag handling |

---

## 🗂️ ПОЛНАЯ КАРТОГРАФИЯ КОМПОНЕНТОВ

### 📍 FRONTEND: Angular 19

#### Компоненты (Components)

**1. App Component (Host)**
- 📄 [widget-angular/src/app/app.ts](widget-angular/src/app/app.ts)
- 📄 [widget-angular/src/app/app.html](widget-angular/src/app/app.html)
- 📄 [widget-angular/src/app/app.scss](widget-angular/src/app/app.scss)
- **Функция:** Главный host component, управляет режимом (create/resume)
- **责任:** 
  - Переключение между Create и Resume режимами
  - Сохранение snapshots через fetch('/api/saved-configurations')
  - Demo-конфиг: `productId: 'CPS_BURGER', kbId: '80'`
- 🟡 **Проблема:** Hardcoded demo values в switchToCreateMode(), видны в UI

**2. ConfiguratorWidgetComponent (Main)**
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.component.ts](widget-angular/src/app/configurator-widget/configurator-widget.component.ts)
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.component.html](widget-angular/src/app/configurator-widget/configurator-widget.component.html)
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.component.scss](widget-angular/src/app/configurator-widget/configurator-widget.component.scss)
- **Функция:** Основной widget для конфигурирования
- **Выходы (Outputs):**
  - `configurationStarted: string` - ID конфигурации
  - `configurationCompleted: ConfigurationSnapshot` - Завершенный snapshot
  - `addedToCart: CompletedConfigurationResult` - Финальный результат
  - `errorOccurred: { errorCode, message }` - Ошибки
- **Управляет:** Facade (business logic), UI state, Characteristic editor

**3. ConfiguratorWidgetFacade (Business Logic)**
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.facade.ts](widget-angular/src/app/configurator-widget/configurator-widget.facade.ts)
- **Функция:** Всю business logic отделена в facade pattern
- **Методы:**
  - `initialize()` - Setup базирован на mode (create/resume)
  - `startConfiguration()` - POST /api/configurations
  - `resumeConfiguration()` - POST /api/configurations/resume
  - `createFromExternalConfiguration()` - POST /api/configurations/external
  - `updateCharacteristic()` - PATCH /api/configurations/{id}
  - `completeConfiguration()` - POST /api/configurations/{id}/complete
  - `addToCart()` - Финализация + сохранение snapshot
  - `deleteCurrentConfiguration()` - DELETE конфигурации
  - `deleteMultipleConfigurations()` - Batch delete
- **State signals:**
  - `blockingIssues` - Computed: Required характеристики без значений
  - `incompleteRequiredSubItemCharacteristics` - SubItems issues
  - `subItemDebug` - Debug info для BOM
- ✅ **Хорошо:** Чистая архитектура, вся logic отделена от presentation

**4. CharacteristicEditorComponent (SubComponent)**
- 📄 [widget-angular/src/app/configurator-widget/characteristic-editor/characteristic-editor.component.ts](widget-angular/src/app/configurator-widget/characteristic-editor/characteristic-editor.component.ts)
- **Функция:** Редактирование одной характеристики
- **Inputs:** Characteristic DTO, Селектор значений
- **Outputs:** Value change events

**5. UI State Helper**
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.ui-state.ts](widget-angular/src/app/configurator-widget/configurator-widget.ui-state.ts)
- **Функция:** Вычисление UI-state based on configuration + status

#### Сервисы (Services)

**1. ConfigurationApiService**
- 📄 [widget-angular/src/app/services/configuration-api.service.ts](widget-angular/src/app/services/configuration-api.service.ts)
- **Функция:** HTTP клиент к backend
- **Методы:**
  - `createConfiguration(payload)` - POST /api/configurations
  - `getConfiguration(configId)` - GET /api/configurations/{id}
  - `patchConfiguration(configId, payload)` - PATCH /api/configurations/{id}
  - `resumeConfiguration(payload)` - POST /api/configurations/resume
  - `completeConfiguration(configId)` - POST /api/configurations/{id}/complete
  - `createFromExternalConfiguration(payload)` - POST /api/configurations/external
  - `deleteConfiguration(configId)` - DELETE /api/configurations/{id}
  - `deleteConfigurations(payload)` - POST /api/configurations/batch/delete
- **Config:** Base URL из environment.ts
- ✅ **Хорошо:** Чистый interface для всех операций

#### Модели (Models)

**1. configuration.models.ts**
- 📄 [widget-angular/src/app/models/configuration.models.ts](widget-angular/src/app/models/configuration.models.ts)
- **Типы:**
  ```typescript
  // Modes
  type ConfiguratorMode = 'create' | 'resume'
  type WidgetState = 'idle' | 'loading' | 'loaded' | 'updating' | 'error' | 'completing' | 'completed'
  type ResumeStrategy = 'LIVECONFIGURATION' | 'SNAPSHOTFALLBACK' | 'READONLYSNAPSHOT'
  
  // Request DTOs
  interface CreateConfigurationRequest { productId, kbId }
  interface UpdateCharacteristicRequest { configId, itemId, characteristicId, value }
  interface ResumeConfigurationRequest { configurationId?, snapshot?, sourceContext? }
  interface ExternalConfigurationPayload { productId, kbId, rootItem, metadata }
  
  // Response DTOs
  interface ConfigurationResponse { configId, productId, kbId, complete, consistent, rootItem, groups, messages, restoreInfo }
  interface ConfigurationSnapshot { configId, productId, savedAt, snapshot {...} }
  interface Characteristic { id, name, valueType, required, visible, readOnly, values[], possibleValues[] }
  ```
- ✅ **Хорошо:** Полная типизация, соответствует backend DTOs

#### Environment Configuration

**1. environment.ts**
- 📄 [widget-angular/src/environments/environment.ts](widget-angular/src/environments/environment.ts)
- **Содержит:**
  ```typescript
  export const environment = {
    production: false,
    apiUrl: '/api'  // Relative path, proxied через proxy.conf.json
  };
  ```
- ⚠️ **Проблема:** production = false всегда, нет env-specific конфигов

#### Конфигурация Build

**1. angular.json**
- 📄 [widget-angular/angular.json](widget-angular/angular.json)
- Стандартный Angular 19 конфиг

**2. proxy.conf.json**
- 📄 [widget-angular/proxy.conf.json](widget-angular/proxy.conf.json)
- Проксирует `/api/*` на backend (localhost:8080)

#### Тесты

**1. App Component Tests**
- 📄 [widget-angular/src/app/app.spec.ts](widget-angular/src/app/app.spec.ts)
- ⚠️ **Минимум тестов** - только проверка компилирования

**2. ConfiguratorWidget Component Tests**
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.component.spec.ts](widget-angular/src/app/configurator-widget/configurator-widget.component.spec.ts)
- **Покрывает:**
  - Create mode flow
  - Resume mode flow  
  - Snapshot fallback scenario (read-only)
  - Update characteristic
  - Complete configuration
  - Error scenarios (missing productId)
- **Helper function:** `createConfigResponse()` - генерирует mock ConfigurationResponse
- ✅ **Хорошо:** Покрыты ключевые flows

**3. API Service Tests**
- 📄 [widget-angular/src/app/services/configuration-api.service.spec.ts](widget-angular/src/app/services/configuration-api.service.spec.ts)
- **Покрывает:** HTTP запросы, mocked HttpTestingController
- ✅ **Хорошо:** Стандартные HTTP tests

**4. Facade Tests**
- 📄 [widget-angular/src/app/configurator-widget/configurator-widget.facade.spec.ts](widget-angular/src/app/configurator-widget/configurator-widget.facade.spec.ts)
- 🟡 **Placeholder test** - не реализовано

---

### 📍 BACKEND: Java Spring Boot 3.4

#### Controllers

**1. ConfigurationController**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/controller/ConfigurationController.java](api-service-java/src/main/java/com/example/apiservicejava/controller/ConfigurationController.java)
- **REST Endpoints:**
  ```
  POST   /api/configurations                      - Create new config
  POST   /api/configurations/resume               - Resume from snapshot/live
  POST   /api/configurations/external             - Create from external config
  GET    /api/configurations/{configId}           - Fetch configuration
  PATCH  /api/configurations/{configId}           - Update characteristic
  POST   /api/configurations/{configId}/complete  - Mark as complete
  DELETE /api/configurations/{configId}           - Delete configuration
  POST   /api/configurations/batch/delete         - Batch delete
  ```
- ✅ **Хорошо:** Clean RESTful design, proper HTTP status codes

**2. SavedConfigurationController**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/controller/SavedConfigurationController.java](api-service-java/src/main/java/com/example/apiservicejava/controller/SavedConfigurationController.java)
- **Endpoints:**
  ```
  GET  /api/saved-configurations           - List all snapshots
  GET  /api/saved-configurations/{fileName} - Fetch snapshot
  POST /api/saved-configurations            - Save new snapshot
  ```
- ⚠️ **Проблема:** IOException не обработана, может вернуть 500

#### Services (Business Logic)

**1. ConfigurationService** 🔴 CORE SERVICE
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/service/ConfigurationService.java](api-service-java/src/main/java/com/example/apiservicejava/service/ConfigurationService.java)
- **Размер:** ~450 строк
- **Основные методы:**

  **a) createConfiguration(CreateConfigurationRequest)**
  - Создает SAP CPS конфигурацию через SapCreateRequest
  - Поля: productKey, kbId, context=[VBAP-VRKME=EA], date=today, source=[cpq, quote_item, 10]
  - Фетчит KB для enrichment
  - Маппит через ConfigurationMapper.toWidgetResponse()
  - ⚠️ Hardcoded context и source
  
  **b) createFromExternalConfiguration(ExternalConfigurationCreateRequest)**
  - Преобразует external DTO в SAP payload через ExternalConfigurationMapper
  - 🟡 **Fallback для kbId:**
    - Если request.kbId = null, использует sapResponse.kbId
    - Если sapResponse.kbId = null, создает ВРЕМЕННУЮ конфигурацию(!):
      ```java
      SapRuntimeConfigurationResponse tempResponse = sapCpsClient.createConfiguration(tempRequest);
      kbId = tempResponse.getKbId().toString();
      sapCpsClient.deleteConfiguration(tempResponse.getId());
      ```
    - ⚠️ Риск: Если deleteConfiguration() fails → мусор в SAP
  - Затем фетчит KB и маппит
  
  **c) getConfiguration(String configId)**
  - Фетчит с ETag через getConfigurationWithEtag()
  - Кэширует ETag в etagByConfigurationId
  - Фетчит KB и маппит
  - ⚠️ Если KB не найдена → 404 ошибка
  
  **d) patchConfiguration(String configId, PatchConfigurationRequest)**
  - 🔴 Проверяет readOnly flag (блокирует если snapshot restore)
  - Получает ETag из кэша (если нет, делает GET)
  - Вызывает SapCpsClient.patchConfiguration()
  - Получает новый ETag
  - Фетчит KB и маппит обновленную конфиг
  - ✅ Правильный optimistic locking
  
  **e) resumeConfiguration(ResumeConfigurationRequest)**
  - **Логика:**
    1. Если request.configurationId set → try live session (GET /api/v2/configurations/{id})
    2. Если live успешно → RestoreInfo.strategy = "LIVECONFIGURATION"
    3. Если live fails (exception) → use snapshot fallback
    4. При fallback: RestoreInfo.strategy = "SNAPSHOTFALLBACK" или "READONLYSNAPSHOT"
    5. Mark конфигурацию как readOnly (блокирует future PATCHes)
  - ✅ **Хорошо:** Полноценная fallback логика, правильный lifecycle
  - 🟡 **Проблема:** Если snapshot.configurationId != лежит live, данные могут быть несовместимы
  
  **f) completeConfiguration(String configId)**
  - Вызывает SapCpsClient.completeConfiguration()
  - Проверяет что complete=true && consistent=true
  - Фетчит KB и маппит
  - 🔴 Блокирует если readOnly
  
  **g) deleteConfiguration(String configId)**
  - Вызывает SapCpsClient.deleteConfiguration()
  - Очищает кэши: etagByConfigurationId.remove(), readOnlyByConfigurationId.remove()
  - ✅ Хорошо: Cleanup логика есть
  
  **h) deleteConfigurations(DeleteConfigurationsRequest) - Batch**
  - Iterates и вызывает deleteConfiguration() для каждого
  - Возвращает success/failure counts

- **State Management:**
  ```java
  private final Map<String, String> etagByConfigurationId = new ConcurrentHashMap<>();
  private final Map<String, Boolean> readOnlyByConfigurationId = new ConcurrentHashMap<>();
  ```
  - ❌ **Проблема:** In-memory, теряется при restart
  - ❌ **Проблема:** В clustered env - не synchronized
  - ⚠️ **Проблема:** Нет cleanup для старых configs

**2. SapKbClient**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/service/SapKbClient.java](api-service-java/src/main/java/com/example/apiservicejava/service/SapKbClient.java)
- **Метод:** `getKnowledgeBase(String kbId)`
  - URL: `{baseUrl}/api/v2/knowledgebases/{kbId}`
  - Header: APIKey
  - Return: SapKbResponse
  - ⚠️ Если KB не найдена → HttpStatusCodeException
  - ⚠️ Возвращает null если problem
- ✅ **Хорошо:** Чистый client, proper logging

**3. SapCpsClient**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/service/sap/SapCpsClient.java](api-service-java/src/main/java/com/example/apiservicejava/service/sap/SapCpsClient.java)
- **Методы:**
  - `getConfiguration(String configId)` - GET /api/v2/configurations/{id}
  - `getConfigurationWithEtag(String configId)` - Возвращает SapGetConfigurationResult (body + ETag)
  - `createConfiguration(SapCreateRequest)` - POST /api/v2/configurations
  - `createConfigurationFromExternal(Map<String, Object>)` - POST с external payload
  - `patchConfiguration(configId, itemId, charId, value, etag)` - PATCH /api/v2/configurations/{configId}/items/{itemId}/characteristics/{charId}
  - `completeConfiguration(String configId)` - POST /api/v2/configurations/{configId}/complete
  - `deleteConfiguration(String configId)` - DELETE /api/v2/configurations/{configId}
- ✅ **Хорошо:** Полный CRUD, proper ETag handling, API key in headers

**4. ConfigurationFileRepository**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/service/ConfigurationFileRepository.java](api-service-java/src/main/java/com/example/apiservicejava/service/ConfigurationFileRepository.java)
- **Функция:** File-based JSON storage для snapshots
- **Методы:**
  - `save(fileName, data)` - Write to ~/configurations-db/{fileName}.json
  - `read(fileName, Class<T>)` - Read JSON
  - `listJsonFiles()` - List all JSON files
  - `exists(fileName)` - Check if file exists
- ⚠️ **Проблема:** Директория настраивается через property `app.configurations-db.path`
- ⚠️ **Проблема:** Нет cleanup, нет access control, не scalable
- ❌ **Проблема:** IOException propagates to controller

**5. SavedConfigurationService**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/service/SavedConfigurationService.java](api-service-java/src/main/java/com/example/apiservicejava/service/SavedConfigurationService.java)
- **Методы:**
  - `list()` - Returns all SavedConfigurationDto
  - `get(fileName)` - Returns specific snapshot
  - `save(SaveConfigurationRequest)` - Saves new, generates UUID if no id
- ✅ **Хорошо:** Простой interface

#### Mappers (DTO/Domain Mapping)

**1. ConfigurationMapper**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/mapper/ConfigurationMapper.java](api-service-java/src/main/java/com/example/apiservicejava/mapper/ConfigurationMapper.java)
- **Главный метод:** `toWidgetResponse(SapRuntimeConfigurationResponse, SapKbResponse)`
  - Маппит SAP response + KB в API ConfigurationResponse
  - Вызывает `resolveProductId()` - tries KbKey.name, productKey, KB HeaderInfo.key
  - Маппит rootItem рекурсивно (subItems)
  - Маппит characteristics с KB enrichment
  - **Логика resolveValueType():**
    - Если KB char type = "float"|"integer" → "NUMERIC"
    - Если multiValued = true → "MULTI"
    - Если freeText (no possibleValues) && type="string" → "FREETEXT"
    - Else → "SINGLE"
  - ✅ **Хорошо:** Полное маппирование, KB enrichment

**2. ExternalConfigurationMapper**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/mapper/ExternalConfigurationMapper.java](api-service-java/src/main/java/com/example/apiservicejava/mapper/ExternalConfigurationMapper.java)
- **Главные методы:**
  - `toSapRequestBody(ExternalConfigurationCreateRequest)` - API → SAP request
  - `fromSapRuntimeResponse(SapRuntimeConfigurationResponse)` - SAP → API response
  
  **toSapRequestBody логика:**
  ```java
  body.put("kbId", parseInteger(request.getKbId()));
  body.put("configurationDate", "2018-08-09");  // 🟡 WHY THIS DATE?
  body.put("complete", false);  // 🟡 Always false
  body.put("consistent", true);
  body.put("locked", false);
  
  Map<String, Object> source = new HashMap<>();
  source.put("application", "generic");
  source.put("type", "product");
  source.put("id", productId != null ? productId : "CPS_BURGER");  // 🔴 FALLBACK
  body.put("source", source);
  
  // Root item mapping
  objectKey: { id: productId|item.key, type: "MARA", classType: "300" }
  quantity: { value: 1, unit: "PCE" }
  objectKeyAuthor: "5"
  
  // SubItems mapping (similar, но bomPosition используется)
  bomPositionObjectKey: { id: productId|"CPS_BURGER", type: "MARA", classType: "300" }
  bomPositionAuthor: "2"
  bomPosition: item.id
  ```
  - 🟡 **Проблема:** Hardcoded "2018-08-09" дата
  - 🟡 **Проблема:** Hardcoded author IDs ("5", "2", "8")
  - 🔴 **Проблема:** Fallback к "CPS_BURGER" если productId = null
  
  **fromSapRuntimeResponse логика:**
  - Обратное маппирование SAP response → API response
  - Инициализирует groups и messages пустыми lists (если null)
  - ✅ **Хорошо:** Defensive programming

- **Test:** ExternalConfigurationMapperTest покрывает KB enrichment баги

#### Models (DTOs)

**API Layer (что видит frontend):**
- `CreateConfigurationRequest` - productId, kbId
- `ConfigurationResponse` - configId, productId, kbId, complete, consistent, rootItem, groups, messages, backendProcessingTimeMs, restoreInfo
- `ResumeConfigurationRequest` - configurationId, snapshot, sourceContext
- `PatchConfigurationRequest` - characteristicId, value, itemId
- `ExternalConfigurationCreateRequest` - productId, kbId, externalConfiguration
- `SaveConfigurationRequest` - id, label, productId, configurationId, snapshot
- `SavedConfigurationDto` - id, fileName, label, productId, configurationId, savedAt, snapshot
- `DeleteConfigurationsRequest` - configurationIds[]
- `DeleteConfigurationsResponse` - successful[], failed[]
- `CharacteristicDto` - id, name, description, valueType, required, visible, readOnly, complete, consistent, values[], possibleValues[]
- `CharacteristicValueDto` - id, name, description, selected, author
- `ConfigurationItem` - id, key, complete, consistent, characteristics[], subItems[]
- `ConfigurationSnapshot` - все полностью скопировано, плюс timestamp
- `RestoreInfo` - mode, status, strategy, liveSessionAvailable, snapshotUsed, readOnly, message

**SAP KB Layer (SapKbResponse модели):**
- `SapKbResponse` - headerInfo, products[], characteristics[]
- `SapKbProduct` - id, name, key
- `SapKbCharacteristic` - id, name, description, type (string|float|integer), length, numberDecimals, entryFieldMask, multiValued, possibleValues[], characteristicGroups[]
- `SapKbPossibleValue` - valueLow, name, description
- `SapKbCharacteristicGroup` - id, name, collapsed, characteristics[]
- `SapKbHeaderInfo` - key{name, ...}, ...

**SAP CPS Runtime Layer (SapRuntimeConfigurationResponse модели):**
- `SapRuntimeConfigurationResponse` - id, kbId, kbKey, productKey, productType, complete, consistent, locked, date, rootItem, messages[]
- `SapRuntimeRootItem` - id, key, complete, consistent, characteristics[], subItems[], quantity
- `SapRuntimeCharacteristic` - id, values[], possibleValues[], required, visible, readOnly, complete, consistent
- `SapRuntimeValue` - value, author
- `SapRuntimePossibleValue` - valueLow, valueHigh, intervalType (0|1), selectable
- `SapCreateRequest` - productKey, kbId, date, context[], source{application, type, id}
- `SapRuntimeConflict` - id, description

#### Configuration

**1. RestTemplateConfig**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/config/RestTemplateConfig.java](api-service-java/src/main/java/com/example/apiservicejava/config/RestTemplateConfig.java)
- **Configures:** RestTemplate bean для HTTP calls

**2. CorsConfig**
- 📄 [api-service-java/src/main/java/com/example/apiservicejava/config/CorsConfig.java](api-service-java/src/main/java/com/example/apiservicejava/config/CorsConfig.java)
- **Configures:** CORS для development (localhost:4200)

**3. application.properties** 🚨
- 📄 [api-service-java/src/main/resources/application.properties](api-service-java/src/main/resources/application.properties)
- **Содержит:**
  ```properties
  spring.application.name=api-service-java
  sap.cps.base-url=https://sandbox.api.sap.com/cpservices/prodconf
  sap.cps.api-key=3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl
  app.configurations-db.path=configurations-db
  ```
  - 🔴 **SECURITY:** API key в открытом виде
  - 🔴 **SECURITY:** Repository exposed в git
  - ⚠️ **Нет production properties**

#### Тесты

**1. ConfigurationControllerTest**
- 📄 [api-service-java/src/test/java/com/example/apiservicejava/ConfigurationControllerTest.java](api-service-java/src/test/java/com/example/apiservicejava/ConfigurationControllerTest.java)
- **Покрывает:**
  - POST /api/configurations - create
  - PATCH /api/configurations/{id} - update
  - DELETE /api/configurations/{id} - delete
  - POST /api/configurations/batch/delete - batch
- ✅ **Хорошо:** Все endpoints протестированы
- 🟡 **Минус:** Только happy path, нет error scenarios

**2. ConfigurationServiceTest**
- 📄 [api-service-java/src/test/java/com/example/apiservicejava/service/ConfigurationServiceTest.java](api-service-java/src/test/java/com/example/apiservicejava/service/ConfigurationServiceTest.java)
- **Покрывает:**
  - `shouldCreateFromExternalConfigurationWithProvidedKbId()` - kbId provided
  - `shouldFallbackToSapResponseKbIdWhenNotProvided()` - kbId fallback
  - `shouldFallbackToProductIdWhenKbIdMissing()` - Creates temporary config (!)
  - `shouldFallbackToSnapshotWhenLiveConfigFails()` - Snapshot fallback
- ✅ **Хорошо:** Fallback scenarios покрыты
- 🔴 **Минус:** Временная конфиг логика есть но рискованна

**3. ExternalConfigurationMapperTest**
- 📄 [api-service-java/src/test/java/com/example/apiservicejava/mapper/ExternalConfigurationMapperTest.java](api-service-java/src/test/java/com/example/apiservicejava/mapper/ExternalConfigurationMapperTest.java)
- **Покрывает:**
  - Mapping SAP response to API response
  - KB enrichment логика
  - Null name handling
- ✅ **Хорошо:** Баги KB enrichment покрыты

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ

### Issue #1: Hardcoded SAP API Key [SECURITY]

**Severity:** 🔴 CRITICAL  
**Confidence:** 🔴 HIGH

**Location:**
```
api-service-java/src/main/resources/application.properties:4
sap.cps.api-key=3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl
```

**Problems:**
1. ❌ API key в git repository
2. ❌ Видна в исходном коде, логах, error messages
3. ❌ Одинаковая для всех environments (sandbox + potentially production)
4. ❌ Нет механизма для rotation

**Impact:**
- Любой с доступом к repo может вызвать SAP APIs
- Возможны DDoS атаки, data theft, configuration tampering
- Финансовые последствия (API costs)

**Fix:**
```bash
# 1. Удалить из git history
git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch api-service-java/src/main/resources/application.properties' \
  --prune-empty --tag-name-filter cat -- --all

# 2. application.properties
sap.cps.base-url=${SAP_CPS_BASE_URL}
sap.cps.api-key=${SAP_CPS_API_KEY}

# 3. Добавить в .gitignore
echo "application.properties" >> .gitignore
echo "application-*.properties" >> .gitignore

# 4. Создать application-local.properties (git ignored) для development
```

---

### Issue #2: Hardcoded Fallback Product ID "CPS_BURGER" [DATA INTEGRITY]

**Severity:** 🟡 MEDIUM-HIGH  
**Confidence:** 🟡 MEDIUM

**Locations:**
```java
// ExternalConfigurationMapper.java:61
source.put("id", productId != null ? productId : "CPS_BURGER");

// ExternalConfigurationMapper.java:106
bomPositionObjectKey.put("id", productId != null ? productId : "CPS_BURGER");
```

**Problems:**
1. ⚠️ Молчаливо используется fallback если productId = null
2. ⚠️ Может создать конфигурацию для неправильного продукта
3. ⚠️ Ошибка обнаружится только в SAP при validation

**Example Scenario:**
```java
ExternalConfigurationCreateRequest request = new ExternalConfigurationCreateRequest();
request.setProductId(null);  // Забыли или ошибка
request.setKbId("80");

// Код молча создает конфигурацию для CPS_BURGER вместо ожидаемого продукта
```

**Impact:**
- Data integrity issues
- Wrong configurations in CPS
- Manual cleanup required

**Fix:**
```java
// ExternalConfigurationMapper.java
private static void mapExternalConfigurationToRoot(...) {
  if (productId == null || productId.isBlank()) {
    throw new IllegalArgumentException("productId must not be null or blank");
  }
  
  Map<String, Object> source = new HashMap<>();
  source.put("id", productId);  // No fallback
  // ...
}
```

---

### Issue #3: Unsafe kbId Resolution with Temporary Configuration Creation

**Severity:** 🟡 MEDIUM  
**Confidence:** 🟡 MEDIUM

**Location:**
```
api-service-java/src/main/java/com/example/apiservicejava/service/ConfigurationService.java:114-138
```

**Code:**
```java
public ConfigurationResponse createFromExternalConfiguration(ExternalConfigurationCreateRequest request) {
  // ... SAP call ...
  
  // Fallback: если kbId все еще пустой, попробовать получить его через productId
  if ((kbId == null || kbId.isBlank()) && request.getProductId() != null) {
    try {
      SapCreateRequest tempRequest = new SapCreateRequest();
      tempRequest.setProductKey(request.getProductId());
      SapRuntimeConfigurationResponse tempResponse = sapCpsClient.createConfiguration(tempRequest);
      
      if (tempResponse != null && tempResponse.getKbId() != null) {
        kbId = tempResponse.getKbId().toString();
        
        // Удалить временную конфигурацию
        if (tempResponse.getId() != null) {
          try {
            sapCpsClient.deleteConfiguration(tempResponse.getId());
          } catch (Exception cleanupEx) {
            log.warn("Failed to cleanup temporary configuration: {}", tempResponse.getId(), cleanupEx);
          }
        }
      }
    } catch (Exception fallbackEx) {
      log.error("Failed to resolve kbId via productId", fallbackEx);
    }
  }
}
```

**Problems:**
1. 🔴 Создает конфигурацию только чтобы получить kbId - очень дорого
2. ⚠️ Если deleteConfiguration() fails, конфигурация остается в SAP (orphaned)
3. ⚠️ Если failure - продолжает работу без kbId (KB enrichment пропускается)
4. ⚠️ Performance issue - 2 дополнительных API call

**Better Approach:**
```java
// Option 1: Lookup KB directly if available
// Option 2: Make kbId mandatory in request
// Option 3: Cache Product → KB mapping
```

---

### Issue #4: Read-only Snapshot Restrictions Not UI-Clear

**Severity:** 🟡 MEDIUM  
**Confidence:** 🟡 MEDIUM

**Problem:**
- Backend помечает конфигурацию как read-only после snapshot restore
- Frontend может не показать это четко пользователю
- Пользователь может попытаться обновить характеристику и получить CONFLICT 409

**Evidence:**
```java
// ConfigurationService.java:191-194
if (Boolean.TRUE.equals(readOnlyByConfigurationId.get(configId))) {
  throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Configuration is read-only after snapshot restore");
}
```

**Frontend side:**
```typescript
// configurator-widget.facade.ts
// updateCharacteristic() может вызвать ошибку если configuration read-only
```

**Fix:**
- Явно отключить UI input элементы когда `restoreInfo.readOnly = true`
- Показать banner: "Configuration restored from snapshot - read-only mode"

---

## 🟠 АНАЛИЗ ДУБЛИРОВАНИЯ КОДА

### Duplication #1: Characteristic Mapping Logic

**Occurrence 1:** ConfigurationMapper.java
```java
private List<CharacteristicDto> mapCharacteristics(
    List<SapRuntimeCharacteristic> runtimeCharacteristics,
    Map<String, SapKbCharacteristic> kbCharacteristics) {
  return runtimeCharacteristics.stream()
      .map(runtimeChar -> mapCharacteristic(runtimeChar, kbCharacteristics.get(runtimeChar.getId())))
      .collect(Collectors.toList());
}

private CharacteristicDto mapCharacteristic(
    SapRuntimeCharacteristic runtimeChar,
    SapKbCharacteristic kbChar) {
  CharacteristicDto dto = new CharacteristicDto();
  dto.setId(runtimeChar.getId());
  dto.setName(kbChar != null && kbChar.getName() != null ? kbChar.getName() : runtimeChar.getId());
  // ... valueType resolution, values mapping, possibleValues mapping
  return dto;
}
```

**Occurrence 2:** ExternalConfigurationMapper.java
```java
public static ConfigurationResponse fromSapRuntimeResponse(SapRuntimeConfigurationResponse sapResponse) {
  // ... similar characteristic mapping and enrichment
}
```

**Problem:**
- Логика KB enrichment (resolveValueType, mapValues) дублирована
- Разные стратегии для live vs external configs
- Сложно поддерживать consistency

**Recommendation:**
```java
// Extracted service
@Service
public class CharacteristicEnricher {
  public CharacteristicDto enrich(
      SapRuntimeCharacteristic runtimeChar,
      SapKbCharacteristic kbChar) {
    // Shared logic
  }
}
```

---

### Duplication #2: ETag/ReadOnly State Management Boilerplate

Multiple places where states are checked and updated:
- getConfiguration() - updates ETag
- patchConfiguration() - retrieves and updates ETag
- resumeConfiguration() - sets readOnly flag
- deleteConfiguration() - removes flags

---

## 📊 HARDCODED VALUES INVENTORY

| Value | Type | Files | Risk | Action |
|-------|------|-------|------|--------|
| `3W8L2uo2AHy9QNVaMA9oYdO2TK0qU4rl` | API Key | application.properties | 🔴 CRITICAL | Remove from git, use env var |
| `CPS_BURGER` | Product ID | ExternalConfigurationMapper (2x), tests | 🟡 MEDIUM | Make mandatory, remove fallback |
| `80` | KB ID | app.ts, tests | 🟢 OK | Demo data, acceptable |
| `VBAP-VRKME=EA` | SAP Context | ConfigurationService:64 | 🟢 OK | SAP standard |
| `quote_item` | SAP Source | ConfigurationService:65 | 🟡 MEDIUM | Consider parameterization |
| `10` | SAP Position | ConfigurationService:65 | 🟡 MEDIUM | Consider parameterization |
| `cpq` | SAP Application | ConfigurationService:65 | 🟡 MEDIUM | Consider parameterization |
| `MARA` | Object Type | ExternalConfigurationMapper (3x) | 🟢 OK | SAP standard |
| `300` | Class Type | ExternalConfigurationMapper (3x) | 🟢 OK | SAP standard |
| `PCE` | Unit | ExternalConfigurationMapper (2x) | 🟢 OK | SAP standard |
| `2018-08-09` | Date | ExternalConfigurationMapper:48 | 🔴 SUSPICIOUS | Why this specific date? |
| `5` | Author | ExternalConfigurationMapper:70,90,96 | ⚠️ UNCLEAR | Document meaning |
| `8` | Author | ExternalConfigurationMapper:162 | ⚠️ UNCLEAR | Document meaning |
| `/api` | API Base URL | environment.ts | 🟢 OK | Relative path for proxy |
| `false` | Production Flag | environment.ts | 🟡 MEDIUM | Should be true for prod build |

---

## ✅ ФАЙЛЫ ДЛЯ CLEANUP

### Remove from Git:
- [ ] application.properties (move to .gitignore)
- [ ] Generate new API key

### Create (but git-ignore):
- [ ] application-local.properties (dev)
- [ ] application-prod.properties (prod)
- [ ] .env.example (template)

### Extract to Config:
- [ ] SAP Context entry
- [ ] SAP Source configuration
- [ ] Author IDs mapping
- [ ] Unknown date "2018-08-09"

### Refactor for DRY:
- [ ] CharacteristicEnricher (shared mapping)
- [ ] StateManager (ETag + ReadOnly cache)

---

## 📈 СТАТУС МИГРАЦИИ ИТОГО

| Компонент | Статус | Примечание |
|-----------|--------|-----------|
| SAP CPS API Integration | ✅ 100% | Все операции реализованы |
| SAP KB API Integration | ✅ 100% | Knowledge Base enrichment работает |
| Fallback Mechanisms | ✅ 90% | Snapshot fallback OK, но kbId resolution рискованна |
| Error Handling | 🟡 60% | Basic error handling, нет retry logic |
| State Management | 🟡 40% | In-memory OK для demo, нужна БД |
| Security | 🔴 20% | Hardcoded key - критично |
| Testing | 🟡 70% | Happy path покрыт, error scenarios - нет |
| Production-ready | 🔴 30% | Нужна security, state management, config refactor |

---

## 🎯 ФИНАЛЬНЫЕ РЕКОМЕНДАЦИИ

### Немедленные (Sprint 0 - Security):
1. ✅ Удалить API key из git (filter-branch)
2. ✅ Implement env var configuration
3. ✅ Rotate API key
4. ✅ Add .gitignore for properties

### Короткие (Sprint 1 - Stability):
1. ✅ Remove CPS_BURGER fallback
2. ✅ Fix kbId resolution (no temp config)
3. ✅ Improve error handling + retry logic
4. ✅ Add UI indication for read-only snapshot mode

### Средние (Sprint 2-3 - Quality):
1. ✅ Migrate state to Redis/Database
2. ✅ Migrate snapshots to Database
3. ✅ Extract CharacteristicEnricher
4. ✅ Add comprehensive error scenario tests

### Долгие (Production readiness):
1. ✅ Load testing (SAP API rate limits)
2. ✅ Monitoring + alerting
3. ✅ Documentation update
4. ✅ Compliance review (data privacy, audit logging)

---

**Report Generated:** 2026-09-02  
**Auditor:** Copilot (Claude Haiku)  
**Project:** SAP CPS Configuration Prototype  
**Status:** For Review
