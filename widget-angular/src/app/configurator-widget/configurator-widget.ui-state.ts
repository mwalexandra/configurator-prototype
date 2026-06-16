// src/app/configurator-widget/configurator-widget.ui-state.ts
import { Signal, computed } from '@angular/core';
import {
  Characteristic,
  ConfigurationMessage,
  ConfigurationResponse,
  WidgetState
} from '../models/configuration.models';

export type WidgetUiState =
  | 'idle'
  | 'loading'
  | 'incomplete'
  | 'conflict'
  | 'ready'
  | 'completed'
  | 'readonly'
  | 'error';

export interface ConfiguratorWidgetUiState {
  isReadOnly: Signal<boolean>;
  hasErrors: Signal<boolean>;
  isReadyForCompletion: Signal<boolean>;
  isReadyForAddToCart: Signal<boolean>;
  uiState: Signal<WidgetUiState>;
  uiStateText: Signal<string>;

  incompleteCharacteristics: Signal<Characteristic[]>;
  hiddenProblemCharacteristics: Signal<Characteristic[]>;
  problemCharacteristicIds: Signal<Set<string>>;
}

export function createConfiguratorWidgetUiState(
  configuration: Signal<ConfigurationResponse | null>,
  status: Signal<WidgetState>,
  errorMessage: Signal<string | null>
): ConfiguratorWidgetUiState {
  const isReadOnly = computed(() =>
    configuration()?.restoreInfo?.readOnly === true
  );

  const allCharacteristics = computed(() =>
    configuration()?.rootItem?.characteristics ?? []
  );

  const incompleteCharacteristics = computed(() =>
    allCharacteristics().filter(c => c.required && !c.complete)
  );

  const errorCharacteristicIds = computed(() => {
    const ids = new Set<string>();
    const messages: ConfigurationMessage[] = configuration()?.messages ?? [];

    for (const msg of messages) {
      if (msg.severity === 'ERROR' && msg.characteristicId) {
        ids.add(msg.characteristicId);
      }
    }

    return ids;
  });

  const problemCharacteristicIds = computed(() => {
    const ids = new Set<string>();

    for (const c of incompleteCharacteristics()) {
      ids.add(c.id);
    }

    for (const id of errorCharacteristicIds()) {
      ids.add(id);
    }

    return ids;
  });

  const hiddenProblemCharacteristics = computed(() =>
    allCharacteristics().filter(
      c => !c.visible && problemCharacteristicIds().has(c.id)
    )
  );

  const hasErrors = computed(() => {
    const current = configuration();
    if (!current) return false;

    return (current.messages ?? []).some(m => m.severity === 'ERROR');
  });

  const hasConflicts = computed(() => {
    const current = configuration();
    if (!current) return false;

    return !current.consistent || hasErrors();
  });

  const isReadyForCompletion = computed(() => {
    const current = configuration();
    if (!current) return false;

    return (
      !isReadOnly() &&
      current.complete &&
      current.consistent &&
      status() !== 'loading' &&
      status() !== 'updating' &&
      status() !== 'completing' &&
      status() !== 'completed'
    );
  });

  const isReadyForAddToCart = computed(() => {
    const current = configuration();
    if (!current) return false;

    return status() === 'completed' && current.complete && current.consistent;
  });

  const uiState = computed<WidgetUiState>(() => {
    const current = configuration();

    if (status() === 'error') return 'error';

    if (
      status() === 'loading' ||
      status() === 'updating' ||
      status() === 'completing'
    ) {
      return 'loading';
    }

    if (!current) return 'idle';
    if (isReadOnly()) return 'readonly';
    if (status() === 'completed') return 'completed';

    if (incompleteCharacteristics().length > 0) {
      return 'incomplete';
    }

    if (hasConflicts()) {
      return 'conflict';
    }

    if (current.complete && current.consistent) {
      return 'ready';
    }

    return 'incomplete';
  });

  const uiStateText = computed(() => {
    const hidden = hiddenProblemCharacteristics();
    const error = errorMessage();

    switch (uiState()) {
      case 'idle':
        return 'Start a new configuration session.';
      case 'loading':
        return 'Loading configuration session...';
      case 'incomplete':
        return hidden.length
          ? `Some required characteristics are still incomplete, including ${hidden.length} hidden field(s).`
          : 'Some required characteristics are still incomplete.';
      case 'conflict':
        return 'The configuration contains conflicts that must be resolved before confirmation.';
      case 'ready':
        return 'The configuration is complete and consistent. You can confirm it now.';
      case 'completed':
        return 'The configuration has been confirmed and is ready for downstream handoff.';
      case 'readonly':
        return 'This configuration was restored from snapshot fallback and is available in read-only mode.';
      case 'error':
        return error ?? 'The configuration could not be processed.';
      default:
        return 'Configuration state unavailable.';
    }
  });

  return {
    isReadOnly,
    hasErrors,
    isReadyForCompletion,
    isReadyForAddToCart,
    uiState,
    uiStateText,
    incompleteCharacteristics,
    hiddenProblemCharacteristics,
    problemCharacteristicIds
  };
}
