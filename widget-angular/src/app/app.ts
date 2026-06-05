import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfiguratorWidgetComponent } from './configurator-widget/configurator-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  hostConfigId = signal<string | null>(null);
  hostStatus = signal<'idle' | 'running' | 'completed' | 'error'>('idle');
  hostError = signal<string | null>(null);

  onConfigurationStarted(configId: string): void {
    this.hostConfigId.set(configId);
    this.hostStatus.set('running');
    this.hostError.set(null);
  }

  onConfigurationCompleted(event: { configId: string; summary: unknown }): void {
    this.hostConfigId.set(event.configId);
    this.hostStatus.set('completed');
  }

  onError(event: { errorCode: string; message: string }): void {
    this.hostStatus.set('error');
    this.hostError.set(`${event.errorCode}: ${event.message}`);
  }
}