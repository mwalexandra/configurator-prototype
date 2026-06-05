import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateConfigurationRequest,
  CreateConfigurationResponse
} from '../models/configuration.models';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationApiService {
  private http = inject(HttpClient);
  private apiBaseUrl = 'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev';

  createConfiguration(
    payload: CreateConfigurationRequest
  ): Observable<CreateConfigurationResponse> {
    return this.http.post<CreateConfigurationResponse>(
      `${this.apiBaseUrl}/configurations`,
      payload
    );
  }

  patchConfiguration(
    configId: string,
    payload: { characteristic: string; value: string }
  ): Observable<{ configId: string; status: string }> {
    return this.http.patch<{ configId: string; status: string }>(
      `${this.apiBaseUrl}/configurations/${configId}`,
      payload
    );
  }
}