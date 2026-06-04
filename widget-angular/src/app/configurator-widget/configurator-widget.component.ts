import { Component, Input, signal } from '@angular/core';
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
  @Input() locale: string = 'de';

  configId = signal<string | null>(null);
  status = signal<'idle' | 'loading' | 'started' | 'error'>('idle');
  errorMessage = signal<string | null>(null);

  constructor(private configurationApi: ConfigurationApiService) {}

  startConfiguration(): void {
    this.status.set('loading');
    this.errorMessage.set(null);

    this.configurationApi.createConfiguration({
      productId: this.productId,
      kbId: this.kbId,
      locale: this.locale
    }).subscribe({
      next: (response) => {
        this.configId.set(response.configId);
        this.status.set('started');

        console.log('onConfigurationStarted', response.configId);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Failed to start configuration');
        console.error(this.errorMessage);
      }
    });
  }
}