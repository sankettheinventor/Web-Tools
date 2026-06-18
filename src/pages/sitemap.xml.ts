// Self-generated sitemap for all current pages.
export async function GET(context: { site?: URL }) {
  const site = context.site?.href ?? 'https://thor-tools.netlify.app/';
  const paths = [
    '',
    'calculators/emi', 'calculators/mortgage', 'calculators/sip',
    'calculators/compound-interest', 'calculators/fd', 'calculators/loan-payoff',
    'calculators/income-tax', 'calculators/retirement', 'calculators/salary',
    'calculators/percentage', 'calculators/bmi', 'calculators/age',
    'calculators/tip', 'calculators/discount', 'calculators/unit-converter',
    'tools/watermark', 'tools/compress', 'tools/convert', 'tools/favicon', 'tools/qr-code',
    'tools/heic-to-jpg', 'tools/pdf-tools', 'tools/svg-optimizer',
    'about', 'privacy-policy', 'terms',
  ];
  const urls = paths.map((p) => new URL(p, site).href);
  const body =
    `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n` +
    urls.map((u) => `  <url><loc>${u}</loc></url>`).join('\n') +
    `\n</urlset>\n`;
  return new Response(body, { headers: { 'Content-Type': 'application/xml' } });
}
