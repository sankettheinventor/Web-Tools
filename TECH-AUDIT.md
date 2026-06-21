# SnapJar — Brutal Technical Audit (deep)

_2026-06-22. Mechanical probes across all 45 pages + 3 parallel deep-read auditors (PDF tools / image tools / calculators). Severity: 🔴 crash·hang·wrong-output · 🟡 should-fix · 🟢 minor. ✅ = fixed this pass; ⏳ = pending (often needs the device to measure/see)._

## CLEAN BILLS (verified, not a problem)
- ✅ **XSS: none.** Every `innerHTML` uses a static template or numeric computed value; all user text goes through `textContent`. qr-code/svg-optimizer output to canvas/`textarea.value`, never `innerHTML`. (Important: the WebView has Capacitor native-bridge access, so this mattered.)
- ✅ **Network/connection/timeout-from-network: N/A in the app.** Only `cloud-import.js` has `fetch()`, and it's stripped from the app build. The app makes **zero network calls** → no connection/offline-timeout failure modes.
- ✅ **localStorage** fully try/guarded (Base.astro only). No `JSON.parse`, no `eval`/`Function`.
- ✅ **Finance formulas correct:** EMI, mortgage, SIP FV, compound interest verified right; `num()` coercion + `i>0` guards mean **no NaN/Infinity/₹Infinity leaks** anywhere in the calculators.
- ✅ **Android config sane:** minSdk 24 (broad budget reach), targetSdk 36 (exceeds Play-2026 API-35), versionCode 1.

