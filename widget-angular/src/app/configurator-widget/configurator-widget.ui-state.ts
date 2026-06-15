// src/app/configurator-widget/configurator-widget.ui-state.ts
import { Signal, computed } from '@angular/core';
import { ConfigurationResponse, WidgetState } from '../models/configuration.models';

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
}

export function createConfiguratorWidgetUiState(
  configuration: Signal<ConfigurationResponse | null>,
  status: Signal<WidgetState>,
  errorMessage: Signal<string | null>
): ConfiguratorWidgetUiState {
  const isReadOnly = computed(() => configuration()?.restoreInfo?.readOnly === true);

  const hasErrors = computed(() => {
    const current = configuration();
    if (!current) return false;

    return (
      !current.consistent ||
      (current.messages ?? []).some(message => message.severity === 'ERROR')
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
    if (hasErrors()) return 'conflict';
    if (!current.complete) return 'incomplete';

    return 'ready';
  });

  const uiStateText = computed(() => {
    switch (uiState()) {
      case 'idle':
        return 'Configuration has not been started yet.';
      case 'loading':
        return 'Configuration is being processed.';
      case 'incomplete':
        return 'Configuration is incomplete. Fill all required characteristics.';
      case 'conflict':
        return 'Configuration contains conflicts or errors. Review the highlighted fields and messages.';
      case 'ready':
        return 'Configuration is complete and consistent. You can finish it now.';
      case 'completed':
        return 'Configuration was completed successfully. Add to cart is available.';
      case 'readonly':
        return 'Snapshot fallback is shown in read-only mode. Changes and completion are not available.';
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
    uiStateText
  };
}