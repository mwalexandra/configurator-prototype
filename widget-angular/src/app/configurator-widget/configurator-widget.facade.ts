// src/app/configurator-widget/configurator-widget.facade.ts
import { WritableSignal } from '@angular/core';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  ConfigurationMessage,
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  WidgetInputConfig,
  WidgetState
} from '../models/configuration.models';

export interface ConfiguratorWidgetFacadeContext {
  config: WidgetInputConfig;
  configuration: WritableSignal<ConfigurationResponse | null>;
  configId: WritableSignal<string | null>;
  status: WritableSignal<WidgetState>;
  errorMessage: WritableSignal<string | null>;
  configurationStarted: { emit(value: string): void };
  configurationCompleted: { emit(value: ConfigurationSnapshot): void };
  addedToCart: { emit(value: ConfigurationSnapshot): void };
  errorOccurred: { emit(value: { errorCode: string; message: string }): void };
}

export class ConfiguratorWidgetFacade {
  constructor(
    private readonly api: ConfigurationApiService,
    private readonly ctx: ConfiguratorWidgetFacadeContext
  ) {}

  initialize(): void {
    this.api.setApiBaseUrl(this.ctx.config.apiBaseUrl);

    if (this.ctx.config.mode === 'resume') {
      this.resumeConfiguration();
    }
  }

  startConfiguration(): void {
    if (this.ctx.config.mode !== 'create') {
      return;
    }

    if (!this.ctx.config.productId || !this.ctx.config.kbId) {
      this.emitError(
        'CONFIG_INPUT_INVALID',
        'Missing productId or kbId for create mode'
      );
      return;
    }

    const payload: CreateConfigurationRequest = {
      productId: this.ctx.config.productId,
      kbId: this.ctx.config.kbId
    };

    this.ctx.status.set('loading');
    this.ctx.errorMessage.set(null);

    this.api.createConfiguration(payload).subscribe({
      next: response => this.applyConfiguration(response, true),
      error: () => this.emitError('CONFIG_START_FAILED', 'Failed to start configuration')
    });
  }

  resumeConfiguration(): void {
    const resume = this.ctx.config.resume;

    if (!resume || (!resume.configurationId && !resume.snapshot)) {
      this.emitError(
        'CONFIG_RESUME_INPUT_INVALID',
        'Resume mode requires configurationId or snapshot'
      );
      return;
    }

    this.ctx.status.set('loading');
    this.ctx.errorMessage.set(null);

    if (resume.configurationId) {
      this.api.getConfiguration(resume.configurationId).subscribe({
        next: response => this.applyConfiguration(response),
        error: () => {
          if (resume.snapshot) {
            this.applySnapshotFallback(resume.snapshot);
            return;
          }

          this.emitError('CONFIG_LOAD_FAILED', 'Failed to load configuration');
        }
      });
      return;
    }

    if (resume.snapshot) {
      this.applySnapshotFallback(resume.snapshot);
    }
  }

  updateCharacteristic(characteristicId: string, value: string | null): void {
    const currentConfigId = this.ctx.configId();
    const current = this.ctx.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
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
      error: () => this.emitError('CONFIG_PATCH_FAILED', 'Failed to update configuration')
    });
  }

  completeConfiguration(): void {
    const currentConfigId = this.ctx.configId();
    const current = this.ctx.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
      return;
    }

    this.ctx.status.set('completing');
    this.ctx.errorMessage.set(null);

    this.api.completeConfiguration(currentConfigId).subscribe({
      next: response => {
        this.applyConfiguration(response);
        this.ctx.status.set('completed');

        const snapshot = this.buildSnapshot(response);
        this.ctx.configurationCompleted.emit(snapshot);
      },
      error: () => this.emitError('CONFIG_COMPLETE_FAILED', 'Failed to complete configuration')
    });
  }

  addToCart(): void {
    const current = this.ctx.configuration();
    if (!current) return;

    this.ctx.addedToCart.emit(this.buildSnapshot(current));
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
        sourceContext: this.ctx.config.resume?.sourceContext ?? 'generic'
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
        message: 'Live configuration could not be restored. Snapshot fallback is shown in read-only mode.'
      }
    });

    this.ctx.configId.set(snapshot.configurationId ?? null);
    this.ctx.status.set('loaded');
    this.ctx.errorMessage.set(null);
  }

  private emitError(errorCode: string, message: string): void {
    this.ctx.status.set('error');
    this.ctx.errorMessage.set(message);
    this.ctx.errorOccurred.emit({ errorCode, message });
  }
}