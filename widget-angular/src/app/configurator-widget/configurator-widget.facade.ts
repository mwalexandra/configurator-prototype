// src/app/configurator-widget/configurator-widget.facade.ts
import { WritableSignal, computed } from '@angular/core';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  CompletedConfigurationResult,
  ConfigurationMessage,
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  DeleteConfigurationsRequest,
  DeleteConfigurationsResponse,
  ExternalConfigurationPayload,
  ResumeConfigurationRequest,
  WidgetInputConfig,
  WidgetState,
  ConfigurationItem,
  ExternalConfigurationItemPayload,
  Characteristic,
  MessageSeverity
} from '../models/configuration.models';
import { Observable } from 'rxjs/internal/Observable';

interface ConfiguratorWidgetFacadeContext {
  widgetInputConfig: WidgetInputConfig;
  configuration: WritableSignal<ConfigurationResponse | null>;
  configId: WritableSignal<string | null>;
  status: WritableSignal<WidgetState>;
  errorMessage: WritableSignal<string | null>;
  configurationStarted: { emit(value: string): void };
  configurationCompleted: { emit(value: ConfigurationSnapshot): void };
  addedToCart: { emit(value: CompletedConfigurationResult): void };
  errorOccurred: { emit(value: { errorCode: string; message: string }): void };
}

export class ConfiguratorWidgetFacade {

  // Debug signal to inspect the current configuration state in a simplified format
  readonly subItemDebug = computed(() => {
    const config = this.ctx.configuration();
    const subItems = config?.rootItem?.subItems ?? [];

    return subItems.map(item => ({
      id: item.id,
      key: item.key,
      complete: item.complete,
      consistent: item.consistent,
      characteristics: (item.characteristics ?? []).map(c => ({
        id: c.id,
        required: c.required,
        visible: c.visible,
        readOnly: c.readOnly,
        complete: c.complete,
        consistent: c.consistent,
        values: (c.values ?? []).map(v => v.id)
      }))
    }));
  });

  readonly blockingIssues = computed(() => {
    const config = this.ctx.configuration();
    if (!config) {
      return [];
    }

    const rootIssues = (config.rootItem?.characteristics ?? [])
      .filter(c => c.required && (!c.complete || !c.consistent))
      .map(c => ({
        level: 'root' as const,
        itemId: config.rootItem?.id ?? 'root',
        itemKey: config.rootItem?.key ?? config.productId,
        characteristicId: c.id,
        complete: c.complete,
        consistent: c.consistent
      }));

    const subItemIssues = (config.rootItem?.subItems ?? []).flatMap(item =>
      (item.characteristics ?? [])
        .filter(c => c.required && (!c.complete || !c.consistent))
        .map(c => ({
          level: 'subItem' as const,
          itemId: item.id,
          itemKey: item.key,
          characteristicId: c.id,
          complete: c.complete,
          consistent: c.consistent
        }))
    );

    return [...rootIssues, ...subItemIssues];
  });

  readonly blockingIssueLabels = computed(() => {
    return this.blockingIssues().map(issue => ({
      ...issue,
      label: issue.level === 'root'
        ? `${issue.characteristicId}`
        : `${issue.itemKey}: ${issue.characteristicId}`
    }));
  });

  // --------------------------------------------------------------------------------------

  constructor(
    private readonly api: ConfigurationApiService,
    private readonly ctx: ConfiguratorWidgetFacadeContext
  ) {}

  initialize(): void {
    this.ctx.configuration.set(null);
    this.ctx.configId.set(null);
    this.ctx.status.set('idle');
    this.ctx.errorMessage.set(null);

    this.api.setApiBaseUrl(this.ctx.widgetInputConfig.apiBaseUrl);

    if (this.ctx.widgetInputConfig.mode === 'resume') {
      this.resumeConfiguration();
    }
  }

