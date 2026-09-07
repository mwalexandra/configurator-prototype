// src/app/configurator-widget/configurator-widget.component.ts
import {
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  computed,
  output,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigurationApiService } from '../services/configuration-api.service';
import {
  Characteristic,
  CompletedConfigurationResult,
  ConfigurationResponse,
  ConfigurationMessage,
  ConfigurationSnapshot,
  WidgetInputConfig,
  WidgetState
} from '../models/configuration.models';
import { ConfiguratorWidgetFacade } from './configurator-widget.facade';
import { createConfiguratorWidgetUiState } from './configurator-widget.ui-state';
import { CharacteristicEditorComponent } from './characteristic-editor/characteristic-editor.component';

interface ConfiguratorSection {
  title: string;
  itemKey: string;
}

interface MeasurementSection {
  title: string;
  key: 'arm-measurements' | 'hand-measurements' | 'production-information';
}

@Component({
  selector: 'app-configurator-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, CharacteristicEditorComponent],
  templateUrl: './configurator-widget.component.html',
  styleUrl: './configurator-widget.component.scss'
})
export class ConfiguratorWidgetComponent implements OnInit, OnChanges {
  @Input({ required: true }) widgetInputConfig!: WidgetInputConfig;

  configurationStarted = output<string>();
  configurationCompleted = output<ConfigurationSnapshot>();
  addedToCart = output<CompletedConfigurationResult>();
  errorOccurred = output<{ errorCode: string; message: string }>();

  configuration = signal<ConfigurationResponse | null>(null);
  configId = signal<string | null>(null);
  status = signal<WidgetState>('idle');
  errorMessage = signal<string | null>(null);

  // Define the sections of the configurator with their corresponding item keys
  protected readonly configuratorSections: ConfiguratorSection[] = [
    { title: 'PRODUKT', itemKey: '000020000009900002' },
    { title: 'ARM', itemKey: '000020000009900021' },
    { title: 'HAND', itemKey: '000020000009900022' }
  ];

  protected readonly leftPanelSections: MeasurementSection[] = [
    { 
      title: 'ARMMASSE', 
      key: 'arm-measurements' 
    },
    {
      title: 'HANDMASSE',
      key: 'hand-measurements'
    },
    {
      title: 'INFORMATIONEN FÜR PRODUKTION',
      key: 'production-information'
    }
  ];

  private static readonly PRODUKT_WHITELIST: string[] = [
    'PH_AL_VP_LIEFERPRIO', 'PH_AL_VP_PRODUKT', 'PH_AL_VP_STEUERAUSFUEHRUNG',
    'PH_AL_VP_AUSFARMHAND', 'PH_AL_VP_AUSFARMSTRUMPF', 'PH_AL_VP_AUSFHANDSCHUH',
    'PH_AL_VP_FARBE', 'PH_AL_VP_SEITE', 'PH_AS_VP_CCL'
  ];

  private static readonly ARM_WHITELIST: string[] = [
    'PH_AS_VP_ARTABSCHL', 'PH_AS_VP_BREITEPORABSCHL', 'PH_AS_VP_KAPPE',
    'PH_AS_VP_GURT', 'PH_AS_VP_BHBEFESTIGUNG', 'PH_AS_VP_FORMABSCHL',
    'PH_AS_VP_SCHRAEGE', 'PH_AS_VP_SCHRAEGEABSCHL', 'PH_AS_VP_ARTANFANG',
    'PH_AS_VP_BREITEPORANFANG', 'PH_AS_VP_HBSTK', 'PH_AS_VP_WINKEL',
    'PH_AS_VP_ELLENBOGENKOMFORT'
  ];

  private static readonly HAND_WHITELIST: string[] = [
    'PH_HS_VP_ARTABSCHL', 'PH_HS_VP_BREITEPORABSCHL', 'PH_HS_VP_FINGER',
    'PH_HS_VP_DAUMEN', 'PH_HS_VP_ANATOMISCHHAND', 'PH_HS_VP_BEFESTIGUNG',
    'PH_HS_VP_FINGERSTRUMPF'
  ];

  protected readonly expandedSectionTitles = signal<Set<string>>(
    new Set(['PRODUKT'])
  );

  protected readonly visibleCharacteristics = computed(() =>
    (this.configuration()?.rootItem?.characteristics ?? []).filter(
      char => char.visible
    )
  );

