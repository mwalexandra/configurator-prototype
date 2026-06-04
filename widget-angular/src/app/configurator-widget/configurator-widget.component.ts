import { Component, Input } from '@angular/core';
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

  configId: string | null = null;
  status: 'idle' | 'loading' | 'started' | 'error' = 'idle';
  errorMessage: string | null = null;

  constructor(private configurationApi: ConfigurationApiService) {}

  startConfiguration(): void {
    this.status = 'loading';
    this.errorMessage = null;

    this.configurationApi.createConfiguration({
      productId: this.productId,
      kbId: this.kbId,
      locale: this.locale
    }).subscribe({
      next: (response) => {
        this.configId = response.configId;
        this.status = 'started';

        console.log('onConfigurationStarted', response.configId);
      },
      error: () => {
        this.status = 'error';
        this.errorMessage = 'Failed to start configuration';
      }
    });
  }
}