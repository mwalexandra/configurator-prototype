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
  ConfigurationMessage,
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  WidgetInputConfig,
  WidgetState
} from '../models/configuration.models';

@Component({
  selector: 'app-configurator-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configurator-widget.component.html',
  styleUrl: './configurator-widget.component.scss'
})
export class ConfiguratorWidgetComponent implements OnInit {
  @Input({ required: true }) config!: WidgetInputConfig;

  configurationStarted = output<string>();
  configurationCompleted = output<ConfigurationSnapshot>();
  addedToCart = output<ConfigurationSnapshot>();
  errorOccurred = output<{ errorCode: string; message: string }>();

  configuration = signal<ConfigurationResponse | null>(null);
  configId = signal<string | null>(null);
  status = signal<WidgetState>('idle');
  errorMessage = signal<string | null>(null);

  visibleCharacteristics = computed(() =>
    this.configuration()?.rootItem?.characteristics?.filter(c => c.visible) ?? []
  );

  constructor(private configurationApi: ConfigurationApiService) {}

  ngOnInit(): void {
    this.configurationApi.setApiBaseUrl(this.config.apiBaseUrl);

    if (this.config.mode === 'resume') {
      this.resumeConfiguration();
    }
  }

  startConfiguration(): void {
    if (this.config.mode !== 'create') {
      return;
    }

    if (!this.config.productId || !this.config.kbId) {
      this.status.set('error');
      this.errorMessage.set('Missing productId or kbId for create mode');
      this.errorOccurred.emit({
        errorCode: 'CONFIG_INPUT_INVALID',
        message: 'Missing productId or kbId for create mode'
      });
      return;
    }

    const payload: CreateConfigurationRequest = {
      productId: this.config.productId,
      kbId: this.config.kbId
    };

    this.status.set('loading');
    this.errorMessage.set(null);

    this.configurationApi.createConfiguration(payload).subscribe({
      next: response => {
        this.applyConfiguration(response, true);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Failed to start configuration');
        this.errorOccurred.emit({
          errorCode: 'CONFIG_START_FAILED',
          message: 'Failed to start configuration'
        });
      }
    });
  }

  resumeConfiguration(): void {
    const resume = this.config.resume;

    if (!resume || (!resume.configurationId && !resume.snapshot)) {
      this.status.set('error');
      this.errorMessage.set('Resume mode requires configurationId or snapshot');
      this.errorOccurred.emit({
        errorCode: 'CONFIG_RESUME_INPUT_INVALID',
        message: 'Resume mode requires configurationId or snapshot'
      });
      return;
    }

    this.status.set('loading');
    this.errorMessage.set(null);

    if (resume.configurationId) {
      this.configurationApi.getConfiguration(resume.configurationId).subscribe({
        next: response => {
          this.applyConfiguration(response);
        },
        error: () => {
          if (resume.snapshot) {
            this.applySnapshotFallback(resume.snapshot);
            return;
          }

          this.status.set('error');
          this.errorMessage.set('Failed to load configuration');
          this.errorOccurred.emit({
            errorCode: 'CONFIG_LOAD_FAILED',
            message: 'Failed to load configuration'
          });
        }
      });
      return;
    }

    if (resume.snapshot) {
      this.applySnapshotFallback(resume.snapshot);
    }
  }

  updateCharacteristic(characteristicId: string, value: string | null): void {
    const currentConfigId = this.configId();
    const current = this.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
      return;
    }

    this.status.set('updating');
    this.errorMessage.set(null);

    this.configurationApi.patchConfiguration(currentConfigId, {
      configurationId: currentConfigId,
      characteristicId,
      value
    }).subscribe({
      next: response => {
        this.applyConfiguration(response);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Failed to update configuration');
        this.errorOccurred.emit({
          errorCode: 'CONFIG_PATCH_FAILED',
          message: 'Failed to update configuration'
        });
      }
    });
  }

  completeConfiguration(): void {
    const currentConfigId = this.configId();
    const current = this.configuration();

    if (!currentConfigId || !current || current.restoreInfo?.readOnly) {
      return;
    }

    this.status.set('completing');
    this.errorMessage.set(null);

    this.configurationApi.completeConfiguration(currentConfigId).subscribe({
      next: response => {
        this.configuration.set(response);
        this.configId.set(response.configurationId);
        this.status.set('completed');

        const snapshot = this.buildSnapshot(response);
        this.configurationCompleted.emit(snapshot);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Failed to complete configuration');
        this.errorOccurred.emit({
          errorCode: 'CONFIG_COMPLETE_FAILED',
          message: 'Failed to complete configuration'
        });
      }
    });
  }

  addToCart(): void {
    const current = this.configuration();
    if (!current) return;

    this.addedToCart.emit(this.buildSnapshot(current));
  }

  getSingleSelectedValueId(char: Characteristic): string {
    return char.values?.[0]?.id ?? '';
  }

  trackByCharacteristicId(_: number, char: Characteristic): string {
    return char.id;
  }

  private applyConfiguration(response: ConfigurationResponse, emitStartedEvent = false): void {
    this.configuration.set(response);
    this.configId.set(response.configurationId);
    this.status.set('loaded');

    if (emitStartedEvent) {
      this.configurationStarted.emit(response.configurationId);
    }
  }

  private applySnapshotFallback(snapshot: ConfigurationSnapshot): void {
    this.configuration.set({
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

    this.configId.set(snapshot.configurationId ?? null);
    this.status.set('loaded');
    this.errorMessage.set(null);
  }

  private buildSnapshot(current: ConfigurationResponse): ConfigurationSnapshot {
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
        sourceContext: this.config.resume?.sourceContext ?? 'generic',
      }
    };
  }

  getGlobalMessages(): ConfigurationMessage[] {
    return (this.configuration()?.messages ?? []).filter(msg => !msg.characteristicId);
  }

  getMessagesForCharacteristic(characteristicId: string): ConfigurationMessage[] {
    return (this.configuration()?.messages ?? []).filter(
      msg => msg.characteristicId === characteristicId
    );
  }

  hasCharacteristicError(characteristicId: string): boolean {
    return this.getMessagesForCharacteristic(characteristicId).some(
      msg => msg.severity === 'ERROR'
    );
  }
}