  startConfiguration(): void {
    if (this.ctx.widgetInputConfig.mode !== 'create') {
      return;
    }

    if (!this.ctx.widgetInputConfig.productId || !this.ctx.widgetInputConfig.kbId) {
      this.ctx.status.set('error');
      this.ctx.errorMessage.set('productId oder kbId fehlt für den Erstellmodus');
      this.ctx.errorOccurred.emit({
        errorCode: 'CONFIG_INPUT_INVALID',
        message: 'productId oder kbId fehlt für den Erstellmodus'
      });
      return;
    }

    const payload: CreateConfigurationRequest = {
      productId: this.ctx.widgetInputConfig.productId,
      kbId: this.ctx.widgetInputConfig.kbId
    };

    this.ctx.status.set('loading');
    this.ctx.errorMessage.set(null);

    this.api.createConfiguration(payload).subscribe({
      next: response => this.applyConfiguration(response, true),
      error: () => this.emitError('CONFIG_START_FAILED', 'Konfiguration konnte nicht gestartet werden')
    });
  }

  resumeConfiguration(): void {
    const resume = this.ctx.widgetInputConfig.resume;

    if (!resume || (!resume.configurationId && !resume.snapshot)) {
      this.emitError(
        'CONFIG_RESUME_INPUT_INVALID',
        'Resume-Modus erfordert configurationId oder Snapshot'
      );
      return;
    }

    this.ctx.status.set('loading');
    this.ctx.errorMessage.set(null);

    const payload: ResumeConfigurationRequest = {
      configurationId: resume.configurationId,
      snapshot: resume.snapshot,
      sourceContext: resume.sourceContext
    };

    this.api.resumeConfiguration(payload).subscribe({
      next: response => this.applyConfiguration(response),
      error: () => this.emitError(
        'CONFIG_RESUME_FAILED',
        'Konfiguration konnte nicht fortgesetzt werden'
      )
    });
  }

  updateCharacteristic(
    characteristicId: string,
    value: string | null,
    itemId?: string
  ): void {
    const currentConfigId = this.ctx.configId();
    const current = this.ctx.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
      return;
    }

    if (this.ctx.status() === 'completed') {
      return;
    }

    this.ctx.status.set('updating');
    this.ctx.errorMessage.set(null);

