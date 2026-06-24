import { Component, computed, signal } from '@angular/core';
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
  finalSnapshot = signal<ConfigurationSnapshot | null>(null);
  lastError = signal<string | null>(null);

  hostMode = signal<'initial' | 'create' | 'resume'>('initial');
  isLoadingSavedConfig = signal(false);

  private apiBaseUrl = environment.apiUrl;

  widgetConfig: WidgetInputConfig | null = null;
  widgetVisible = signal(false);

  readonly canChooseCreate = computed(() => !this.isLoadingSavedConfig());
  readonly canChooseResume = computed(() => !this.isLoadingSavedConfig());
  readonly showWidget = computed(() => this.widgetConfig !== null);
  readonly showCreateOnlyHostBlocks = computed(() => this.hostMode() === 'create');
  readonly showResumeOnlyHostBlocks = computed(() => this.hostMode() === 'resume');
  readonly showModeChooser = computed(() => this.hostMode() === 'initial');
  readonly isCreateMode = computed(() => this.hostMode() === 'create');
  readonly isResumeMode = computed(() => this.hostMode() === 'resume');

  readonly isCreateModeButtonDisabled = computed(
    () => this.isLoadingSavedConfig() || this.isCreateMode()
  );

  readonly isResumeModeButtonDisabled = computed(
    () => this.isLoadingSavedConfig() || this.isResumeMode()
  );

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

      await response.json();

    } catch (error) {
      this.lastError.set(error instanceof Error ? error.message : 'Save failed');
      throw error;
    }
  }

  onError(event: { errorCode: string; message: string }): void {
    this.lastError.set(`${event.errorCode}: ${event.message}`);
  }

  async switchToCreateMode(): Promise<void> {
    this.hostMode.set('create');
    this.startedConfigId.set(null);
    this.completedResult.set(null);
    this.finalSnapshot.set(null);
    this.lastError.set(null);

    this.widgetConfig = null;
    await Promise.resolve();

    this.widgetConfig = {
      apiBaseUrl: this.apiBaseUrl,
      mode: 'create',
      productId: 'CPS_BURGER',
      kbId: '80'
    };
  }

  async switchToResumeMode(): Promise<void> {
    this.isLoadingSavedConfig.set(true);
    this.lastError.set(null);

    try {
      const response = await fetch('/api/saved-configurations');
      const configs = await response.json();

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

      this.hostMode.set('resume');
      this.startedConfigId.set(null);
      this.completedResult.set(null);
      this.finalSnapshot.set(null);

      this.widgetConfig = null;
      await Promise.resolve();
      
      this.widgetConfig = {
        apiBaseUrl: this.apiBaseUrl,
        mode: 'resume',
        resume: {
          configurationId: selected.configurationId,
          snapshot: selected.snapshot,
          sourceContext: 'generic'
        }
      };
      
      await this.remountWidget();
    } catch (error) {
      this.lastError.set(
        error instanceof Error ? error.message : 'Geladene Konfiguration konnte nicht gelesen werden'
      );
    } finally {
      this.isLoadingSavedConfig.set(false);
    }
  }

  resetModeSelection(): void {
    this.hostMode.set('initial');
    this.widgetConfig = null;
    this.startedConfigId.set(null);
    this.completedResult.set(null);
    this.finalSnapshot.set(null);
    this.lastError.set(null);
  }

  private async remountWidget(): Promise<void> {
    this.widgetVisible.set(false);
    await Promise.resolve();
    this.widgetVisible.set(true);
  }
}
