import { getCollection } from 'astro:content';
import { catSlug } from '../lib/categories';

// Self-generated sitemap for all current pages (incl. blog posts + categories).
export async function GET(context: { site?: URL }) {
  const site = context.site?.href ?? 'https://snapjar.sankettoraskar-business.workers.dev/';
  const posts = await getCollection('blog', ({ data }) => !data.draft);
  const cats = Array.from(new Set(posts.map((p) => p.data.category)));
  const paths = [
    '', 'blog',
    'calculators/emi', 'calculators/mortgage', 'calculators/sip',
    'calculators/compound-interest', 'calculators/fd', 'calculators/loan-payoff',
    'calculators/income-tax', 'calculators/retirement', 'calculators/salary',
    'calculators/percentage', 'calculators/bmi', 'calculators/age',
    'calculators/tip', 'calculators/discount', 'calculators/unit-converter',
    'tools/watermark', 'tools/compress', 'tools/convert', 'tools/favicon', 'tools/qr-code',
    'tools/heic-to-jpg', 'tools/pdf-tools', 'tools/svg-optimizer',
    'tools/jpg-to-png', 'tools/png-to-jpg', 'tools/webp-to-png',
    'tools/png-to-webp', 'tools/jpg-to-webp', 'tools/webp-to-jpg',
    'tools/color-picker', 'tools/palette-extractor', 'tools/color-converter', 'tools/gradient-generator',
    'tools/word-counter', 'tools/character-counter', 'tools/case-converter', 'tools/text-to-pdf',
    'tools/jpg-to-pdf', 'tools/pdf-to-jpg',
    'tools/split-pdf', 'tools/remove-pages', 'tools/extract-pages',
    'tools/rotate-pdf', 'tools/add-page-numbers', 'tools/watermark-pdf',
    'tools/organize-pdf', 'tools/compress-pdf',
    'tools/video-to-mp3',
    'about', 'privacy-policy', 'terms',
  ];
  const urls = [
    ...paths.map((p) => new URL(p, site).href),
    ...posts.map((p) => new URL(`blog/${p.slug}`, site).href),
    ...cats.map((c) => new URL(`blog/category/${catSlug(c)}`, site).href),
  ];
  const body =
    `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n` +
    urls.map((u) => `  <url><loc>${u}</loc></url>`).join('\n') +
    `\n</urlset>\n`;
  return new Response(body, { headers: { 'Content-Type': 'application/xml' } });
}
