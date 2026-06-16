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
  finalSnapshot = signal<ConfigurationSnapshot | null>(null);
  lastError = signal<string | null>(null);

  widgetConfig: WidgetInputConfig = {
    apiBaseUrl: 'http://localhost:8080',
    mode: 'create',
    productId: 'CPS_BURGER',
    kbId: '80'
  };

  onConfigurationStarted(configId: string): void {
    this.startedConfigId.set(configId);
  }

  onConfigurationCompleted(snapshot: ConfigurationSnapshot): void {
    this.completedSnapshot.set(snapshot);
  }

  onAddedToCart(snapshot: ConfigurationSnapshot): void {
    this.finalSnapshot.set(snapshot);
    this.lastError.set(null);
  }

  onError(event: { errorCode: string; message: string }): void {
    this.lastError.set(`${event.errorCode}: ${event.message}`);
  }

  switchToCreateMode(): void {
    this.widgetConfig = {
      apiBaseUrl: this.widgetConfig.apiBaseUrl,
      mode: 'create',
      productId: 'CPS_BURGER',
      kbId: '80'
    };
    this.startedConfigId.set(null);
    this.completedSnapshot.set(null);
    this.finalSnapshot.set(null);
    this.lastError.set(null);
  }

  switchToResumeMode(): void {
    const snapshot = this.finalSnapshot();

    if (!snapshot) {
      this.lastError.set('No final snapshot available yet for resume mode');
      return;
    }

    this.widgetConfig = {
      apiBaseUrl: this.widgetConfig.apiBaseUrl,
      mode: 'resume',
      resume: {
        configurationId: snapshot.configurationId,
        snapshot,
        sourceContext: 'generic'
      }
    };

    this.completedSnapshot.set(null);
    this.lastError.set(null);
  }
}
