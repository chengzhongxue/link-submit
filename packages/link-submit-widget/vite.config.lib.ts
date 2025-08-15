import { fileURLToPath } from 'url';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';
import { viteStaticCopy as StaticCopy } from 'vite-plugin-static-copy';
import { sharedPluginsConfig } from './src/vite/shared-plugin-config';

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'LinkSubmitWidget',
      fileName: 'link-submit-widget',
      formats: ['iife', 'es'],
    },
    emptyOutDir: true,
    rollupOptions: {
      output: {
        extend: true,
      },
    },
  },
  plugins: [
    ...sharedPluginsConfig,
    dts(),
    StaticCopy({
      targets: [
        {
          src: ['./dist/link-submit-widget.iife.js', './var.css'],
          dest: fileURLToPath(new URL('../../src/main/resources/static', import.meta.url)),
        },
      ],
    }),
  ],
});
