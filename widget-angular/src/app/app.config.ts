import { ApplicationConfig, ErrorHandler } from '@angular/core';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';

class WidgetErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    // Centralized widget error handling — replace with host-bridge or telemetry if needed
    // Keep minimal and resilient for third-party host pages
    // eslint-disable-next-line no-console
    console.error('[Configurator Widget] Global error:', error);
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: ErrorHandler, useClass: WidgetErrorHandler },
    provideClientHydration(),
    provideHttpClient(),
  ],
};
