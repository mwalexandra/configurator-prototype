import { Component, Input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  Characteristic,
  ConfigurationResponse,
  ConfigurationSnapshot,
  CreateConfigurationRequest,
  UpdateCharacteristicRequest,
  WidgetState
} from '../models/configuration.models';

@Component({
  selector: 'app-configurator-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configurator-widget.component.html',
  styleUrl: './configurator-widget.component.scss'
})
export class ConfiguratorWidgetComponent {
  @Input() productId!: string;
  @Input() kbId!: string;
  @Input() locale: string = 'de';

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

  startConfiguration(): void {
    const payload: CreateConfigurationRequest = {
      productId: this.productId,
      kbId: this.kbId,
      locale: this.locale
    };

    this.status.set('loading');
    this.errorMessage.set(null);

    this.configurationApi.createConfiguration(payload).subscribe({
      next: (response) => {
        this.configuration.set(response);
        this.configId.set(response.configurationId);
        this.status.set('loaded');
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

  updateCharacteristic(characteristicId: string, value: string | null): void {
    const currentConfigId = this.configId();
    if (!currentConfigId) {
      return;
    }

    const payload: UpdateCharacteristicRequest = {
      configurationId: currentConfigId,
      characteristicId,
      value
    };

    this.status.set('updating');
    this.errorMessage.set(null);

    this.configurationApi.patchConfiguration(currentConfigId, payload).subscribe({
      next: (response) => {
        this.configuration.set(response);
        this.status.set('loaded');
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
      sourceContext: 'generic'
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
}