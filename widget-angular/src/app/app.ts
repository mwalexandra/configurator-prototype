import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  ConfiguratorWidgetComponent
 } from './configurator-widget/configurator-widget.component';
import { ConfigurationSnapshot } from './models/configuration.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  startedConfigId = signal<string | null>(null);
  completedSnapshot = signal<ConfigurationSnapshot | null>(null);
  lastError = signal<string | null>(null);

  onConfigurationStarted(configId: string): void {
    this.startedConfigId.set(configId);
  }

  onConfigurationCompleted(snapshot: ConfigurationSnapshot): void {
    this.completedSnapshot.set(snapshot);
  }

  onError(event: { errorCode: string; message: string }): void {
    this.lastError.set(`${event.errorCode}: ${event.message}`);
  }
}