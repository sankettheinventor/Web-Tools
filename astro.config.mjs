import { defineConfig } from 'astro/config';

// Update `site` to your real domain when you have one — powers sitemap + canonical URLs.
// APP build → 'file' (/tools/x.html): Capacitor's WebView asset server resolves these
// directly. (Directory/index.html paths fall back to the root index, so every tool
// opened the home.) WEB build keeps 'directory' for pretty SEO URLs.
const isApp = process.env.PUBLIC_APP_BUILD === '1';
export default defineConfig({
  site: 'https://snapjar.sankettoraskar-business.workers.dev',
  build: { format: isApp ? 'file' : 'directory' },
});
