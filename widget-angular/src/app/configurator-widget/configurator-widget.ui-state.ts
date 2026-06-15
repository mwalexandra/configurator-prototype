// src/app/configurator-widget/configurator-widget.ui-state.ts
import { Signal, computed } from '@angular/core';
import { Characteristic, ConfigurationMessage, ConfigurationResponse, WidgetState } from '../models/configuration.models';

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
  // Общее состояние
  isReadOnly: Signal<boolean>;
  hasErrors: Signal<boolean>;
  isReadyForCompletion: Signal<boolean>;
  isReadyForAddToCart: Signal<boolean>;
  uiState: Signal<WidgetUiState>;
  uiStateText: Signal<string>;

  // Диагностика характеристик
  incompleteCharacteristics: Signal<Characteristic[]>;
  hiddenProblemCharacteristics: Signal<Characteristic[]>;
  problemCharacteristicIds: Signal<Set<string>>;
}

export function createConfiguratorWidgetUiState(
  configuration: Signal<ConfigurationResponse | null>,
  status: Signal<WidgetState>,
  errorMessage: Signal<string | null>
): ConfiguratorWidgetUiState {

  // ── базовые ─────────────────────────────────────────────────────────────────

  const isReadOnly = computed(() =>
    configuration()?.restoreInfo?.readOnly === true
  );

  const hasErrors = computed(() => {
    const current = configuration();
    if (!current) return false;

    return (
      !current.consistent ||
      (current.messages ?? []).some(m => m.severity === 'ERROR')
    );
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

  // ── диагностика характеристик ────────────────────────────────────────────────

  const allCharacteristics = computed(() =>
    configuration()?.rootItem?.characteristics ?? []
  );

  // Характеристика неполная, если required=true и complete=false
  const incompleteCharacteristics = computed(() =>
    allCharacteristics().filter(c => c.required && !c.complete)
  );

  // Характеристики с ERROR-сообщением
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

  // Объединённый набор ID для подсветки в шаблоне
  const problemCharacteristicIds = computed(() => {
    const ids = new Set<string>();
    for (const c of incompleteCharacteristics()) ids.add(c.id);
    for (const id of errorCharacteristicIds()) ids.add(id);
    return ids;
  });

  // Скрытые проблемные поля — именно они чаще всего блокируют Complete
  const hiddenProblemCharacteristics = computed(() =>
    allCharacteristics().filter(
      c => !c.visible && (problemCharacteristicIds().has(c.id))
    )
  );

  // ── UI state ─────────────────────────────────────────────────────────────────

  const uiState = computed<WidgetUiState>(() => {
    const current = configuration();

    if (status() === 'error') return 'error';
    if (
      status() === 'loading' ||
      status() === 'updating' ||
      status() === 'completing'
    ) return 'loading';

    if (!current) return 'idle';
    if (isReadOnly()) return 'readonly';
    if (status() === 'completed') return 'completed';
    if (hasErrors()) return 'conflict';
    if (!current.complete) return 'incomplete';

    return 'ready';
  });

  const uiStateText = computed(() => {
    const hidden = hiddenProblemCharacteristics();

    switch (uiState()) {
      case 'idle':
        return 'Configuration has not been started yet.';
      case 'loading':
        return 'Configuration is being processed.';
      case 'incomplete':
        return hidden.length
          ? `Configuration is incomplete. ${hidden.length} required field(s) are not visible: ${hidden.map(c => c.name || c.id).join(', ')}.`
          : 'Configuration is incomplete. Fill all required characteristics.';
      case 'conflict':
        return 'Configuration contains conflicts or errors. Review the highlighted fields.';
      case 'ready':
        return 'Configuration is complete and consistent. You can finish it now.';
      case 'completed':
        return 'Configuration was completed successfully. Add to cart is available.';
      case 'readonly':
        return 'Snapshot fallback is shown in read-only mode. Changes are not available.';
      case 'error':
        return errorMessage() ?? 'An error occurred.';
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