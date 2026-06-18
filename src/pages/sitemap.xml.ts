// Self-generated sitemap for all current pages.
export async function GET(context: { site?: URL }) {
  const site = context.site?.href ?? 'https://thor-tools.netlify.app/';
  const paths = [
    '',
    'calculators/emi', 'calculators/mortgage', 'calculators/sip',
    'calculators/compound-interest', 'calculators/loan-payoff',
    'calculators/percentage', 'calculators/bmi',
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
