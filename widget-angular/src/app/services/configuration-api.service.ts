import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateConfigurationRequest,
  ConfigurationResponse,
  ResumeConfigurationRequest,
  UpdateCharacteristicRequest
} from '../models/configuration.models';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationApiService {
  private http = inject(HttpClient);

  // fallback for local prototype runs; host config should override this
  private apiBaseUrl = 'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev';

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

  completeConfiguration(
    configurationId: string
  ): Observable<ConfigurationResponse> {
    return this.http.post<ConfigurationResponse>(
      `${this.apiBaseUrl}/configurations/${configurationId}/complete`,
      {}
    );
  }

  resumeConfiguration(
    payload: ResumeConfigurationRequest
  ): Observable<ConfigurationResponse> {
    return this.http.post<ConfigurationResponse>(
      `${this.apiBaseUrl}/configurations/resume`,
      payload
    );
  }
}