  private facade: ConfiguratorWidgetFacade | null = null;

  protected readonly ui = createConfiguratorWidgetUiState(
    this.configuration,
    this.status,
    this.errorMessage
  );

  constructor(private configurationApi: ConfigurationApiService) {}

  ngOnInit(): void {
    this.facade = new ConfiguratorWidgetFacade(this.configurationApi, {
      widgetInputConfig: this.widgetInputConfig,
      configuration: this.configuration,
      configId: this.configId,
      status: this.status,
      errorMessage: this.errorMessage,
      configurationStarted: this.configurationStarted,
      configurationCompleted: this.configurationCompleted,
      addedToCart: this.addedToCart,
      errorOccurred: this.errorOccurred
    });

    this.facade.initialize();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['widgetInputConfig'] && this.facade) {
      this.facade = new ConfiguratorWidgetFacade(this.configurationApi, {
        widgetInputConfig: this.widgetInputConfig,
        configuration: this.configuration,
        configId: this.configId,
        status: this.status,
        errorMessage: this.errorMessage,
        configurationStarted: this.configurationStarted,
        configurationCompleted: this.configurationCompleted,
        addedToCart: this.addedToCart,
        errorOccurred: this.errorOccurred
      });

      this.facade.initialize();
    }
  }

  // TODO to remove 
  protected logRootItem(): void {
    console.log('configuration:', this.configuration());
    console.log('rootItem:', this.configuration()?.rootItem);
    console.log('rootItem.characteristics:', this.configuration()?.rootItem?.characteristics);
  }

  // TODO to remove
  protected logWhitelistedSections(): void {
    console.log('configuration loaded?', !!this.configuration());
    console.log('PH_AL_VP_STEUERAUSFUEHRUNG raw:', this.findCharacteristicById('PH_AL_VP_STEUERAUSFUEHRUNG'));
    console.log('PH_AL_VP_SEITE raw:', this.findCharacteristicById('PH_AL_VP_SEITE'));

    for (const section of this.configuratorSections) {
      const chars = this.getWhitelistedSectionCharacteristics(section);
      console.log(
        `[${section.title}] visible whitelisted count:`,
        chars.length,
        chars.map(c => ({ id: c.id, name: c.name, readOnly: c.readOnly, visible: c.visible }))
      );
    }
  }

  // TODO to remove
  protected logMessagesDebug(): void {
    console.log('all messages:', this.configuration()?.messages);
    console.log('blockingIssues:', this.facade?.blockingIssues());
    console.log('blockingIssueLabels:', this.facade?.blockingIssueLabels());
  }

  protected get blockingIssuesForView() {
    return this.facade?.blockingIssueLabels?.() ?? [];
  }

  scrollToField(itemId: string, characteristicId: string): void {
    const el = document.getElementById(`char-${itemId}-${characteristicId}`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    (el?.querySelector('input, select, textarea') as HTMLElement | null)?.focus();
  }

  protected trackBlockingIssue(index: number, issue: { itemId: string; characteristicId: string }) {
    return `${issue.itemId}-${issue.characteristicId}`;
  }

  protected get subItemsForView() {
    const subItems = this.configuration()?.rootItem?.subItems ?? [];
    const blockingItemIds = new Set(
      this.facade?.blockingIssues()
        .filter(issue => issue.level === 'subItem')
        .map(issue => issue.itemId) ?? []
    );

    return subItems.filter(subItem => blockingItemIds.has(subItem.id));
  }

  protected getItemByKey(itemKey: string) {
    const rootItem = this.configuration()?.rootItem;

    if (!rootItem) {
      return null;
    }

    if (rootItem.key === itemKey) {
      return rootItem;
    }

    return (rootItem.subItems ?? []).find(item => item.key === itemKey) ?? null;
  }

  protected findCharacteristicById(id: string): Characteristic | undefined {
    const rootItem = this.configuration()?.rootItem;
    if (!rootItem) {
      return undefined;
    }

    const inRoot = rootItem.characteristics.find(c => c.id === id);
    if (inRoot) {
      return inRoot;
    }

    for (const subItem of rootItem.subItems ?? []) {
      const found = subItem.characteristics.find(c => c.id === id);
      if (found) {
        return found;
      }
    }

    return undefined;
  }

  protected getItemIdForCharacteristicId(id: string): string | undefined {
    const rootItem = this.configuration()?.rootItem;
    if (!rootItem) {
      return undefined;
    }

    if (rootItem.characteristics.some(c => c.id === id)) {
      return rootItem.id;
    }

    for (const subItem of rootItem.subItems ?? []) {
      if (subItem.characteristics.some(c => c.id === id)) {
        return subItem.id;
      }
    }

    return undefined;
  }

  protected getGroupCharacteristic(
    groupCharacteristicId: string
  ): Characteristic | undefined {
    return this.configuration()?.rootItem?.characteristics
      .find((characteristic) => characteristic.id === groupCharacteristicId);
  }

  protected getLeftPanelCharacteristics(
    section: MeasurementSection
  ): Characteristic[] {
    if (section.key === 'arm-measurements') {
      const armItem = this.getItemByKey('000020000009900021');
      const armCharacteristics = armItem?.characteristics ?? [];

      return armCharacteristics.filter((characteristic) =>
        characteristic.id.startsWith('PH_AS_FM_') &&
        !characteristic.readOnly &&
        characteristic.visible
      );
    }

    if (section.key === 'hand-measurements') {
      const handItem = this.getItemByKey('000020000009900022');
      const handCharacteristics = handItem?.characteristics ?? [];

      return handCharacteristics.filter((characteristic) =>
        characteristic.id.startsWith('PH_HS_FM_') &&
        !characteristic.readOnly &&
        characteristic.visible
      );
    }

    if (section.key === 'production-information') {
      const rootCharacteristics =
        this.configuration()?.rootItem?.characteristics ?? [];

      const productionCharacteristicIds = [
        'PH_AL_FS_INFOPROD',
        'PH_AL_FT_INFOPROD'
      ];

      const characteristicsById = new Map(
        rootCharacteristics.map((characteristic) => [
          characteristic.id,
          characteristic
        ])
      );

      return productionCharacteristicIds
        .map((id) => characteristicsById.get(id))
        .filter(
          (characteristic): characteristic is Characteristic =>
            characteristic !== undefined
        );
    }

    return [];
  }

  protected getSectionCharacteristics(itemKey: string): Characteristic[] {
    return this.getItemByKey(itemKey)?.characteristics ?? [];
  }

  protected getWhitelistedSectionCharacteristics(
    section: ConfiguratorSection
  ): Characteristic[] {
    const whitelist = this.getWhitelistForSection(section.itemKey);

    return whitelist
      .map(id => this.findCharacteristicById(id))
      .filter((characteristic): characteristic is Characteristic =>
        characteristic !== undefined && characteristic.visible
      );
  }

  private getWhitelistForSection(itemKey: string): string[] {
    switch (itemKey) {
      case '000020000009900002':
        return ConfiguratorWidgetComponent.PRODUKT_WHITELIST;
      case '000020000009900021':
        return ConfiguratorWidgetComponent.ARM_WHITELIST;
      case '000020000009900022':
        return ConfiguratorWidgetComponent.HAND_WHITELIST;
      default:
        return [];
    }
  }

  protected getSectionItemId(itemKey: string): string | undefined {
    return this.getItemByKey(itemKey)?.id;
  }

  protected toggleSection(title: string): void {
    this.expandedSectionTitles.update(current => {
      const next = new Set(current);

      if (next.has(title)) {
        next.delete(title);
      } else {
        next.add(title);
      }

      return next;
    });
  }

  protected isSectionExpanded(title: string): boolean {
    return this.expandedSectionTitles().has(title);
  }

  public startConfiguration(): void {
    this.facade?.startConfiguration();
  }

  public resumeConfiguration(): void {
    this.facade?.resumeConfiguration();
  }

  createFromExternalConfiguration(): void {
    this.facade?.createFromExternalConfiguration();
  }

  public updateCharacteristic(
    characteristicId: string,
    value: string | null,
    itemId?: string
  ): void {
    this.facade?.updateCharacteristic(characteristicId, value, itemId);
  }

  public completeConfiguration(): void {
    this.facade?.completeConfiguration();
  }

  protected addToCart(): void {
    this.facade?.addToCart();
  }

  protected deleteConfiguration(): void {
    this.facade?.deleteCurrentConfiguration();
  }

  protected deleteConfigurationIds(configurationIds: string[]): void {
    this.facade?.deleteMultipleConfigurations(configurationIds);
  }

  protected getSingleSelectedValueId(char: Characteristic): string {
    return char.values?.[0]?.id ?? '';
  }

  protected trackByCharacteristicId(_: number, char: Characteristic): string {
    return char.id;
  }

  protected getGlobalMessages(): ConfigurationMessage[] {
    return this.facade?.getGlobalMessages() ?? [];
  }

  protected getMessagesForCharacteristic(characteristicId: string): ConfigurationMessage[] {
    return this.facade?.getMessagesForCharacteristic(characteristicId) ?? [];
  }

  // not used
  protected hasCharacteristicProblem(characteristicId: string): boolean {
    return this.ui.problemCharacteristicIds().has(characteristicId);
  }

  protected isCharacteristicIncomplete(char: Characteristic, itemId?: string): boolean {
    return this.facade?.blockingIssues().some(issue =>
      issue.characteristicId === char.id &&
      issue.itemId === itemId &&
      issue.complete === false
    ) ?? false;
  }

  protected hasCharacteristicConflict(char: Characteristic, itemId?: string): boolean {
    const issues = this.facade?.blockingIssues() ?? [];
    const match = issues.find(issue =>
      issue.characteristicId === char.id &&
      issue.itemId === itemId &&
      issue.complete === true &&
      issue.consistent === false
    );
    if (match) {
      console.log('conflict on', char.id, 'itemId', itemId, '-> messages for this char:', this.getMessagesForCharacteristic(char.id));
    }
    return !!match;
  }

  protected translateMode(mode: string | undefined): string {
    switch (mode) {
      case 'create':
        return 'Erstellen';
      case 'resume':
        return 'Fortsetzen';
      default:
        return mode ?? 'Unbekannt';
    }
  }

  protected translateStatus(status: string | null): string {
    switch (status) {
      case 'idle':
        return 'Inaktiv';
      case 'loading':
        return 'Lädt';
      case 'updating':
        return 'Aktualisiert';
      case 'completing':
        return 'Speichert';
      case 'completed':
        return 'Abgeschlossen';
      case 'error':
        return 'Fehler';
      default:
        return status ?? 'Unbekannt';
    }
  }

  protected translateUiState(uiState: string | null): string {
    switch (uiState) {
      case 'idle':
        return 'Inaktiv';
      case 'loading':
        return 'Lädt';
      case 'incomplete':
        return 'Unvollständig';
      case 'conflict':
        return 'Konflikt';
      case 'ready':
        return 'Bereit';
      case 'completed':
        return 'Abgeschlossen';
      case 'readonly':
        return 'Nur-Lese';
      case 'error':
        return 'Fehler';
      default:
        return uiState ?? 'Unbekannt';
    }
  }

  protected isCharacteristicReadOnly(char: Characteristic): boolean {
    return !!char.readOnly
      || this.ui.isReadOnly()
      || this.status() === 'updating'
      || this.status() === 'completing'
      || this.status() === 'completed';
  }

  protected isSubItemCharacteristicReadOnly(char: Characteristic): boolean {
    return !!char.readOnly
      || this.ui.isReadOnly()
      || this.status() === 'updating'
      || this.status() === 'completing'
      || this.status() === 'completed';
  }

  protected getRootItemId(): string | undefined {
    return this.configuration()?.rootItem?.id;
  }

  protected isRootCharacteristicConfirmed(char: Characteristic): boolean {
    return this.status() === 'completed' && !!char.complete && !!char.consistent;
  }

  protected getRootCharacteristicMessages(char: Characteristic): ConfigurationMessage[] {
    return this.getMessagesForCharacteristic(char.id);
  }

  protected firstVisibleCharacteristic(): Characteristic | null {
    return this.visibleCharacteristics()[0] ?? null;
  }

  // TODO: This is a temporary solution to get the itemId for the left panel characteristics. In the future, we should refactor the code to avoid this hardcoded mapping.
  protected getLeftPanelItemId(
    section: MeasurementSection
  ): string | undefined {
    if (section.key === 'arm-measurements') {
      return this.getSectionItemId('000020000009900021');
    }

    if (section.key === 'hand-measurements') {
      return this.getSectionItemId('000020000009900022');
    }

    return this.getRootItemId();
  }

}
