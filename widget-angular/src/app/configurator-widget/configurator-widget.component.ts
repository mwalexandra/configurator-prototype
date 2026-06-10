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
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  ResumeConfigurationRequest,
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
      kbId: this.config.kbId,
      locale: this.config.locale
    };

    this.status.set('loading');
    this.errorMessage.set(null);

    this.configurationApi.createConfiguration(payload).subscribe({
      next: (response) => {
        this.applyConfiguration(response);
        this.configurationStarted.emit(response.configurationId);
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

    const payload: ResumeConfigurationRequest = {
      configurationId: resume.configurationId,
      snapshot: resume.snapshot,
      sourceContext: resume.sourceContext
    };

    this.status.set('loading');
    this.errorMessage.set(null);

    // phase 1: if backend resume endpoint is not ready yet,
    // prefer direct load by configurationId
    if (resume.configurationId && !resume.snapshot) {
      this.configurationApi.getConfiguration(resume.configurationId).subscribe({
        next: (response) => {
          this.applyConfiguration(response);
        },
        error: () => {
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

    this.configurationApi.resumeConfiguration(payload).subscribe({
      next: (response) => {
        this.applyConfiguration(response);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Failed to resume configuration');
        this.errorOccurred.emit({
          errorCode: 'CONFIG_RESUME_FAILED',
          message: 'Failed to resume configuration'
        });
      }
    });
  }

  updateCharacteristic(characteristicId: string, value: string | null): void {
    const currentConfigId = this.configId();
    if (!currentConfigId) {
      return;
    }

    this.status.set('updating');
    this.errorMessage.set(null);

    this.configurationApi.patchConfiguration(currentConfigId, {
      configurationId: currentConfigId,
      characteristicId,
      value
    }).subscribe({
      next: (response) => {
        this.applyConfiguration(response, false);
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
    const current = this.configuration();
    if (!current) {
      return;
    }

    const snapshot: ConfigurationSnapshot = {
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
        sourceContext: this.config.resume?.sourceContext ?? 'generic',
        locale: this.config.locale,
        version: '1'
      }
    };

    this.status.set('completed');
    this.configurationCompleted.emit(snapshot);
  }

  getSingleSelectedValueId(char: Characteristic): string {
    return char.values?.[0]?.id ?? '';
  }

  trackByCharacteristicId(_: number, char: Characteristic): string {
    return char.id;
  }

  private applyConfiguration(
    response: ConfigurationResponse,
    emitStartedEvent = false
  ): void {
    this.configuration.set(response);
    this.configId.set(response.configurationId);
    this.status.set('loaded');

    if (emitStartedEvent) {
      this.configurationStarted.emit(response.configurationId);
    }
  }
}