import { defineConfig } from 'astro/config';
import fs from 'node:fs';
import path from 'node:path';

import cloudflare from "@astrojs/cloudflare";

// Update `site` to your real domain when you have one — powers sitemap + canonical URLs.
// APP build → 'file' (/tools/x.html): Capacitor's WebView asset server resolves these
// directly. (Directory/index.html paths fall back to the root index, so every tool
// opened the home.) WEB build keeps 'directory' for pretty SEO URLs.
const isApp = process.env.PUBLIC_APP_BUILD === '1';

// SECURITY/PRIVACY: strip pages that reach external servers from the APP bundle so they can't
// be hit by direct URL inside the offline app. video-to-mp3 fetches a 32MB ffmpeg wasm + jszip
// from jsDelivr/unpkg; /admin (Decap CMS) loads unpkg; cloud-import.js talks to Google/Dropbox.
// All three are web-only — never linked from the app UI — so removing them from dist is safe.
const stripFromApp = {
  name: 'snapjar-strip-app-web-only',
  hooks: {
    'astro:build:done': ({ dir }) => {
      if (!isApp) return;
      const root = dir && dir.pathname ? dir.pathname : path.resolve('dist');
      const base = process.platform === 'win32' && root.startsWith('/') ? root.slice(1) : root;
      const targets = ['tools/video-to-mp3.html', 'admin', 'assets/cloud-import.js'];
      for (const t of targets) {
        const p = path.join(base, t);
        try { fs.rmSync(p, { recursive: true, force: true }); console.log('[strip-app] removed ' + t); } catch (e) {}
      }
    },
  },
};

export default defineConfig({
  site: 'https://snapjar.sankettoraskar-business.workers.dev',
  build: { format: isApp ? 'file' : 'directory' },
  integrations: isApp ? [stripFromApp] : [],
  output: "hybrid",
  adapter: cloudflare()
});