// ──────────────────────────────
// Modes & widget state
// ──────────────────────────────

export type ConfiguratorMode = 'create' | 'resume';

export type WidgetState =
  | 'idle'
  | 'loading'
  | 'loaded'
  | 'updating'
  | 'error'
  | 'completing'
  | 'completed';

export type CharacteristicValueType = 'SINGLE' | 'MULTI' | 'FREETEXT' | 'NUMERIC';
export type ResumeStrategy = 'LIVECONFIGURATION' | 'SNAPSHOTFALLBACK' | 'READONLYSNAPSHOT';
export type ResumeStatus = 'RESUMED' | 'FALLBACKAPPLIED' | 'FAILED';

export type ResumeSourceContext =
  | 'commerce'
  | 'erp'
  | 'salesforce'
  | 'generic';

// ──────────────────────────────
// Host → Widget
// ──────────────────────────────

export interface WidgetInputConfig {
  apiBaseUrl: string;
  mode: ConfiguratorMode;
  // create mode
  productId?: string;
  kbId?: string;
  // resume mode
  resume?: ResumeInput;
}

export interface ResumeInput {
  configurationId?: string;
  snapshot?: ConfigurationSnapshot;
  sourceContext?: ResumeSourceContext;
}

// ──────────────────────────────
// Widget → API-Service
// ──────────────────────────────

export interface CreateConfigurationRequest {
  productId: string;
  kbId?: string;
}

export interface UpdateCharacteristicRequest {
  configurationId: string;
  itemId?: string;
  characteristicId: string;
  value: string | null;
}

export interface ResumeConfigurationRequest {
  configurationId?: string;
  snapshot?: ConfigurationSnapshot;
  sourceContext?: ResumeSourceContext;
}

// ──────────────────────────────
// Shared configuration domain
// ──────────────────────────────

export interface ConfigurationItem {
  id: string;
  key: string;
  complete: boolean;
  consistent: boolean;
  characteristics: Characteristic[];
  subItems?: ConfigurationItem[];
}

export interface CharacteristicGroup {
  id: string;
  name?: string;
  complete: boolean;
  consistent: boolean;
  visible: boolean;
}

export interface Characteristic {
  id: string;
  name: string;
  description?: string;
  valueType: CharacteristicValueType;
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

export type CharacteristicValueAuthor =
  | 'Default'
  | 'System'
  | 'User'
  | string;

export interface CharacteristicValue {
  id: string;
  name: string;
  description?: string;
  author?: CharacteristicValueAuthor;
}

export type MessageSeverity =
  | 'INFO'
  | 'WARNING'
  | 'ERROR';

export interface ConfigurationMessage {
  severity: MessageSeverity;
  text: string;
  characteristicId?: string;
}

// ──────────────────────────────
// Snapshot / save-restore
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

  metadata?: SnapshotMetadata;
}

export interface SnapshotMetadata {
  sourceContext?: ResumeSourceContext;
  hostEntityType?: string;
  hostEntityId?: string;
  version?: string;
}

// ──────────────────────────────
// Restore metadata
// ──────────────────────────────

export interface RestoreInfo {
  mode: ConfiguratorMode;
  status: ResumeStatus;
  strategy?: ResumeStrategy;
  liveSessionAvailable: boolean;
  snapshotUsed: boolean;
  readOnly: boolean;
  message?: string;
}

// ──────────────────────────────
// API-Service → Widget runtime response
// ──────────────────────────────

export interface ConfigurationResponse {
  configurationId: string;
  productId: string;
  kbId?: string;

  complete: boolean;
  consistent: boolean;

  rootItem: ConfigurationItem;
  groups: CharacteristicGroup[];
  messages: ConfigurationMessage[];

  backendProcessingTimeMs?: number;
  restoreInfo?: RestoreInfo;
}

// ──────────────────────────────
//  Widget → Host outputs
// ──────────────────────────────

export interface CompletedConfigurationResult {
  configurationId: string;
  productId: string;
  kbId?: string;
  addedToCart: boolean;
  receivedAt: string;
  snapshot: ConfigurationSnapshot;
  fullConfiguration: ConfigurationResponse;
}