// src/app/configurator-widget/configurator-widget.facade.ts
import { WritableSignal, computed } from '@angular/core';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  CompletedConfigurationResult,
  ConfigurationMessage,
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  ResumeConfigurationRequest,
  WidgetInputConfig,
  WidgetState
} from '../models/configuration.models';

export interface ConfiguratorWidgetFacadeContext {
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

  readonly incompleteRequiredSubItemCharacteristics = computed(() => {
    const config = this.ctx.configuration();
    const subItems = config?.rootItem?.subItems ?? [];

    return subItems.flatMap(item =>
      (item.characteristics ?? [])
        .filter(c => c.required && (!c.complete || !c.consistent))
        .map(c => ({
          itemId: item.id,
          itemKey: item.key,
          characteristicId: c.id,
          complete: c.complete,
          consistent: c.consistent,
          values: (c.values ?? []).map(v => v.id)
        }))
    );
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

  updateCharacteristic(characteristicId: string, value: string | null): void {
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
}
