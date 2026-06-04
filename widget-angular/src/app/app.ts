import { Component } from '@angular/core';
import { ConfiguratorWidgetComponent } from './configurator-widget/configurator-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ConfiguratorWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}