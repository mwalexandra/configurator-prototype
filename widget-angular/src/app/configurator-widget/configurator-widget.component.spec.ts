import { TestBed } from '@angular/core/testing';
import { of, type Observable } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ConfiguratorWidgetComponent } from './configurator-widget.component';
import { ConfigurationApiService } from '../services/configuration-api.service';
import { ConfigurationResponse } from '../models/configuration.models';

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
            kbId: '80',
            locale: 'en'
        };
    });

    it('should start configuration and emit configurationStarted', () => {
        const mockResponse: ConfigurationResponse = {
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
        messages: []
        };

        apiService.createConfiguration.mockReturnValue(of(mockResponse));

        const emitSpy = vi.spyOn(component.configurationStarted, 'emit');

        component.startConfiguration();

        expect(apiService.createConfiguration).toHaveBeenCalledWith({
            productId: 'CPS_BURGER',
            kbId: '80',
            locale: 'en'
        });
        expect(component.configuration()).toEqual(mockResponse);
        expect(component.configId()).toBe('cfg-123');
        expect(component.status()).toBe('loaded');
        expect(emitSpy).toHaveBeenCalledWith('cfg-123');
    });

    it('should resume configuration by configurationId', () => {
        const mockResponse: ConfigurationResponse = {
            configurationId: 'cfg-999',
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
            messages: []
        };

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

        expect(apiService.setApiBaseUrl).toHaveBeenCalledWith('https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev');
        expect(apiService.getConfiguration).toHaveBeenCalledWith('cfg-999');
        expect(component.configuration()).toEqual(mockResponse);
        expect(component.configId()).toBe('cfg-999');
        expect(component.status()).toBe('loaded');
        expect(emitSpy).not.toHaveBeenCalled();
    });

    it('should update configuration characteristic', () => {
        const initialResponse: ConfigurationResponse = {
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
            messages: []
        };

        const updatedResponse: ConfigurationResponse = {
            ...initialResponse,
            complete: true
        };

        apiService.patchConfiguration.mockReturnValue(of(updatedResponse));

        component.configuration.set(initialResponse);
        component.configId.set('cfg-123');

        component.updateCharacteristic('CPS_OPTION_M', 'M');

        expect(apiService.patchConfiguration).toHaveBeenCalledWith('cfg-123', {
            configurationId: 'cfg-123',
            characteristicId: 'CPS_OPTION_M',
            value: 'M'
        });

        expect(component.configuration()).toEqual(updatedResponse);
        expect(component.status()).toBe('loaded');
    });

    it('should complete configuration', () => {
    component.config = {
        apiBaseUrl: 'http://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev',
        mode: 'create',
        productId: 'CPS_BURGER',
        kbId: '80',
        locale: 'de'
    };

    const completedResponse: ConfigurationResponse = {
        configurationId: 'cfg-123',
        productId: 'CPS_BURGER',
        kbId: '80',
        complete: true,
        consistent: true,
        rootItem: {
        id: '1',
        key: 'CPS_BURGER',
        complete: true,
        consistent: true,
        characteristics: []
        },
        groups: [],
        messages: []
    };

    apiService.completeConfiguration.mockReturnValue(of(completedResponse));

    component.configuration.set(completedResponse);
    component.configId.set('cfg-123');

    const emitSpy = vi.spyOn(component.configurationCompleted, 'emit');

    component.completeConfiguration();

    expect(apiService.completeConfiguration).toHaveBeenCalledWith('cfg-123');
    expect(component.status()).toBe('completed');
    expect(emitSpy).toHaveBeenCalledOnce();
    expect(emitSpy.mock.calls[0][0]).toEqual(
        expect.objectContaining({
        configurationId: 'cfg-123',
        productId: 'CPS_BURGER',
        kbId: '80',
        complete: true,
        consistent: true,
        rootItem: {
            id: '1',
            key: 'CPS_BURGER',
            complete: true,
            consistent: true,
            characteristics: []
        },
        groups: [],
        messages: []
        })
    );
    expect(emitSpy.mock.calls[0][0].savedAt).toEqual(expect.any(String));
    });

    it('should not start configuration and emit error when productId or kbId is missing', () => {
        apiService.createConfiguration.mockClear();
        // базовый конфиг из beforeEach уже установлен
        // ломаем значение прямо перед вызовом
        component.config.mode = 'create';
        component.config.productId = '';
        component.config.kbId = '';

        const errorSpy = vi.spyOn(component.errorOccurred, 'emit');

        component.startConfiguration();

        expect(component.status()).toBe('error');
        expect(component.errorMessage()).toBe('Missing productId or kbId for create mode');

        expect(errorSpy).toHaveBeenCalledWith({
            errorCode: 'CONFIG_INPUT_INVALID',
            message: 'Missing productId or kbId for create mode'
        });

        expect(apiService.createConfiguration).not.toHaveBeenCalled();
    });
});