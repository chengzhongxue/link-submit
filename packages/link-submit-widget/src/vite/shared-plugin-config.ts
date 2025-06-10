import presetIcons from '@unocss/preset-icons';
import { presetUno } from 'unocss';
import UnoCSS from 'unocss/vite';

export const sharedPluginsConfig = [
  UnoCSS({
    mode: 'shadow-dom',
    presets: [presetUno(), presetIcons()],
    theme: {
      colors: {
        base: 'var(--link-submit-widget-base-bg-color)',
        modal: {
          layer: 'var(--link-submit-widget-modal-layer-color)',
        },
        form: {
          bg: 'var(--link-submit-widget-form-bg-color)',
          border: 'var(--link-submit-widget-form-border-color)',
          text: 'var(--link-submit-widget-form-text-color)',
          label: 'var(--link-submit-widget-form-label-color)',
          placeholder: 'var(--link-submit-widget-form-placeholder-color)',
          button: {
            bg: 'var(--link-submit-widget-form-button-bg-color)',
            text: 'var(--link-submit-widget-form-button-text-color)',
            hover: {
              bg: 'var(--link-submit-widget-form-button-hover-bg-color)',
            },
          },
        },
      },
      borderRadius: {
        base: 'var(--link-submit-widget-base-rounded, 0.4em)',
      },
    },
    shortcuts: {
      'bg-base': 'bg-[var(--link-submit-widget-base-bg-color)]',
      'bg-modal': 'bg-[var(--link-submit-widget-modal-layer-color)]',
      'rounded-base': 'rounded-[var(--link-submit-widget-base-rounded)]',
      'form-input': 'rounded-base border border-form-border px-4 py-3 text-base focus:(outline-none border-blue-500) bg-form-bg text-form-text placeholder-form-placeholder',
      'form-label': 'text-base font-medium text-form-label mb-1',
      'form-button': 'rounded-base bg-form-button-bg text-form-button-text px-8 py-3 text-base font-medium hover:bg-form-button-hover-bg transition disabled:opacity-50 disabled:cursor-not-allowed',
    },
  }),
];