    this.api.patchConfiguration(currentConfigId, {
      configurationId: currentConfigId,
      itemId,
      characteristicId,
      value
    }).subscribe({
      next: response => this.applyConfiguration(response),
      error: () => this.emitError('CONFIG_PATCH_FAILED', 'Konfiguration konnte nicht aktualisiert werden')
    });
  }

  completeConfiguration(): void {
    const currentConfigId = this.ctx.configId();
    const current = this.ctx.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
      return;
    }

    if (!current.complete || !current.consistent) {
      this.emitError(
        'CONFIG_NOT_READY',
        'Die Konfiguration ist nicht vollständig oder nicht konsistent'
      );
      return;
    }

    this.ctx.status.set('completing');
    this.ctx.errorMessage.set(null);

    this.api.completeConfiguration(currentConfigId).subscribe({
      next: response => {
        if (!response.complete || !response.consistent) {
          this.emitError(
            'CONFIG_CONFIRMATION_INVALID',
            'Die Konfiguration konnte nicht bestätigt werden, da sie unvollständig oder inkonsistent ist.'
          );
          return;
        }

        this.ctx.configuration.set(response);
        this.ctx.configId.set(response.configurationId);
        this.ctx.errorMessage.set(null);
        this.ctx.status.set('completed');

        const snapshot = this.buildSnapshot(response);
        this.ctx.configurationCompleted.emit(snapshot);
      },
      error: () =>
        this.emitError(
          'CONFIG_COMPLETE_FAILED',
          'Bestätigung der Konfiguration fehlgeschlagen'
        )
    });
  }

  addToCart(): void {
    const current = this.ctx.configuration();

    if (!current) {
      return;
    }

    if (this.ctx.status() !== 'completed') {
      this.emitError(
        'CONFIG_NOT_CONFIRMED',
        'Die Konfiguration muss bestätigt werden, bevor sie in den Warenkorb gelegt werden kann'
      );
      return;
    }

    const snapshot = this.buildSnapshot(current);

    const result: CompletedConfigurationResult = {
      configurationId: current.configurationId,
      productId: current.productId,
      kbId: current.kbId,
      addedToCart: true,
      receivedAt: new Date().toISOString(),
      snapshot,
      fullConfiguration: current
    };

    this.ctx.addedToCart.emit(result);
  }

  buildSnapshot(current: ConfigurationResponse): ConfigurationSnapshot {
    return {
      configurationId: current.configurationId,
      productId: current.productId,
      kbId: current.kbId,
      savedAt: new Date().toISOString(),
      complete: current.complete,
      consistent: current.consistent,
      rootItem: current.rootItem,
      groups: current.groups,
      messages: current.messages,
      metadata: {
        version: '1',
        sourceContext: this.ctx.widgetInputConfig.resume?.sourceContext ?? 'generic'
      }
    };
  }

  getGlobalMessages(): ConfigurationMessage[] {
    return (this.ctx.configuration()?.messages ?? []).filter(msg => !msg.characteristicId);
  }

  getMessagesForCharacteristic(characteristicId: string): ConfigurationMessage[] {
    return (this.ctx.configuration()?.messages ?? []).filter(
      msg => msg.characteristicId === characteristicId
    );
  }

  hasCharacteristicError(characteristicId: string): boolean {
    return this.getMessagesForCharacteristic(characteristicId).some(
      msg => msg.severity === 'ERROR'
    );
  }

  getEffectiveHints(characteristic: Characteristic): { text: string; severity: MessageSeverity }[] {
    const cpsMessages = this.getMessagesForCharacteristic(characteristic.id);
    if (cpsMessages.length) return cpsMessages;
    if (characteristic.required && !characteristic.complete) {
      return [{ text: 'Erforderliches Feld ist nicht ausgefüllt', severity: 'ERROR' }];
    }
    if (!characteristic.consistent) {
      return [{ text: 'Wert steht im Konflikt mit einer anderen Auswahl', severity: 'ERROR' }];
    }
    return [];
  }

  readonly errorCountByGroup = computed(() => {
    const issues = this.blockingIssues();
    const groups = this.ctx.configuration()?.groups ?? [];
    const map = new Map<string, number>();
    for (const g of groups) {
      const ids = (g as any).characteristicIDs ?? [];
      map.set(g.id, issues.filter(i => ids.includes(i.characteristicId)).length);
    }
    return map;
  });

  private applyConfiguration(
    response: ConfigurationResponse,
    emitStartedEvent = false
  ): void {
    this.ctx.configuration.set(response);
    this.ctx.configId.set(response.configurationId);
    this.ctx.status.set('loaded');
    this.ctx.errorMessage.set(null);

    if (emitStartedEvent) {
      this.ctx.configurationStarted.emit(response.configurationId);
    }
  }

  private applySnapshotFallback(snapshot: ConfigurationSnapshot): void {
    this.ctx.configuration.set({
      configurationId: snapshot.configurationId ?? 'snapshot-only',
      productId: snapshot.productId,
      kbId: snapshot.kbId,
      complete: snapshot.complete,
      consistent: snapshot.consistent,
      rootItem: snapshot.rootItem,
      groups: snapshot.groups ?? [],
      messages: snapshot.messages ?? [],
      restoreInfo: {
        mode: 'resume',
        status: 'FALLBACKAPPLIED',
        strategy: 'READONLYSNAPSHOT',
        liveSessionAvailable: false,
        snapshotUsed: true,
        readOnly: true,
        message:
          'Live-Konfiguration konnte nicht wiederhergestellt werden. Snapshot-Fallback wird im Nur-Lese-Modus angezeigt.'
      }
    });
    this.ctx.configId.set(snapshot.configurationId ?? 'snapshot-only');
    this.ctx.status.set('loaded');
    this.ctx.errorMessage.set(null);
  }

  private emitError(errorCode: string, message: string): void {
    this.ctx.status.set('error');
    this.ctx.errorMessage.set(message);
    this.ctx.errorOccurred.emit({ errorCode, message });
  }

  createFromExternalConfiguration(): void {
    const current = this.ctx.configuration();
    if (!current) {
      this.emitError('CONFIG_EXTERNAL_CREATE_INVALID', 'Keine Konfiguration zum Export vorhanden');
      return;
    }

    // Guard: только для readonly snapshot без живой runtime сессии
    const restoreInfo = current.restoreInfo;
    if (
      restoreInfo?.readOnly !== true ||
      restoreInfo?.liveSessionAvailable !== false
    ) {
      this.emitError(
        'CONFIG_EXTERNAL_CREATE_INVALID',
        'createFromExternalConfiguration ist nur für Readonly-Snapshots ohne Runtime-Sitzung erlaubt'
      );
      return;
    }

    const s = this.ctx.status();
    if (s === 'loading' || s === 'updating' || s === 'completing' || s === 'completed') {
      return;
    }

    const payload = this.buildExternalConfigurationPayload(current);

    this.ctx.status.set('loading');
    this.ctx.errorMessage.set(null);

    this.api.createFromExternalConfiguration(payload).subscribe({
      next: response => this.applyConfiguration(response, true),
      error: () =>
        this.emitError(
          'CONFIG_EXTERNAL_CREATE_FAILED',
          'Konfiguration aus externalConfiguration konnte nicht erstellt werden'
        )
    });
  }

  private buildExternalConfigurationPayload(
    current: ConfigurationResponse
  ): ExternalConfigurationPayload {
    return {
      productId: current.productId,
      kbId: current.kbId ?? undefined,
      rootItem: this.mapItemToExternalConfiguration(current.rootItem),
      metadata: {
        version: '1.0',
        sourceContext: this.ctx.widgetInputConfig.resume?.sourceContext ?? 'widget',
        configurationId: current.configurationId,
        savedAt: new Date().toISOString()
      }
    };
  }

  private mapItemToExternalConfiguration(
    item: ConfigurationItem
  ): ExternalConfigurationItemPayload {
    return {
      id: item.id,
      key: item.key,
      characteristics: (item.characteristics ?? [])
        .filter(char => (char.values?.length ?? 0) > 0)
        .map(char => ({
          id: char.id,
          values: (char.values ?? []).map(value => ({
            value: value.id
          }))
        })),
      subItems: (item.subItems ?? []).map(subItem =>
        this.mapItemToExternalConfiguration(subItem)
      )
    };
  }

  deleteCurrentConfiguration(): void {
    const currentConfigId = this.ctx.configId();

    if (!currentConfigId) {
      this.emitError('CONFIG_DELETE_INVALID', 'Keine aktive Konfiguration zum Löschen vorhanden');
      return;
    }

    this.ctx.status.set('updating');
    this.ctx.errorMessage.set(null);

    this.api.deleteConfiguration(currentConfigId).subscribe({
      next: () => {
        this.ctx.configuration.set(null);
        this.ctx.configId.set(null);
        this.ctx.status.set('idle');
        this.ctx.errorMessage.set(null);

        // Автоматически восстановить следующую конфигурацию
        this.resumeConfiguration();
      },
      error: () =>
        this.emitError('CONFIG_DELETE_FAILED', 'Die Konfiguration konnte nicht gelöscht werden')
    });
  }

  deleteMultipleConfigurations(configurationIds: string[]): void {
    if (!configurationIds || configurationIds.length === 0) {
      this.emitError('CONFIG_DELETE_LIST_INVALID', 'Liste der zu löschenden Konfigurationen ist leer');
      return;
    }

    this.ctx.status.set('updating');
    this.ctx.errorMessage.set(null);

    const payload: DeleteConfigurationsRequest = {
      configurationIds
    };

    this.api.deleteConfigurations(payload).subscribe({
      next: (response: DeleteConfigurationsResponse) => {
        const message = `${response.successfullyDeleted} von ${response.totalRequested} Konfigurationen gelöscht.` +
          (response.failedConfigurationIds.length > 0
            ? ` ${response.failedConfigurationIds.length} fehlgeschlagen.`
            : '');

        this.ctx.status.set('idle');
        this.ctx.errorMessage.set(null);

        // Очищаем текущую конфигурацию, если она была удалена
        const currentConfigId = this.ctx.configId();
        if (currentConfigId && configurationIds.includes(currentConfigId)) {
          this.ctx.configuration.set(null);
          this.ctx.configId.set(null);
        }

        this.ctx.errorOccurred.emit({
          errorCode: 'CONFIG_BULK_DELETE_SUCCESS',
          message
        });
      },
      error: () =>
        this.emitError(
          'CONFIG_BULK_DELETE_FAILED',
          'Batch-Löschung der Konfigurationen fehlgeschlagen'
        )
    });
  }
}
