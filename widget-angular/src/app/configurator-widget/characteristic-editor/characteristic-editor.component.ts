import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  Characteristic,
  ConfigurationMessage
} from '../../models/configuration.models';

@Component({
  selector: 'app-characteristic-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './characteristic-editor.component.html',
  styleUrl: './characteristic-editor.component.scss'
})
export class CharacteristicEditorComponent {
    characteristic = input.required<Characteristic>();
    itemId = input<string | undefined>();
    readOnly = input(false);
    completed = input(false);
    messages = input<ConfigurationMessage[]>([]);
    hasProblem = input(false);
    incomplete = input(false);
    consistent = input(true);

    valueChanged = output<{
        itemId?: string;
        characteristicId: string;
        value: string | null;
    }>();

    protected getSingleSelectedValueId(): string {
        return this.characteristic().values?.[0]?.id ?? '';
    }

    protected emitValue(value: string | null): void {
        this.valueChanged.emit({
        itemId: this.itemId(),
        characteristicId: this.characteristic().id,
        value
        });
    }
}