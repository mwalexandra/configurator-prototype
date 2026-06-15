import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ConfiguratorWidgetComponent } from './configurator-widget.component';
import { ConfigurationApiService } from '../services/configuration-api.service';
import { ConfigurationResponse } from '../models/configuration.models';

describe('ConfiguratorWidgetComponent', () => {
  let component: ConfiguratorWidgetComponent;

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
      apiBaseUrl: 'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev',
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
      apiBaseUrl: 'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev',
      mode: 'resume',
      resume: {
        configurationId: 'cfg-999'
      }
    };

    const emitSpy = vi.spyOn(component.configurationStarted, 'emit');

    component.ngOnInit();

    // API-Basis-URL muss gesetzt werden
    expect(apiService.setApiBaseUrl).toHaveBeenCalledWith(
      'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev'
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
      apiBaseUrl: 'http://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev',
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

  // Hier können später Integrationstests (mit echtem HttpClient) ergänzt werden.
});