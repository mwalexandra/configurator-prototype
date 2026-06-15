import { ApplicationConfig, ErrorHandler, importProvidersFrom } from '@angular/core';
import { provideClientHydration } from '@angular/platform-browser';

import { provideHttpClient } from '@angular/common/http';

export class WidgetErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
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
