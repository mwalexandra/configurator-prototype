# configurator-prototype
SAP CPS Configuration Widget - Bachelorarbeit Projekt

## Architektur

```
┌─────────────────────┐
│  Angular Widget     │  Frontend UI
│  (widget-angular)   │
└──────────┬──────────┘
           │ HTTP
           ▼
┌─────────────────────┐
│  Java API Service   │  Backend + SAP CPS Integration
│  (api-service-java) │
└──────────┬──────────┘
           │ REST API
           ▼
┌─────────────────────┐
│  SAP CPS Runtime    │  Configuration Engine
└─────────────────────┘
```

## Stack

**Frontend:** Angular 19 + TypeScript + SCSS  
**Backend:** Java 17 + Spring Boot 3.4 + Maven  
**Integration:** SAP CPS API v2 + SAP Knowledge Base API

## Quick Start

Tenant-Parametern in Enviroment hinzuzufügen. 

### Backend
```bash
cd api-service-java
load_cps_env           # Um die Credentials für Curaflow-CPS-Tenant zu kriegen
./mvnw spring-boot:run
# Läuft auf http://localhost:8080
```

### Frontend
```bash
cd widget-angular
npm install
npm start
# Läuft auf http://localhost:4200
```

### Konfiguration
`api-service-java/src/main/resources/application.properties`:
```properties
sap.cps.base-url=<SAP_CPS_URL>
sap.cps.api-key=<API_KEY>
```

## API Endpoints

### Configurations
| Methode |          Endpoint                   |              Beschreibung                  |
|---------|-------------------------------------|--------------------------------------------|
| POST    | `/api/configurations`               | Neue Konfiguration erstellen               |
| POST    | `/api/configurations/resume`        | Konfiguration fortsetzen (live/snapshot)   |
| POST    | `/api/configurations/external`      | Aus externer Config erstellen              |
| GET     | `/api/configurations/{id}`          | Konfiguration abrufen                      |
| PATCH   | `/api/configurations/{id}`          | Charakteristik ändern                      |
| POST    | `/api/configurations/{id}/complete` | Konfiguration abschließen                  |
| DELETE  | `/api/configurations/{id}`          | Konfiguration löschen                      |
| POST    | `/api/configurations/batch/delete`  | Mehrere Konfigurationen löschen            |

### Saved Configurations
| Methode |         Endpoint            |     Beschreibung       |
|---------|-----------------------------|------------------------|
| POST    | `/api/saved-configurations` | Snapshot speichern     |
| GET     | `/api/saved-configurations` | Alle Snapshots abrufen |
| GET     | `/api/saved-configurations/{id}` | Snapshot abrufen  |

## Features

✅ **Konfiguration erstellen/fortsetzen** - Create/Resume mit Live-Session oder Snapshot  
✅ **Charakteristiken bearbeiten** - Werte auswählen, Konflikte lösen  
✅ **Snapshot-System** - Lokale Speicherung + Wiederherstellung  
✅ **Read-only Fallback** - Snapshot-Anzeige wenn Runtime nicht verfügbar  
✅ **Knowledge Base Integration** - Anreicherung mit KB-Metadaten  
✅ **Konfiguration löschen** - Aus CPS Runtime entfernen  
✅ **SubItems Support** - Mehrere Konfigurationspositionen  

## Projektstruktur

```
configurator-prototype/
├── api-service-java/          # Spring Boot Backend
│   ├── src/main/java/
│   │   └── com/example/apiservicejava/
│   │       ├── controller/    # REST Controllers
│   │       ├── service/       # Business Logic + SAP Integration
│   │       ├── mapper/        # DTO Mapping
│   │       └── model/         # Data Models (API + SAP)
│   └── configurations-db/     # Lokale Snapshot-Speicherung (JSON)
│
└── widget-angular/            # Angular Frontend
    └── src/app/
        ├── configurator-widget/     # Haupt-Widget Komponente
        ├── services/                # API Service Layer
        └── models/                  # TypeScript Models
```

## Modes

**Create Mode:** Neue Konfiguration mit `productId` + `kbId`  
**Resume Mode:** Bestehende Konfiguration mit `configurationId` oder `snapshot`

## Workflow

1. **Start** → POST `/api/configurations` (Create) oder POST `/api/configurations/resume`
2. **Edit** → PATCH `/api/configurations/{id}` für jede Änderung
3. **Complete** → POST `/api/configurations/{id}/complete`
4. **Save** → POST `/api/saved-configurations` (optional)
5. **Delete** → DELETE `/api/configurations/{id}` (optional) 

