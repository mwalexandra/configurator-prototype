import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfiguratorWidgetComponent } from './configurator-widget/configurator-widget.component';
import {
  WidgetInputConfig,
  CompletedConfigurationResult,
  ConfigurationSnapshot
} from './models/configuration.models';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  startedConfigId = signal<string | null>(null);
  completedResult = signal<CompletedConfigurationResult | null>(null);
  //completedSnapshot = signal<ConfigurationSnapshot | null>(null);
  finalSnapshot = signal<ConfigurationSnapshot | null>(null);
  lastError = signal<string | null>(null);

  private apiBaseUrl = environment.apiUrl;

  widgetConfig: WidgetInputConfig = {
    apiBaseUrl: this.apiBaseUrl,
    mode: 'create',
    productId: 'CPS_BURGER',
    kbId: '80'
  };

  onConfigurationStarted(configId: string): void {
    this.startedConfigId.set(configId);
  }

  onConfigurationCompleted(snapshot: ConfigurationSnapshot): void {
    this.finalSnapshot.set(snapshot);
    this.lastError.set(null);
  }

  async onAddedToCart(result: CompletedConfigurationResult): Promise<void> {
    this.completedResult.set(result);
    this.finalSnapshot.set(result.snapshot);
    this.lastError.set(null);

    try {
      const response = await fetch('/api/saved-configurations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          label: `Saved ${new Date().toISOString()}`,
          productId: result.productId,
          configurationId: result.configurationId,
          snapshot: result.snapshot
        })
      });

      if (!response.ok) {
        throw new Error(`Save failed: ${response.status}`);
      }

      const saved = await response.json();
      console.log('Saved configuration', saved);
    } catch (error) {
      this.lastError.set(error instanceof Error ? error.message : 'Save failed');
      throw error;
    }
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
    //this.completedSnapshot.set(null);
    this.finalSnapshot.set(null);
    this.lastError.set(null);
  }

  async switchToResumeMode(): Promise<void> {
    const response = await fetch('/api/saved-configurations');
    const configs = await response.json();

    if (!configs || configs.length === 0) {
      this.lastError.set('Keine gespeicherten Konfigurationen gefunden');
      return;
    }

    const selected = [...configs]
        .filter((c: any) =>
          c?.snapshot?.configurationId &&
          c?.snapshot?.productId &&
          c?.snapshot?.rootItem
        )
        .sort((a: any, b: any) =>
          new Date(b.savedAt ?? b.snapshot?.savedAt ?? 0).getTime() -
          new Date(a.savedAt ?? a.snapshot?.savedAt ?? 0).getTime()
        )[0];

    if (!selected) {
      this.lastError.set('Keine gültige gespeicherte Konfiguration gefunden');
      return;
    }

    console.log('app.ts - 112');
    console.log('selected', selected);
    console.log('selected.snapshot', selected?.snapshot);

    this.widgetConfig = {
      apiBaseUrl: this.widgetConfig.apiBaseUrl,
      mode: 'resume',
      resume: {
        configurationId: selected.configurationId,
        snapshot: selected.snapshot,
        sourceContext: 'generic'
      }
    };
  }
}
