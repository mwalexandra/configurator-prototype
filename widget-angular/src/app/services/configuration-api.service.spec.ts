import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';

import { ConfigurationApiService } from './configuration-api.service';
import {
  CreateConfigurationRequest,
  ConfigurationResponse
} from '../models/configuration.models';

import { environment } from '../../environments/environment';

describe('ConfigurationApiService', () => {
  let service: ConfigurationApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ConfigurationApiService]
    });

    service = TestBed.inject(ConfigurationApiService);
    httpMock = TestBed.inject(HttpTestingController);

    service.setApiBaseUrl(environment.apiUrl);
  });

  afterEach(() => {
    httpMock.verify();
  });

    it('should POST createConfiguration with correct payload', () => {
    const payload: CreateConfigurationRequest = {
      productId: 'CPSBURGER',
      kbId: '80'
    };

    const mockResponse: ConfigurationResponse = {
      configurationId: 'cfg-123',
      productId: 'CPSBURGER',
      kbId: '80',
      complete: false,
      consistent: true,
      rootItem: {
        id: '1',
        key: 'CPSBURGER',
        complete: false,
        consistent: true,
        characteristics: []
      },
      groups: [],
      messages: []
    };

    service.createConfiguration(payload).subscribe((response) => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/configurations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush(mockResponse);
    });

    it('should PATCH configuration with characteristicId and value', () => {
    const configurationId = 'cfg-123';

    const payload = {
        configurationId: 'cfg-123',
        characteristicId: 'CPS_OPTION_M',
        value: 'M'
    };

    const mockResponse: ConfigurationResponse = {
        configurationId: 'cfg-123',
        productId: 'CPSBURGER',
        kbId: '80',
        complete: false,
        consistent: true,
        rootItem: {
        id: '1',
        key: 'CPSBURGER',
        complete: false,
        consistent: true,
        characteristics: []
        },
        groups: [],
        messages: []
    };

    service.patchConfiguration(configurationId, payload).subscribe((response) => {
        expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/configurations/cfg-123`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({
        characteristicId: 'CPS_OPTION_M',
        value: 'M'
    });

    req.flush(mockResponse);
    });

    it('should POST completeConfiguration with empty body', () => {
        const configurationId = 'cfg-123';

        const mockResponse: ConfigurationResponse = {
            configurationId: 'cfg-123',
            productId: 'CPSBURGER',
            kbId: '80',
            complete: true,
            consistent: true,
            rootItem: {
            id: '1',
            key: 'CPSBURGER',
            complete: true,
            consistent: true,
            characteristics: []
            },
            groups: [],
            messages: []
        };

        service.completeConfiguration(configurationId).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(
            `${environment.apiUrl}/configurations/cfg-123/complete`
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({});

        req.flush(mockResponse);
    });
});