## 🔴 FIXED THIS PASS (deterministic, verified by reasoning)
- ✅ **income-tax: 87A rebate cliff → marginal relief.** New regime jumped tax from ₹0 at ₹12,00,000 taxable to ~₹60k+cess at ₹12,00,001 — **legally wrong**. Added marginal relief (`tax=min(tax, taxable−1200000)`). Old-regime ₹5L cliff left as-is (that one IS the law — no marginal relief in old regime).
- ✅ **age calculator: UTC off-by-one.** `toISOString()` + `new Date(string)` shifted the date a day for non-UTC zones (IST audience). Now formats/parses yyyy-mm-dd in **local** time.
- ✅ **extract-pages / remove-pages: silent range bug.** `5-3` (reversed) and `1-`/`-5` (NaN) silently produced nothing. Now swaps `a>b` and guards NaN.
- ✅ **qr-code: WiFi/vCard not escaped.** SSID/password/name with `; , : " \` made a **malformed, unscannable** QR. Added spec-correct escaping.

## 🔴 PENDING — crash / hang vectors on budget phones (the real one-star risks)
- ⏳ **Canvas OOM — no max-dimension clamp.** `compress-pdf`, `pdf-to-jpg`, `organize-pdf` (user scale ×3 × big page) and `compress`/`convert`/`watermark`/`color-picker`/`favicon` (50MP phone photo) allocate tens of millions of px → blank/null canvas or WebView kill on 2–4GB RAM. **Fix:** clamp longest side to ~4096px / ~16MP before drawing. *(Needs device to confirm the safe ceiling.)*
- ⏳ **jpg-to-pdf: infinite hang on a bad image.** `toJpeg` has no `img.onerror`, so a corrupt/HEIC/oversized image never resolves the `await` → "Building PDF…" frozen forever. **Fix:** `im.onerror→resolve(null)`, skip nulls. *(Same missing-onerror pattern in 6 image tools → frozen drop zone.)*
- ⏳ **pdf.js tools: no try/catch on password-protected PDFs.** `pdf-to-jpg`, `compress-pdf`, `organize-pdf` — pdf.js throws `PasswordException` (it ignores pdf-lib's `ignoreEncryption`), unhandled → "Rendering…" frozen, no error. **Fix:** wrap render path, show "This PDF is password-protected."
- ⏳ **No double-submit guard anywhere** (only 1 of 25 tools). Tap "Merge/Compress/Convert" twice on a slow phone → two concurrent loops mutate the same buffer → corrupted output + double download. **Fix:** `btn.disabled=true` in a `try/finally`.

## 🟡 PENDING — should-fix
- ⏳ **Blob-URL leaks: 33 created, 21 revoked.** Batch tools (`jpg-to-pdf`, `heic-to-jpg`, `watermark`, `convert`, `pdf-to-jpg`, `compress`, `favicon`, `color-picker`, `palette-extractor`) create thumbnail object URLs and never revoke → accumulating memory over a session. **Fix:** revoke on `img.onload` / on reset. *(Care: don't revoke before the image paints.)*
- ⏳ **watermark.astro "Download all": N concurrent full-res canvas encodes** (`setTimeout(i*400)`) → 30 large photos = 30 overlapping 100MB canvases = OOM. **Fix:** serialize (await each `toBlob`) + clamp dims.
- ⏳ **No cancel/abort/timeout on long loops** (split-pdf 500 pages, organize-pdf 200-page thumbnails, big OCR later). UI freezes minutes then maybe OOM, no escape. **Fix:** yield every N (`await setTimeout`), show progress, cap+warn on huge counts, add Cancel.
- ⏳ **`toBlob` null not handled** (`pdf-to-jpg`, `convert`) → `createObjectURL(null)` / `null.size` throws. **Fix:** `if(!blob) skip/error`.
- ⏳ **WebP encode not feature-detected** (`convert`/`[convert]`/`favicon`) — old Android WebView may not encode `image/webp`; `toBlob` returns null/PNG → a "→WebP" page outputs PNG bytes named `.webp` = corrupt file. **Fix:** feature-detect once, warn/fallback honestly.
- ⏳ **watermark-pdf: long text runs off the page** — font size not shrunk to fit page width, offset math pushes start point off-edge → watermark invisible. **Fix:** shrink size until text width ≤ 0.9·page width.
- ⏳ **income-tax slab breakdown table** (line ~232) uses bands/labels that don't match the new-regime slab edges (4L/8L/12L) → the per-row breakdown is mislabeled (headline number is now correct after the fix above).
- ⏳ **FileReader: 0 onerror handlers** (4 uses); **pure pdf-lib tools** (rotate/watermark/add-page-numbers/split) have **no try/catch on load** → corrupt PDF = silent dead-end.
- ⏳ **loan-payoff:** headline shows "Never" while the schedule below shows a real payoff when only an annual lump is set — contradictory.
- ⏳ **tip:** round-up makes per-person × split exceed the rounded total (reconciliation off by a rupee or two).

## 🟢 MINOR
- ⏳ Fixed download filenames collide across runs (`merged.pdf`, `page-1.jpg`) — append source/timestamp.
- ⏳ `revokeObjectURL` on a fixed 1500ms `setTimeout` after download → on a slow phone the blob may be revoked before the download consumes it (empty file). Gate on the download instead.
- ⏳ svg-optimizer whitespace-collapse can alter text spacing inside `<text>`/`xml:space="preserve"`.
- ⏳ fd post-tax yield model deducts tax from a balance that compounded gross — internally inconsistent (not user-facing-broken).

## NATIVE / CAPACITOR GAPS (need the device)
- ⏳ **Share-target handler NOT wired.** Manifest advertises SnapJar as a share destination, but there's no `appUrlOpen`/SendIntent handler → sharing a file to SnapJar opens the app and does nothing. **Dead feature if shipped.**
- ⏳ **No hardware back-button handling** — confirm Android back doesn't just exit the app from a tool screen.
- ⏳ **pdfjs worker offline:** confirm the bundled `pdf.worker` loads under the `https://localhost` Capacitor scheme on-device.

## SUGGESTED FIX ORDER (next pass)
1. Canvas dimension clamp (shared helper) — kills the top OOM vector across 8 tools.
2. `img.onerror` + pdf.js try/catch — kills the frozen-UI hangs.
3. Double-submit guard (shared pattern) across all action buttons.
4. Blob-URL revoke sweep + watermark serial download-all.
5. Long-op yield/cancel/progress + page-count caps.
6. Share-target runtime handler + back-button (device).
7. The 🟡 logic/display fixes (slab table, watermark-pdf positioning, WebP detect, loan-payoff, tip).

_Most 🔴/🟡 robustness items share ~5 root-cause patterns — fixable as shared helpers, not 45 one-offs. Thresholds (clamp size, page caps) want one device measurement first._
