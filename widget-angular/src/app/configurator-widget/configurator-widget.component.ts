import { Component, Input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigurationApiService } from '../services/configuration-api.service';

@Component({
  selector: 'app-configurator-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './configurator-widget.component.html',
  styleUrl: './configurator-widget.component.scss'
})

export class ConfiguratorWidgetComponent {
  @Input() productId!: string;
  @Input() kbId!: string;
  @Input() local: string = 'de';

  configurationStarted = output<string>();
  configurationCompleted = output<{ configId: string; summary: unknown }>();
  errorOccurred = output<{ errorCode: string; message: string }>();

  configId = signal<string | null>(null);
  status = signal<'idle' | 'loading' | 'started' | 'error' | 'updating' | 'completed'>('idle');
  errorMessage = signal<string | null>(null);
  selectedColor = signal<string>('RED');

  constructor(private configurationApi: ConfigurationApiService) {}

  startConfiguration(): void {
    this.status.set('loading');
    this.errorMessage.set(null);

    this.configurationApi.createConfiguration({
      productId: this.productId,
      kbId: this.kbId,
      local: this.local
    }).subscribe({
      next: (response) => {
        this.configId.set(response.configId);
        this.status.set('started');

        this.configurationStarted.emit(response.configId);
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

  updateColor(value: string): void {
    this.selectedColor.set(value);

    const currentConfigId = this.configId();
    if (!currentConfigId) {
      return;
    }

    this.status.set('updating');
    this.errorMessage.set(null);

    this.configurationApi.patchConfiguration(currentConfigId, {
      characteristic: 'color',
      value
    }).subscribe({
      next: () => {
        this.status.set('completed');
      },
      error: (error) => {
        this.status.set('error');
        this.errorMessage.set('Failed to update configuration');
        this.errorOccurred.emit({
          errorCode: 'CONFIG_PATCH_FAILED',
          message: 'Failed to update configuration'
        });
        console.error(error);
      }
    });
  }

  completeConfiguration(summary: unknown): void {
    const currentConfigId = this.configId();
    if (!currentConfigId) return;

    this.status.set('completed');
    this.configurationCompleted.emit({
      configId: currentConfigId,
      summary
    });
  }
}