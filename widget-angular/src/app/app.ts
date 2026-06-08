import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  ConfiguratorWidgetComponent,
  ConfigurationSummary
 } from './configurator-widget/configurator-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  hostConfigId = signal<string | null>(null);
  hostStatus = signal<'idle' | 'started' | 'completed' | 'error'>('idle');
  hostError = signal<string | null>(null);
  completionSummary = signal<ConfigurationSummary | null>(null);

  onConfigurationStarted(configId: string): void {
    this.hostConfigId.set(configId);
    this.hostStatus.set('started');
    this.hostError.set(null);
  }

  onConfigurationCompleted(summary: ConfigurationSummary ): void {
    this.hostConfigId.set(summary.configId);
    this.hostStatus.set('completed');
    this.completionSummary.set(summary);
    this.hostError.set(null);
  }

  onError(event: { errorCode: string; message: string }): void {
    this.hostStatus.set('error');
    this.hostError.set(`${event.errorCode}: ${event.message}`);
  }
}