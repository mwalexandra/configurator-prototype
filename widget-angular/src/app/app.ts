import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfiguratorWidgetComponent } from './configurator-widget/configurator-widget.component';
import {
  ConfigurationSnapshot,
  WidgetInputConfig
} from './models/configuration.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  startedConfigId = signal<string | null>(null);
  completedSnapshot = signal<ConfigurationSnapshot | null>(null);
  lastError = signal<string | null>(null);

  widgetConfig: WidgetInputConfig = {
    apiBaseUrl: 'https://shiny-space-acorn-rwgjqrx9x9ph5774-8080.app.github.dev',
    mode: 'create',
    productId: 'CPS_BURGER',
    kbId: '80',
    locale: 'de'
  };

  onConfigurationStarted(configId: string): void {
    this.startedConfigId.set(configId);
  }

  onConfigurationCompleted(snapshot: ConfigurationSnapshot): void {
    this.completedSnapshot.set(snapshot);
  }

  onError(event: { errorCode: string; message: string }): void {
    this.lastError.set(`${event.errorCode}: ${event.message}`);
  }

  switchToCreateMode(): void {
    this.widgetConfig = {
      apiBaseUrl: this.widgetConfig.apiBaseUrl,
      mode: 'create',
      productId: 'CPS_BURGER',
      kbId: '80',
      locale: 'de'
    };
    this.startedConfigId.set(null);
    this.completedSnapshot.set(null);
    this.lastError.set(null);
  }

  switchToResumeMode(): void {
    if (!this.startedConfigId()) {
      this.lastError.set('No configurationId available yet for resume mode');
      return;
    }

    this.widgetConfig = {
      apiBaseUrl: this.widgetConfig.apiBaseUrl,
      mode: 'resume',
      resume: {
        configurationId: this.startedConfigId()!,
        sourceContext: 'generic'
      }
    };
    this.completedSnapshot.set(null);
    this.lastError.set(null);
  }
}