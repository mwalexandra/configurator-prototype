import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateConfigurationRequest,
  ConfigurationResponse,
  UpdateCharacteristicRequest
} from '../models/configuration.models';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationApiService {
  private http = inject(HttpClient);
  private apiBaseUrl = '';

  setApiBaseUrl(apiBaseUrl: string): void {
    this.apiBaseUrl = apiBaseUrl.replace(/\/$/, '');
  }

  createConfiguration(
    payload: CreateConfigurationRequest
  ): Observable<ConfigurationResponse> {
    return this.http.post<ConfigurationResponse>(
      `${this.apiBaseUrl}/configurations`,
      payload
    );
  }

  getConfiguration(
    configurationId: string
  ): Observable<ConfigurationResponse> {
    return this.http.get<ConfigurationResponse>(
      `${this.apiBaseUrl}/configurations/${configurationId}`
    );
  }

  patchConfiguration(
    configurationId: string,
    payload: UpdateCharacteristicRequest
  ): Observable<ConfigurationResponse> {
    return this.http.patch<ConfigurationResponse>(
      `${this.apiBaseUrl}/configurations/${configurationId}`,
      {
        characteristicId: payload.characteristicId,
        value: payload.value
      }
    );
  }
}