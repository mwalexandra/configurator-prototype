import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ConfiguratorWidgetComponent } from './configurator-widget.component';
import { ConfigurationApiService } from '../services/configuration-api.service';
import { ConfigurationResponse } from '../models/configuration.models';

import { environment } from '../../environments/environment';

  // Hilfsfunktion: Erzeugt eine Standard-Konfigurationsantwort,
  // die in den Tests je nach Szenario mit Overrides angepasst werden kann.
function createConfigResponse(
    overrides: Partial<ConfigurationResponse> = {}
): ConfigurationResponse {
    return {
      configurationId: 'cfg-123',
      productId: 'CPS_BURGER',
      kbId: '80',
      complete: false,
      consistent: true,
      rootItem: {
        id: '1',
        key: 'CPS_BURGER',
        complete: false,
        consistent: true,
        characteristics: []
      },
      groups: [],
      messages: [],
      ...overrides
    };
}

describe('ConfiguratorWidgetComponent', () => {
    let component: ConfiguratorWidgetComponent;

    const apiService = {
        createConfiguration: vi.fn(),
        patchConfiguration: vi.fn(),
        completeConfiguration: vi.fn(),
        getConfiguration: vi.fn(),
        resumeConfiguration: vi.fn(),
        setApiBaseUrl: vi.fn()
    };

    beforeEach(async () => {
        // Mocks vor jedem Test zurücksetzen,
        // damit die Tests unabhängig voneinander bleiben.
        apiService.createConfiguration.mockReset();
        apiService.patchConfiguration.mockReset();
        apiService.completeConfiguration.mockReset();
        apiService.getConfiguration.mockReset();
        apiService.resumeConfiguration.mockReset();
        apiService.setApiBaseUrl.mockReset();

        await TestBed.configureTestingModule({
        imports: [ConfiguratorWidgetComponent],
        providers: [{ provide: ConfigurationApiService, useValue: apiService }]
        }).compileComponents();

        const fixture = TestBed.createComponent(ConfiguratorWidgetComponent);
        component = fixture.componentInstance;

        component.config = {
        apiBaseUrl: environment.apiUrl,
        mode: 'create',
        productId: 'CPS_BURGER',
        kbId: '80'
        };
    });

    it('should start configuration and emit configurationStarted', () => {
        const mockResponse = createConfigResponse();

        apiService.createConfiguration.mockReturnValue(of(mockResponse));

        const emitSpy = vi.spyOn(component.configurationStarted, 'emit');

        component.startConfiguration();

        // Prüft, dass der Service mit dem erwarteten Payload aufgerufen wird
        expect(apiService.createConfiguration).toHaveBeenCalledWith({
        productId: 'CPS_BURGER',
        kbId: '80'
        });
        // Prüft, dass der interne Zustand aktualisiert wurde
        expect(component.configuration()).toEqual(mockResponse);
        expect(component.configId()).toBe('cfg-123');
        expect(component.status()).toBe('loaded');
        // Prüft, dass das Output-Event mit der richtigen ID emittiert wurde
        expect(emitSpy).toHaveBeenCalledWith('cfg-123');
    });

    it('should resume configuration by configurationId', () => {
        const mockResponse = createConfigResponse({ configurationId: 'cfg-999' });

        apiService.getConfiguration.mockReturnValue(of(mockResponse));

        component.config = {
        apiBaseUrl: environment.apiUrl,
        mode: 'resume',
        resume: {
            configurationId: 'cfg-999'
        }
        };

        const emitSpy = vi.spyOn(component.configurationStarted, 'emit');

        component.ngOnInit();

        // API-Basis-URL muss gesetzt werden
        expect(apiService.setApiBaseUrl).toHaveBeenCalledWith(
        environment.apiUrl
        );
        // Konfiguration wird per ID geladen
        expect(apiService.getConfiguration).toHaveBeenCalledWith('cfg-999');
        // Zustand im Widget prüfen
        expect(component.configuration()).toEqual(mockResponse);
        expect(component.configId()).toBe('cfg-999');
        expect(component.status()).toBe('loaded');
        // Beim Resume über configurationId wird kein configurationStarted-Event emittiert
        expect(emitSpy).not.toHaveBeenCalled();
    });

    it('should update configuration characteristic', () => {
        const initialResponse = createConfigResponse();
        const updatedResponse = createConfigResponse({ complete: true });

        apiService.patchConfiguration.mockReturnValue(of(updatedResponse));

        // Ausgangszustand setzen
        component.configuration.set(initialResponse);
        component.configId.set('cfg-123');

        component.updateCharacteristic('CPS_OPTION_M', 'M');

        // Prüfen, dass der Patch-Call korrekt war
        expect(apiService.patchConfiguration).toHaveBeenCalledWith('cfg-123', {
        configurationId: 'cfg-123',
        characteristicId: 'CPS_OPTION_M',
        value: 'M'
        });

        // Antwort wurde in den internen Zustand übernommen
        expect(component.configuration()).toEqual(updatedResponse);
        expect(component.status()).toBe('loaded');
    });

    it('should complete configuration', () => {
        component.config = {
        apiBaseUrl: environment.apiUrl,
        mode: 'create',
        productId: 'CPS_BURGER',
        kbId: '80'
        };

        const completedResponse = createConfigResponse({ complete: true });

        apiService.completeConfiguration.mockReturnValue(of(completedResponse));

        // Bereits geladene Konfiguration simulieren
        component.configuration.set(completedResponse);
        component.configId.set('cfg-123');

        const emitSpy = vi.spyOn(component.configurationCompleted, 'emit');

        component.completeConfiguration();

        // Service-Aufruf prüfen
        expect(apiService.completeConfiguration).toHaveBeenCalledWith('cfg-123');
        // Status im Widget prüfen
        expect(component.status()).toBe('completed');
        // Output-Event mit Snapshot prüfen
        expect(emitSpy).toHaveBeenCalledOnce();
        expect(emitSpy.mock.calls[0][0]).toEqual(
        expect.objectContaining({
            configurationId: 'cfg-123',
            productId: 'CPS_BURGER',
            kbId: '80',
            complete: true,
            consistent: true,
            rootItem: completedResponse.rootItem,
            groups: [],
            messages: []
        })
        );
        expect(emitSpy.mock.calls[0][0].savedAt).toEqual(expect.any(String));
    });

    it('should not start configuration and emit error when productId or kbId is missing', () => {
        // Basiskonfiguration manipulieren, um ungültigen Input zu simulieren
        component.config.mode = 'create';
        component.config.productId = '';
        component.config.kbId = '';

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        component.startConfiguration();

        // Widget geht in den Fehlerzustand
        expect(component.status()).toBe('error');
        expect(component.errorMessage()).toBe('Missing productId or kbId for create mode');

        // Fehler-Event nach außen emittiert
        expect(errorSpy).toHaveBeenCalledWith({
        errorCode: 'CONFIG_INPUT_INVALID',
        message: 'Missing productId or kbId for create mode'
        });

        // Kein Aufruf des API-Services erfolgt
        expect(apiService.createConfiguration).not.toHaveBeenCalled();
    });

    it('should apply snapshot fallback in read-only mode when resume by configurationId fails', () => {
        const snapshot = {
            configurationId: 'snapshot-1',
            productId: 'CPS_BURGER',
            kbId: '80',
            savedAt: new Date().toISOString(),
            complete: false,
            consistent: true,
            rootItem: {
            id: '1',
            key: 'CPS_BURGER',
            complete: false,
            consistent: true,
            characteristics: []
            },
            groups: [],
            messages: []
        };

        // API-Aufruf für getConfiguration soll mit Fehler enden
        apiService.getConfiguration.mockReturnValue({
            subscribe: ({ next, error }: any) => {
            if (error) {
                error(new Error('Backend error'));
            }
            }
        } as any);

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        component.config = {
            apiBaseUrl: environment.apiUrl,
            mode: 'resume',
            resume: {
            configurationId: 'cfg-999',
            snapshot
            }
        };

        component.ngOnInit();

        const cfg = component.configuration();

        // Fallback-Snapshot wurde angewendet
        expect(cfg).not.toBeNull();
        expect(cfg?.configurationId).toBe('snapshot-1');
        expect(cfg?.productId).toBe('CPS_BURGER');
        expect(cfg?.kbId).toBe('80');
        expect(cfg?.restoreInfo?.readOnly).toBe(true);
        expect(cfg?.restoreInfo?.strategy).toBe('READONLYSNAPSHOT');

        // Status ist "loaded", keine zusätzliche Fehler-Emission
        expect(component.status()).toBe('loaded');
        expect(component.errorMessage()).toBeNull();
        expect(errorSpy).not.toHaveBeenCalled();
    });

    it('should set error state and emit error when resume configuration has neither configurationId nor snapshot', () => {
        // Ungültige Resume-Konfiguration: weder ID noch Snapshot gesetzt
        component.config = {
            apiBaseUrl: environment.apiUrl,
            mode: 'resume',
            resume: {} as any
        };

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        // Act: ngOnInit löst resumeConfiguration() aus
        component.ngOnInit();

        // Widget geht in den Fehlerzustand
        expect(component.status()).toBe('error');
        expect(component.errorMessage()).toBe('Resume mode requires configurationId or snapshot');

        // Fehler-Event nach außen
        expect(errorSpy).toHaveBeenCalledWith({
            errorCode: 'CONFIG_RESUME_INPUT_INVALID',
            message: 'Resume mode requires configurationId or snapshot'
        });
    });

    it('should not complete configuration when in read-only mode', () => {
        // Ausgangszustand: Konfiguration im Read-Only-Modus
        const readonlyConfig: ConfigurationResponse = {
            configurationId: 'cfg-readonly',
            productId: 'CPS_BURGER',
            kbId: '80',
            complete: false,
            consistent: true,
            rootItem: {
            id: '1',
            key: 'CPS_BURGER',
            complete: false,
            consistent: true,
            characteristics: []
            },
            groups: [],
            messages: [],
            restoreInfo: {
            mode: 'resume',
            status: 'FALLBACKAPPLIED',
            strategy: 'READONLYSNAPSHOT',
            liveSessionAvailable: false,
            snapshotUsed: true,
            readOnly: true,
            message: 'read-only test'
            }
        };

        // Konfiguration und ID im Widget setzen
        component.configuration.set(readonlyConfig);
        component.configId.set('cfg-readonly');

        const completeSpy = apiService.completeConfiguration;
        const completedEventSpy = vi.spyOn(component.configurationCompleted, 'emit');

        // Status vor dem Aufruf merken
        const prevStatus = component.status();

        // Act: Versuch, die Konfiguration abzuschließen
        component.completeConfiguration();

        // Service darf nicht aufgerufen werden
        expect(completeSpy).not.toHaveBeenCalled();

        // Status bleibt unverändert
        expect(component.status()).toBe(prevStatus);

        // Kein Completed-Event wird emittiert
        expect(completedEventSpy).not.toHaveBeenCalled();
    });

});

  // Integrationstests
