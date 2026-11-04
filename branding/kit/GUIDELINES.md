# TubeGuard logo: usage guide

## The idea

A **seal** with **play** cut out of its centre. The scalloped outline is the same Material 3 Expressive "cookie"
shape the app uses on its screens, so the icon and the app read as one product. A seal stands for "checked and
protected" without the usual shield or padlock, and the play is a plain play symbol, not YouTube's red mark.

## Files

| Use | File |
|---|---|
| Symbol, master (black, 256 × 256) | `../masters/tubeguard-symbol.svg` |
| Symbol, small-size cut (32 px and below; favicon, launcher, notifications) | `../masters/tubeguard-symbol-small.svg` |
| Horizontal lockup | `../masters/tubeguard-horizontal.svg` |
| Stacked lockup | `../masters/tubeguard-stacked.svg` |
| Wordmark only | `../masters/tubeguard-wordmark.svg` |
| One-colour, brand-colour, square, app-icon and favicon variants (SVG + PNG) | `symbol/` |
| Lockups in black, white, salmon and ember (SVG + PNG) | `lockups/` |
| Web icons: `favicon.ico`, PNG set, `site.webmanifest`, `head-snippet.html` | `web/` |
| Android TV banner preview | `android/banner-preview.png` |
| Presentation with mockups | `presentation.html`, `slides/` |

The app's launcher icon, themed (monochrome) icon, banner, notification icon and top-bar logo are generated from
the masters by `../tools/android_assets.py`. Rerun it after any change to the masters, so the app never drifts
from them. The masters themselves come from `../tools/build_logo.py`.

## Colour

| Name | HEX | RGB | CMYK (approx.) | Use |
|---|---|---|---|---|
| **Salmon** | `#FFB4A9` | 255 180 169 | 0 29 34 0 | The logo on dark backgrounds; the app's accent |
| **Ember** | `#8F2A21` | 143 42 33 | 0 71 77 44 | The logo on light backgrounds |
| **Charcoal** | `#131314` | 19 19 20 | 5 5 0 92 | Backgrounds, the app icon tile |
| **Mist** | `#E6E1E3` | 230 225 227 | 0 2 1 10 | Text on charcoal |

Contrast: Salmon on Charcoal **10.9 : 1**, Ember on white **8.3 : 1**, Mist on Charcoal **14.4 : 1**.
**Never put Salmon on white (1.7 : 1)**: use Ember there. For print, match the CMYK values against a swatch
book; they are conversions, not measured Pantone matches.

## Type

The wordmark is set in **Plus Jakarta Sans** at weight 750, tracked −1.2 %, and converted to outlines, so no font
is needed to use it. The font is under the SIL Open Font License, which allows use in logos
(`PlusJakartaSans-OFL.txt`). For text next to the logo, use the app's own type (Roboto on Android TV).

## Clear space

Keep empty space around the logo equal to **one third of the symbol's height** on every side. It's the same gap that
separates the symbol from the word in the horizontal lockup.

## Minimum sizes

| Version | Smallest |
|---|---|
| Symbol, master | 40 px / 10 mm |
| Symbol, small-size cut | 16 px / 5 mm |
| Horizontal lockup | 120 px wide / 30 mm |
| Stacked lockup | 64 px wide / 18 mm |

Below 40 px, always use the small-size cut. It has eight deeper lobes and a larger play, so both survive.

## Backgrounds

- **Charcoal or any dark colour:** Salmon or white.
- **White or any light colour:** Ember or black.
- **Photos:** only where the area behind the logo is plainly dark or plainly light. Otherwise put it on a charcoal
  tile.

## Don't

- Don't recolour the play cut-out or fill it in: it's a hole, and the background shows through.
- Don't use YouTube's red, or place the play in a red rounded rectangle.
- Don't add outlines, shadows, gradients or glows.
- Don't rotate the seal, stretch it, or change the number of lobes.
- Don't retype the name in another font, or change the space between symbol and name.
- Don't put Salmon on white.

## Still to do

- **Trademark search:** run a professional search (trademark databases and a reverse image search) before
  distributing widely. I can't guarantee clearance.
- **Placeholder in the mockups:** the website and browser-tab mockups use `tubeguard.com` as a placeholder. It isn't
  your domain.
