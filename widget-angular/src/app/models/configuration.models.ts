// widget-angular/src/app/models/configuration.models.ts

export type ConfiguratorMode = 'create' | 'resume';

export type WidgetState =
  | 'idle'
  | 'loading'
  | 'loaded'
  | 'updating'
  | 'error'
  | 'completing'
  | 'completed';

// ──────────────────────────────
// Widget Input (Host → Widget)
// ──────────────────────────────
export interface WidgetInputConfig {
  apiBaseUrl: string;
  mode: ConfiguratorMode;
  // create mode
  productId?: string;
  kbId?: string;
  locale?: string;
  // resume mode
  configurationId?: string;
  // optional snapshot fallback
  snapshot?: ConfigurationSnapshot;
}

// ──────────────────────────────
// Requests (Widget → API-Service)
// ──────────────────────────────
export interface CreateConfigurationRequest {
  productId: string;
  kbId?: string;
  locale?: string;
}

export interface UpdateCharacteristicRequest {
  configurationId: string;
  characteristicId: string;
  // value = SAP internal key (valueLow), null = deselect
  value: string | null;
}

// ──────────────────────────────
// Normalized Response (API-Service → Widget)
// ──────────────────────────────
export interface ConfigurationResponse {
  configurationId: string;     // mapped from SAP "id"
  productId: string;           // mapped from SAP "kbKey.name"
  kbId?: string;               // mapped from SAP "kbId" (number → string)
  complete: boolean;
  consistent: boolean;
  rootItem: ConfigurationItem;
  groups: CharacteristicGroup[]; // from SAP characteristicGroups[]
  messages: ConfigurationMessage[];
}

export interface CharacteristicGroup {
  id: string;
  name: string;
  complete: boolean;
  consistent: boolean;
  visible: boolean;
}

export interface ConfigurationItem {
  id: string;
  key: string;
  complete: boolean;
  consistent: boolean;
  characteristics: Characteristic[];
  subItems?: ConfigurationItem[];
}

export interface Characteristic {
  id: string;
  name: string;
  description?: string;
  valueType: 'SINGLE' | 'MULTI' | 'FREE_TEXT' | 'NUMERIC';
  required: boolean;
  visible: boolean;
  readOnly: boolean;
  complete: boolean;
  consistent: boolean;
  length?: number;
  numberDecimals?: number;
  entryFieldMask?: string;
  values: CharacteristicValue[];
  possibleValues: CharacteristicValue[];
}

export interface CharacteristicValue {
  id: string;
  name: string;
  description?: string;
  author?: 'Default' | 'System' | 'User';
}

export interface ConfigurationMessage {
  severity: 'INFO' | 'WARNING' | 'ERROR';
  text: string;
  characteristicId?: string;
}

// ──────────────────────────────
// Snapshot (save/restore)
// ──────────────────────────────
export interface ConfigurationSnapshot {
  configurationId?: string;
  productId: string;
  kbId?: string;
  savedAt: string;
  complete: boolean;
  consistent: boolean;
  rootItem: ConfigurationItem;
  groups?: CharacteristicGroup[];
  messages?: ConfigurationMessage[];
  sourceContext?: string;
}