describe('ConfiguratorWidgetComponent (Integration)', () => {
    let component: ConfiguratorWidgetComponent;
    let httpMock: HttpTestingController;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
        imports: [ConfiguratorWidgetComponent],
        providers: [
            ConfigurationApiService,
            // Stellt HttpClient bereit
            provideHttpClient(),
            // Konfiguriert das Test-Backend (HttpTestingController)
            provideHttpClientTesting()
        ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(ConfiguratorWidgetComponent);
    component = fixture.componentInstance;

    // Gültige Config für den Create-Modus setzen
    component.config = {
      apiBaseUrl: environment.apiUrl,
      mode: 'create',
      productId: 'CPS_BURGER',
      kbId: '80'
    };

    fixture.detectChanges();
    });

    afterEach(() => {
        // Sicherstellen, dass keine offenen HTTP-Requests übrig bleiben
        httpMock.verify();
    });

    it('should start configuration via HTTP API and update state on success', () => {
        // Spy auf das Output-Event configurationStarted
        const startedSpy = vi.spyOn(component.configurationStarted, 'emit');

        // Act: Start der Konfiguration auslösen
        component.startConfiguration();

        // Erwarteten HTTP-Request abfangen
        const req = httpMock.expectOne(
        `${environment.apiUrl}/configurations`
        );

        // Request-Eigenschaften prüfen
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
        productId: 'CPS_BURGER',
        kbId: '80'
        });

        // Beispiel-Antwort wie vom Backend
        const mockResponse = createConfigResponse();

        // Antwort zurück an HttpClient schicken
        req.flush(mockResponse);

        // Interner Widgetzustand prüfen
        expect(component.configuration()).toEqual(mockResponse);
        expect(component.configId()).toBe('cfg-123');
        expect(component.status()).toBe('loaded');

        // Output-Event prüfen
        expect(startedSpy).toHaveBeenCalledWith('cfg-123');
    });

    it('should update characteristic via HTTP API and refresh configuration on success', () => {
        // Ausgangszustand: vorhandene Konfiguration mit ID
        const initialResponse = createConfigResponse();
        const updatedResponse = createConfigResponse({ complete: true });

        // Konfiguration und ID direkt im Widget setzen
        component.configuration.set(initialResponse);
        component.configId.set('cfg-123');

        // Act: Merkmalsänderung auslösen
        component.updateCharacteristic('CPS_OPTION_M', 'M');

        // Erwarteten PATCH-Request abfangen
        const req = httpMock.expectOne(
            `${environment.apiUrl}/configurations/cfg-123`
        );

        // Request-Eigenschaften prüfen
        expect(req.request.method).toBe('PATCH');
        expect(req.request.body).toEqual({
            characteristicId: 'CPS_OPTION_M',
            value: 'M'
        });

        // Backend-Antwort simulieren (z. B. Konfiguration jetzt complete)
        req.flush(updatedResponse);

        // Interner Zustand prüfen: Konfiguration übernommen, Status wieder 'loaded'
        expect(component.configuration()).toEqual(updatedResponse);
        expect(component.status()).toBe('loaded');
    });

    it('should complete configuration via HTTP API and emit snapshot on success', () => {
        // Ausgangszustand: fertige Konfiguration mit ID setzen
        const completedResponse = createConfigResponse({ complete: true });

        component.configuration.set(completedResponse);
        component.configId.set('cfg-123');

        const completedSpy = vi.spyOn(component.configurationCompleted, 'emit');

        // Act: Abschluss der Konfiguration auslösen
        component.completeConfiguration();

        // Erwarteten POST-Request auf /complete abfangen
        const req = httpMock.expectOne(
            `${environment.apiUrl}/configurations/cfg-123/complete`
        );

        // Request-Eigenschaften prüfen
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({});

        // Backend-Antwort simulieren
        req.flush(completedResponse);

        // Zustand im Widget prüfen
        expect(component.configuration()).toEqual(completedResponse);
        expect(component.configId()).toBe('cfg-123');
        expect(component.status()).toBe('completed');

        // Output-Event mit Snapshot prüfen (mindestens ID und savedAt)
        expect(completedSpy).toHaveBeenCalledOnce();
        const snapshotArg = completedSpy.mock.calls[0][0];

        expect(snapshotArg).toEqual(
            expect.objectContaining({
            configurationId: 'cfg-123',
            productId: 'CPS_BURGER',
            kbId: '80',
            complete: true,
            consistent: true
            })
        );
        expect(snapshotArg.savedAt).toEqual(expect.any(String));
    });

    it('should set error state and emit errorOccurred when updateCharacteristic HTTP call fails', () => {
        // Ausgangszustand: vorhandene Konfiguration mit ID
        const initialResponse = createConfigResponse();

        component.configuration.set(initialResponse);
        component.configId.set('cfg-123');

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        // Act: Merkmalsänderung auslösen
        component.updateCharacteristic('CPS_OPTION_M', 'M');

        // Erwarteten PATCH-Request abfangen
        const req = httpMock.expectOne(
            `${environment.apiUrl}/configurations/cfg-123`
        );

        expect(req.request.method).toBe('PATCH');
        expect(req.request.body).toEqual({
            characteristicId: 'CPS_OPTION_M',
            value: 'M'
        });

        // HTTP-Fehler simulieren
        req.flush(
            { message: 'Update failed' },
            { status: 500, statusText: 'Server Error' }
        );

        // Widget sollte in den Fehlerzustand gehen
        expect(component.status()).toBe('error');
        expect(component.errorMessage()).toBe('Failed to update configuration');

        // Fehler-Event nach außen prüfen
        expect(errorSpy).toHaveBeenCalledWith({
            errorCode: 'CONFIG_PATCH_FAILED',
            message: 'Failed to update configuration'
        });
    });

    it('should set error state and emit errorOccurred when completeConfiguration HTTP call fails', () => {
        // Ausgangszustand: Konfiguration mit ID vorhanden
        const currentResponse = createConfigResponse({ complete: false });

        component.configuration.set(currentResponse);
        component.configId.set('cfg-123');

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        // Act: Abschluss der Konfiguration auslösen
        component.completeConfiguration();

        // Erwarteten POST-Request auf /complete abfangen
        const req = httpMock.expectOne(
            `${environment.apiUrl}/configurations/cfg-123/complete`
        );

        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({});

        // HTTP-Fehler simulieren
        req.flush(
            { message: 'Complete failed' },
            { status: 500, statusText: 'Server Error' }
        );

        // Widget sollte in den Fehlerzustand gehen
        expect(component.status()).toBe('error');
        expect(component.errorMessage()).toBe('Failed to complete configuration');

        // Fehler-Event nach außen prüfen
        expect(errorSpy).toHaveBeenCalledWith({
            errorCode: 'CONFIG_COMPLETE_FAILED',
            message: 'Failed to complete configuration'
        });
    });
});
