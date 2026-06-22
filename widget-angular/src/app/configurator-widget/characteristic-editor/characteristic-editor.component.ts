import { Component, Input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
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
    @Input({ required: true }) characteristic!: Characteristic;
    @Input() itemId?: string;
    @Input() readOnly = false;
    @Input() completed = false;
    @Input() messages: ConfigurationMessage[] = [];
    @Input() hasProblem = false;
    @Input() incomplete = false;

    valueChanged = output<{
    itemId?: string;
    characteristicId: string;
    value: string | null;
    }>();

    protected getSingleSelectedValueId(): string {
        return this.characteristic?.values?.[0]?.id ?? '';
    }

    protected emitValue(value: string | null): void {
        this.valueChanged.emit({
        itemId: this.itemId,
        characteristicId: this.characteristic.id,
        value
        });
    }
}