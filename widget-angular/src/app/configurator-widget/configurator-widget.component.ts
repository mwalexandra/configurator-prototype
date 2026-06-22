// src/app/configurator-widget/configurator-widget.component.ts
import {
  Component,
  Input,
  OnInit,
  computed,
  output,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  Characteristic,
  CompletedConfigurationResult,
  ConfigurationResponse,
  ConfigurationMessage,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  ResumeConfigurationRequest,
  WidgetInputConfig,
  WidgetState
} from '../models/configuration.models';
import { ConfiguratorWidgetFacade } from './configurator-widget.facade';
import { createConfiguratorWidgetUiState } from './configurator-widget.ui-state';

@Component({
  selector: 'app-configurator-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configurator-widget.component.html',
  styleUrl: './configurator-widget.component.scss'
})
export class ConfiguratorWidgetComponent implements OnInit {
  @Input({ required: true }) widgetInputConfig!: WidgetInputConfig;

  configurationStarted = output<string>();
  configurationCompleted = output<ConfigurationSnapshot>();
  addedToCart = output<CompletedConfigurationResult>();
  errorOccurred = output<{ errorCode: string; message: string }>();

  configuration = signal<ConfigurationResponse | null>(null);
  configId = signal<string | null>(null);
  status = signal<WidgetState>('idle');
  errorMessage = signal<string | null>(null);

  visibleCharacteristics = computed(() =>
    (this.configuration()?.rootItem?.characteristics ?? []).filter(c => c.visible)
  );

  private facade: ConfiguratorWidgetFacade | null = null;

  readonly ui = createConfiguratorWidgetUiState(
    this.configuration,
    this.status,
    this.errorMessage
  );

  constructor(private configurationApi: ConfigurationApiService) {}

  ngOnInit(): void {
    this.facade = new ConfiguratorWidgetFacade(this.configurationApi, {
      widgetInputConfig: this.widgetInputConfig,
      configuration: this.configuration,
      configId: this.configId,
      status: this.status,
      errorMessage: this.errorMessage,
      configurationStarted: this.configurationStarted,
      configurationCompleted: this.configurationCompleted,
      addedToCart: this.addedToCart,
      errorOccurred: this.errorOccurred
    });

    this.facade.initialize();
  }

  protected get blockingIssuesForView() {
    return this.facade?.blockingIssueLabels?.() ?? [];
  }

  protected trackBlockingIssue(index: number, issue: { itemId: string; characteristicId: string }) {
    return `${issue.itemId}-${issue.characteristicId}`;
  }

  protected get subItemsForView() {
    return this.configuration()?.rootItem?.subItems ?? [];
  }

  startConfiguration(): void {
    this.facade?.startConfiguration();
  }

  resumeConfiguration(): void {
    this.facade?.resumeConfiguration();
  }

  updateCharacteristic(characteristicId: string, value: string | null): void {
    this.facade?.updateCharacteristic(characteristicId, value);
    console.log(this.facade?.subItemDebug());
    console.log(this.facade?.incompleteRequiredSubItemCharacteristics());
    console.log(this.facade?.blockingIssues());
  }

  protected updateSubItemCharacteristic(
    itemId: string,
    characteristicId: string,
    value: string | null
  ): void {
    this.facade?.updateCharacteristic(characteristicId, value, itemId);
  }

  completeConfiguration(): void {
    this.facade?.completeConfiguration();
  }

  addToCart(): void {
    this.facade?.addToCart();
  }

  getSingleSelectedValueId(char: Characteristic): string {
    return char.values?.[0]?.id ?? '';
  }

  trackByCharacteristicId(_: number, char: Characteristic): string {
    return char.id;
  }

  getGlobalMessages(): ConfigurationMessage[] {
    return this.facade?.getGlobalMessages() ?? [];
  }

  getMessagesForCharacteristic(characteristicId: string): ConfigurationMessage[] {
    return this.facade?.getMessagesForCharacteristic(characteristicId) ?? [];
  }

  hasCharacteristicProblem(characteristicId: string): boolean {
    return this.ui.problemCharacteristicIds().has(characteristicId);
  }

  isCharacteristicIncomplete(char: Characteristic): boolean {
    return !!char.required && !char.complete;
  }

  translateMode(mode: string | undefined): string {
    switch (mode) {
      case 'create':
        return 'Erstellen';
      case 'resume':
        return 'Fortsetzen';
      default:
        return mode ?? 'Unbekannt';
    }
  }

  translateStatus(status: string | null): string {
    switch (status) {
      case 'idle':
        return 'Inaktiv';
      case 'loading':
        return 'Lädt';
      case 'updating':
        return 'Aktualisiert';
      case 'completing':
        return 'Speichert';
      case 'completed':
        return 'Abgeschlossen';
      case 'error':
        return 'Fehler';
      default:
        return status ?? 'Unbekannt';
    }
  }

  translateUiState(uiState: string | null): string {
    switch (uiState) {
      case 'idle':
        return 'Inaktiv';
      case 'loading':
        return 'Lädt';
      case 'incomplete':
        return 'Unvollständig';
      case 'conflict':
        return 'Konflikt';
      case 'ready':
        return 'Bereit';
      case 'completed':
        return 'Abgeschlossen';
      case 'readonly':
        return 'Nur-Lese';
      case 'error':
        return 'Fehler';
      default:
        return uiState ?? 'Unbekannt';
    }
  }
}
